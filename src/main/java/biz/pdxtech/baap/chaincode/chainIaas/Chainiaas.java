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
package biz.pdxtech.baap.chaincode.chainIaas;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.chaincode.deploy.Deploy;
import biz.pdxtech.baap.chaincode.deploy.DeployInfo;
import biz.pdxtech.baap.command.IaasRemote;
import biz.pdxtech.baap.command.JnaConstants;
import biz.pdxtech.baap.command.RuntiomeUtil;
import biz.pdxtech.baap.conf.BaapProperties;
import biz.pdxtech.baap.util.FileUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public class Chainiaas extends ChaincodeBase {
    public static Map<String, List<DeployInfo>> waitDeployMap = new ConcurrentHashMap<>();
    public static Map<String, ChainiaasDTO> waitStartStack = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();
    private static Logger logger = LoggerFactory.getLogger(Chainiaas.class);

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Integer randomPort(Random random) {

        return random.nextInt(300) + 30030;
    }

    public static Integer getPort() {
        Random random = new Random();
        String port = RuntiomeUtil.execCmd(MessageFormat.format(JnaConstants.randomPortScript, randomPort(random)));
        port = port.replace(",", "");
        port = port.replace("\n", "");
        List<Integer> list = EngineList.getUsePort();
        boolean flag = list.contains(Integer.parseInt(port));
        System.out.println(flag);
        if (!flag) {
            EngineList.getUsePort().add(Integer.parseInt(port));
            return Integer.parseInt(port);
        }
        return getPort();

    }

    public static void run(EngineConf ec) {
        int port = Integer.parseInt(BaapProperties.getProperty("pdx.baap.port"));
        StringBuffer i = new StringBuffer();
        i.append(Constants.BAAP_PDX_OWNER + ":");
        i.append(Constants.BAAP_CHAINIAAS_NAME + ":");
        i.append(Constants.BAAP_CHAINIAAS_VERSION);
        String[] args = new String[]{"-a", "127.0.0.1:" + port, "-i", i.toString(), "-c", ec.getId()};
        Chainiaas chainIaas = new Chainiaas();
        chainIaas.start(args);
        logger.info("chainIaas chaincode in {} is running!", ec.getId());
    }

    public static void createChainStack(ChainiaasDTO chainIaasDTO, Integer delayTime) {
        try {
            String chainId = chainIaasDTO.getChainId();
            String enode = chainIaasDTO.getEnode();
            String genesis = chainIaasDTO.getGenesis();
            String address = chainIaasDTO.getAddress();
            String type = chainIaasDTO.getType();

            Map map = new HashMap();
            map.put("chainId", chainId);
            map.put("rpcPort", chainIaasDTO.getRpcPort());
            map.put("p2pPort", chainIaasDTO.getSynPort());
            logger.info("engine type is {}", type);
            logger.info("Current node eNode is :{} ", enode);
            logger.info("Received eNode is :{}", EngineList.getEngineList().get(0).getEnode());

            String staticNode = "";
            String iaasResult = IaasRemote.iaasRemoteCall(IaasRemote.POST, IaasRemote.GET_STATICNODE, map);
            JsonObject staticJson = new JsonParser().parse(iaasResult).getAsJsonObject();
            staticNode = staticJson.get("data").toString();

            FileUtil.createJsonFile(genesis, MessageFormat.format(JnaConstants.genesisDir, chainId), JnaConstants.genesisFile);

            String staticDir = MessageFormat.format(JnaConstants.staticNodeDir, chainId);
            FileUtil.createJsonFile(staticNode, staticDir, JnaConstants.staticNodeFile);
            String startDockerScript = MessageFormat.format(JnaConstants.startDockerScript, chainIaasDTO.getSynPort(), chainIaasDTO.getEthccPort(), chainIaasDTO.getRpcPort(), chainId, type, address, type);
            startDockerScript = startDockerScript.replace(",", "");
            String result = RuntiomeUtil.execCmd(startDockerScript);
            logger.info("chainiaas Container startup script is:{}", startDockerScript);
            logger.info("chainiaas Start execution results is:{}", result);

        } catch (Exception e) {
            logger.info("error", e);
        }
    }

    public static void downloadDeployInfoList(ChainiaasDTO chainIaasDTO, DeployInfo[] deployList) {

        waitStartStack.put(getChainIaasinfo(chainIaasDTO.getChainId()), chainIaasDTO);
        waitDeployMap.put(getDownloadDeployInfo(chainIaasDTO.getChainId()), new ArrayList(Arrays.asList(deployList)));
        logger.info("The number of contracts to be downloaded is {}", deployList.length);
        if (deployList.length <= 0) {
            Chainiaas.createChainStack(chainIaasDTO, 0);
        }
        for (DeployInfo deployInfo : deployList) {
            if (Deploy.checkDeployInfo.test(deployInfo)) {
                logger.info("Repeat Deployment contract is {}:", deployInfo.getChaincodeId());
                continue;
            }
            Deploy.downloadDeploy(deployInfo);
        }
    }

    public static String getChainIaasinfo(String chainId) {
        return chainId + "info";

    }

    public static String getStartDeployInfo(String chainId) {
        return chainId + "start";

    }

    public static String getDownloadDeployInfo(String chainId) {
        return chainId + "Download";

    }

    @Override
    public Chaincode.Response init(ChaincodeStub stub) {

        String function = stub.getFunction();
        if (!function.equals("init")) {
            return newErrorResponse(String.format("Unknown function: %s", function));
        }
        return newSuccessResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        CompletableFuture.runAsync(() -> {
            try {
                String txId = stub.getTxId();
                logger.info("chainIaas Accepted transaction Id is {}", txId);
                String function = stub.getFunction();
                List<String> params = stub.getParameters();
                String chainId = params.get(0);
                String enode = params.get(1);
                switch (function) {
                    case "createChain":
                        String genesis = params.get(2);
                        String address = params.get(3);
                        String type = params.get(4);
                        String deployArray = params.get(5);
                        DeployInfo[] deployList = mapper.readValue(deployArray, DeployInfo[].class);
                        ChainiaasDTO chainIaasDTO = new ChainiaasDTO();
                        chainIaasDTO.setAddress(address);
                        chainIaasDTO.setChainId(chainId);
                        chainIaasDTO.setEnode(enode);
                        chainIaasDTO.setType(type);
                        chainIaasDTO.setGenesis(genesis);
                        Map replaceMap = new HashMap();
                        Gson gson = new Gson();
                        String str = "";
                        Integer synPort = 0;
                        Integer ethccPort = 0;
                        Integer rpcPort = 0;
                        if (!enode.equals(EngineList.getEngineList().get(0).getEnode())) {
                            return;
                        }
                        synchronized (EngineList.getUsePort()) {
                            while (true) {
                                ethccPort = getPort();
                                synPort = getPort();
                                rpcPort = getPort();
                                replaceMap.put(synPort.hashCode(), true);
                                replaceMap.put(ethccPort.hashCode(), true);
                                replaceMap.put(rpcPort.hashCode(), true);
                                if (replaceMap.size() == 3) {
                                    break;
                                } else {
                                    replaceMap.clear();
                                }
                            }
                        }

                        synchronized (EngineList.class) {
                            LinkedList<EngineConf> updateInstance = EngineList.getEngineList();
                            if (Objects.isNull(updateInstance)) {
                                updateInstance = new LinkedList<>();
                            } else {
                                for (EngineConf engine : updateInstance) {
                                    if (engine.getId().equals(chainId)) {
                                        return;
                                    }
                                }
                            }
                            EngineConf engineConf = new EngineConf(chainId, type, "", false, "127.0.0.1", ethccPort, rpcPort, enode);
                            updateInstance.add(engineConf);
                            str = gson.toJson(updateInstance);
                            FileUtil.createJsonFile(str, JnaConstants.engerDir, JnaConstants.engerFile);
                        }
                        chainIaasDTO.setRpcPort(rpcPort);
                        chainIaasDTO.setEthccPort(ethccPort);
                        chainIaasDTO.setSynPort(synPort);
                        downloadDeployInfoList(chainIaasDTO, deployList);
                        break;
                    case "deleteChain":
                        String stopScript = MessageFormat.format(JnaConstants.stopDockerScript, "pdx-chain-" + chainId);
                        RuntiomeUtil.execCmd(stopScript);
                        break;
                }
            } catch (Exception e) {
                logger.info("error", e);
            }
        });
        return newSuccessResponse("success".getBytes(StandardCharsets.UTF_8));
    }

}
