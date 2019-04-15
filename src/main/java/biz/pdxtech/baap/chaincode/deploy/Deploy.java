/*************************************************************************
 * Copyright (C) 2016-2019 The PDX Blockchain Hypercloud Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *************************************************************************/

package biz.pdxtech.baap.chaincode.deploy;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.chaincode.chainIaas.Chainiaas;
import biz.pdxtech.baap.chaincode.chainIaas.ChainiaasDTO;
import biz.pdxtech.baap.command.IaasRemote;
import biz.pdxtech.baap.command.JnaConstants;
import biz.pdxtech.baap.command.RuntiomeUtil;
import biz.pdxtech.baap.command.TimingJob;
import biz.pdxtech.baap.conf.BaapProperties;
import biz.pdxtech.baap.util.StreamDownloadUtil;
import biz.pdxtech.baap.util.grpc.StreamClientTLS;
import biz.pdxtech.baap.util.json.BaapJSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.ssl.SslContext;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


public class Deploy extends ChaincodeBase {

    private static Logger logger = LoggerFactory.getLogger(Deploy.class);

    private static ObjectMapper mapper = new ObjectMapper();
    public static Map<String, DeployInfo> ccReconnect = new HashMap<>();
    public static Map<String, Integer> ccReconnectTimes = new HashMap<>();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Runnable runnable = () -> {
            Iterator<String> itKey = ccReconnect.keySet().iterator();
            while (itKey.hasNext()) {
                String key = itKey.next();
                DeployInfo deployInfo = ccReconnect.get(key);
                try {
                    Integer repeatTime = ccReconnectTimes.get(key);
                    logger.info("current DeployInfo is {} ==== retry count{}",getDeployInfoKey(deployInfo),repeatTime);
                    if (repeatTime == null) {
                        repeatTime = 0;
                    }
                    repeatTime++;
                    if (repeatTime > JnaConstants.repeatTimes) {
                        itKey.remove();
                        ccReconnectTimes.remove(key);
                    } else {
                        ccReconnectTimes.put(key, repeatTime);
                    }

                    if (deployInfo.getFileId() == null) {
                        deployInfo = getDeployInfoByCID(deployInfo.getChaincodeId(), deployInfo.getChannel());
                    }
                    startDeploy(deployInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("reconnect error -------->" + e.getMessage());
                    continue;
                }
            }
        };

        TimingJob.addTask(runnable,JnaConstants.startRepeat,JnaConstants.startRepeat,TimeUnit.MILLISECONDS);
    }


    public static void downloadDeploy(DeployInfo deployInfo) {
        //download file from streamserver
        logger.info("Start downloading contracts is {}", deployInfo.getChaincodeId());
        ClassLoader classLoader = StreamDownloadUtil.class.getClassLoader();
        InputStream trustCertIn = classLoader.getResourceAsStream("root.crt");
        InputStream clientCertChainIn = classLoader.getResourceAsStream("client.crt");
        InputStream clientPrivateKeyIn = classLoader.getResourceAsStream("client.key");

        String pwd = BaapProperties.getProperty("crt.pwd");
        String host = BaapProperties.getProperty("stream.host");
        int port = Integer.parseInt(BaapProperties.getProperty("stream.grpc.port"));
        String parentPath = BaapProperties.getProperty("stream.jar.path");
        
        try {
        	String dirPath = parentPath + "/" + deployInfo.getFileId() + "/";
        	File dir = new File(dirPath);
			if (!dir.exists()) {
				boolean result = dir.mkdirs();
				if (!result) {
					logger.error("mkdirs failed!");
				}
			}
			String filePath = dirPath + deployInfo.getFileName();
			
            SslContext sslContext = StreamClientTLS.buildSslContext(trustCertIn, clientCertChainIn, clientPrivateKeyIn, pwd);
            StreamDownloadUtil download = new StreamDownloadUtil();
            boolean flag = download.download(sslContext, host, port, filePath, deployInfo.getPbk(), deployInfo.getFileHash(), deployInfo.getFileId());
            logger.info("Successful contract upload filename is:{}", deployInfo.getChaincodeId());
            deployInfo.setStatus(JnaConstants.CCDEPLOY);
            if (flag) {
                IaasRemote.iaasRemoteCall(IaasRemote.POST, IaasRemote.CREATE_CHAINCODE, deployInfo);
                synchronized (deployInfo.getChannel()) {
                    List<DeployInfo> downloadList =  Chainiaas.waitDeployMap.get(Chainiaas.getDownloadDeployInfo(deployInfo.getChannel()));
                    List<DeployInfo> startList = Chainiaas.waitDeployMap.get(Chainiaas.getStartDeployInfo(deployInfo.getChannel()));
                    if (downloadList != null) {
                        if (Objects.isNull(startList)) {
                            startList = new ArrayList<>();
                            Chainiaas.waitDeployMap.put(Chainiaas.getStartDeployInfo(deployInfo.getChannel()), startList);
                        }
                        for (int i = 0; i < downloadList.size(); i++) {
                            DeployInfo deployInfo1 = downloadList.get(i);
                            if (deployInfo1.getChaincodeAddress().equals(deployInfo.getChaincodeAddress())) {
                                startList.add(downloadList.get(i));
                                downloadList.remove(i);
                            }
                        }
                        if (downloadList.size() <= 0) {
                            logger.info("Number of contracts to be initiated is {}", startList.size());
                            Map map = new HashMap();
                            ChainiaasDTO chainIaasDTO = Chainiaas.waitStartStack.get(Chainiaas.getChainIaasinfo(deployInfo.getChannel()));
                            map.put("chainId", chainIaasDTO.getChainId());
                            String iaasResult = IaasRemote.iaasRemoteCall(IaasRemote.POST, IaasRemote.DEPLOY_DOWNLOAD, map);
                            JsonObject staticJson = new JsonParser().parse(iaasResult).getAsJsonObject();
                            String deployInfoArray = staticJson.get("data").toString();
                            DeployInfo[] deployList = mapper.readValue(deployInfoArray, DeployInfo[].class);
                            Chainiaas.downloadDeployInfoList(chainIaasDTO, deployList);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Runnable runnable = () -> {
                downloadDeploy(deployInfo);
            };
            TimingJob.addOneTimeTask(runnable, JnaConstants.startRepeat);
            e.printStackTrace();
        }
    }

    public static void startDeploy(DeployInfo deployInfo) {

        String dirPath = deployInfo.getFileId() + "/";

        String startCCrScript = MessageFormat.format(JnaConstants.startCCrScript, dirPath+deployInfo.getFileName(),
                deployInfo.getChaincodeId(), deployInfo.getChannel(), deployInfo.getChaincodeAddress()+"-"+deployInfo.getChannel());
        deployInfo.setDeployTime(System.currentTimeMillis());
//        EngineList.getStartList().add(deployInfo);
        ccReconnect.put(getDeployInfoKey(deployInfo), deployInfo);
        logger.info("Start contract script is :{}", startCCrScript);
        RuntiomeUtil.execCmd(startCCrScript);
    }


    public static DeployInfo getDeployInfoByCID(String chaincodeId, String chainId) {
        Map params = new HashMap();
        params.put("chainCodeId", chaincodeId);
        params.put("channel", chainId);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String result = IaasRemote.iaasRemoteCall(IaasRemote.POST, IaasRemote.GET_CHAINCODE, params);
        JsonObject oldCommitBlock = new JsonParser().parse(result).getAsJsonObject();
        String data = oldCommitBlock.get("data").toString();
        Optional<DeployInfo> deployInfo = null;
        try {
            deployInfo = Optional.of(mapper.readValue(data, DeployInfo.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deployInfo.get();
    }


    public static void run(EngineConf ec) {
        int port = Integer.parseInt(BaapProperties.getProperty("pdx.baap.port"));
        StringBuffer i = new StringBuffer();
        i.append(Constants.BAAP_PDX_OWNER + ":");
        i.append(Constants.BAAP_DEPLOY_NAME + ":");
        i.append(Constants.BAAP_DEPLOY_VERSION);
        String[] args = new String[]{"-a", "127.0.0.1:" + port, "-i", i.toString(), "-c", ec.getId()};
        Deploy baapDeploy = new Deploy();
        baapDeploy.start(args);
        logger.info("deploy chaincode in {} is running!", ec.getId());
    }

    public static Predicate<DeployInfo> checkDeployInfo = deployInfo -> {
        synchronized (deployInfo.getChaincodeId()) {
            if (EngineList.getRepeatDeployMap().get(deployInfo.getFileId()) != null) {
                return true;
            }
            EngineList.getRepeatDeployMap().put(deployInfo.getFileId(), false);
            return  false;
        }
    };

    @Override
    public Response init(ChaincodeStub stub) {
        return newSuccessResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        List<byte[]> args = stub.getArgs();
        Map params = new HashMap();
        String function = new String(args.get(0));
        CompletableFuture.runAsync(() -> {
            String txId;
            DeployInfo deployInfo;
            String chaincodeId;
            try {
                txId = stub.getTxId();
                logger.info("Deploy Accepted transaction Id is {}", txId);
                switch (function) {
                    case "deploy":
                        deployInfo = BaapJSONUtil.fromJson(new String(args.get(2)), DeployInfo.class);
                        if(checkDeployInfo.test(deployInfo)){
                            logger.info("Repeat Deployment contract is {}:",deployInfo.getChaincodeId());
                        }
                        downloadDeploy(deployInfo);
                        break;
                    case "start":
                        chaincodeId = new String(args.get(1));
                        try {
                            deployInfo = getDeployInfoByCID(chaincodeId, stub.getChannelId());
                        } catch (Exception e){
                        	deployInfo = new DeployInfo();
                        	deployInfo.setChaincodeId(chaincodeId);
                        	deployInfo.setChannel(stub.getChannelId());
                            ccReconnect.put(getDeployInfoKey(deployInfo), deployInfo);
                            logger.error("Start contract fail is {}:",getDeployInfoKey(deployInfo));
                            return;
                        }
                        startDeploy(deployInfo);
                        break;
                    case "stop":
                        chaincodeId = new String(args.get(1));
                        deployInfo = getDeployInfoByCID(chaincodeId, stub.getChannelId());
                        String stopCCrScript = MessageFormat.format(JnaConstants.stopDockerScript, deployInfo.getChaincodeName());
                        logger.info("Start contract script :{}", stopCCrScript);
                        RuntiomeUtil.execCmd(stopCCrScript);
                        params.put("chaincodeAddress", deployInfo.getChaincodeAddress());
                        params.put("channel", deployInfo.getChannel());
                        params.put("status", JnaConstants.CCSTOP);
                        IaasRemote.iaasRemoteCall(IaasRemote.POST, IaasRemote.UPDATE_CHAINCODE_STATUS, params);
                        break;
                    case "withdraw":
                        chaincodeId = new String(args.get(1));
                        deployInfo = getDeployInfoByCID(chaincodeId, stub.getChannelId());
                        File file = new File(JnaConstants.dappsDir + deployInfo.getFileName());
                        file.delete();
                        params.put("chaincodeAddress", deployInfo.getChaincodeAddress());
                        params.put("channel", deployInfo.getChannel());
                        params.put("status", JnaConstants.CCDROP);
                        IaasRemote.iaasRemoteCall(IaasRemote.POST, IaasRemote.UPDATE_CHAINCODE_STATUS, params);
                        break;
                }
            } catch (Exception e) {
                logger.info("error", e);
            }
        });
        return newSuccessResponse();
    }
    
    public static String getDeployInfoKey(DeployInfo deployInfo) {
    	return deployInfo.getChaincodeId() + ":" + deployInfo.getChannel();
    }

}
