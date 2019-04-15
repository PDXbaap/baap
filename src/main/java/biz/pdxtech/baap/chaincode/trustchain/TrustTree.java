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
package biz.pdxtech.baap.chaincode.trustchain;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.command.IaasRemote;
import biz.pdxtech.baap.command.JnaConstants;
import biz.pdxtech.baap.conf.BaapProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class TrustTree extends ChaincodeBase {

    private static Logger logger = LoggerFactory.getLogger(TrustTree.class);
    private static ObjectMapper mapper = new ObjectMapper();
    private Client client = Client.create();

    @Override
    public Chaincode.Response init(ChaincodeStub stub){

        String function = stub.getFunction();
        if (!function.equals("init")) {
            return newErrorResponse(String.format("Unknown function: %s", function));
        }
        return newSuccessResponse();

    }

    public static void run(EngineConf ec) {
        int port = Integer.parseInt(BaapProperties.getProperty("pdx.baap.port"));
        StringBuffer i = new StringBuffer();
        i.append(Constants.BAAP_PDX_OWNER + ":");
        i.append(Constants.BAAP_TRUSTTREE_NAME + ":");
        i.append(Constants.BAAP_TRUSTTREE_VERSION);
        String[] args = new String[]{"-a", "127.0.0.1:" + port, "-i", i.toString(), "-c", ec.getId()};
        TrustTree trustTransaction = new TrustTree();
        trustTransaction.start(args);
        logger.info("trustTransaction chaincode in {} is running!", ec.getId());
    }

    public static byte[] intToBytes2(int n) {
        byte[] b = new byte[4];

        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (24 - i * 8));

        }
        return b;
    }

    public static int byteToInt2(byte[] b) {

        int mask = 0xff;
        int temp = 0;
        int n = 0;
        for (int i = 0; i < b.length; i++) {
            n <<= 8;
            temp = b[i] & mask;
            n |= temp;
        }
        return n;
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        String response="";
        List<String> params= stub.getParameters();
        try {
            String txId = stub.getTxId();
            logger.info("TrustTree Accepted transaction Id is {}", txId);
            for(String commitTrust:params){
            logger.info("Currently accepted block parameters is :{}", commitTrust);
            CommitTrust nowCommitBlock = mapper.readValue(commitTrust, CommitTrust.class);
            String chainId=nowCommitBlock.getChainID();
            int nowBlockNum=nowCommitBlock.getCommitBlockNo();
            if(nowBlockNum<=JnaConstants.minBlock){
                stub.putState(getCommitKey(chainId,nowBlockNum),nowCommitBlock.getCommitBlockHash().getBytes());
                stub.putState(getTrustTreeHeight(chainId),intToBytes2(nowBlockNum));
                continue;
            }
            int prevBlockNum=nowBlockNum-1;
            String value= new String (stub.getState(getCommitKey(chainId,prevBlockNum)));
            logger.info("The last storage block :{}", value);
            String function = stub.getFunction();
            synchronized (chainId) {
                switch (function) {
                    case "putState":
                        String blockString;
                        if (Objects.isNull(value) || value.length() <= 0) {
                            if(Objects.isNull(EngineList.getActivEnode().get(nowCommitBlock.getChainID()))){
                               storeActiveNodes(chainId,nowCommitBlock.getPreTrustChainID());
                            }
                            try {
                                blockString = getBlock(EngineList.getActivEnode().get(chainId), nowCommitBlock.getChainID());
                            }catch(Exception e){
                                storeActiveNodes(chainId,nowCommitBlock.getPreTrustChainID());
                                blockString = getBlock(EngineList.getActivEnode().get(chainId), nowCommitBlock.getChainID());
                            }
                            logger.info("Data returned by trust Federation {}", blockString);
                            JsonObject oldCommitBlock = new JsonParser().parse(blockString).getAsJsonObject();
                            String prevHash = oldCommitBlock.get("result").toString();
                            prevHash = prevHash.replace("\"", "");
                            logger.info("The largest hash in the previous chain {}", prevHash);
                            response = changeBook(stub, prevHash, nowCommitBlock);
                        } else {
                            response = changeBook(stub, value, nowCommitBlock);
                        }
                        break;
                   }
               }
               if(response.equals("putError")){
                   break;
               }
           }
        }catch (Exception e){
            logger.info("error", e);
            return newErrorResponse(e);
        }
        return newSuccessResponse(response.getBytes(StandardCharsets.UTF_8));
    }

    public void storeActiveNodes(String chainId,String prevChainId) throws IOException {
        Map param = new HashMap();
        param.put("chainId", prevChainId);
        String result = IaasRemote.iaasRemoteCall(IaasRemote.GET, IaasRemote.GET_HOSTS, param);
        JsonObject activeHost = new JsonParser().parse(result).getAsJsonObject();
        String data = activeHost.get("data").toString();
        logger.info("Active nodes from Iaas is {}", data);
        String[] hostsList = mapper.readValue(data, String[].class);
        EngineList.getActivEnode().put(chainId,hostsList[0]);
    }

    public String changeBook(ChaincodeStub stub, String prevHash, CommitTrust nowCommitBlock) {
        String response;
        Integer prevBlockNum = nowCommitBlock.CommitBlockNo-1;
        if (nowCommitBlock.getPrevCommitBlockHash().equals(prevHash)) {
            stub.putState(getCommitKey(nowCommitBlock.getChainID(), nowCommitBlock.getCommitBlockNo()), nowCommitBlock.getCommitBlockHash().getBytes());
            stub.putState(getTrustTreeHeight(nowCommitBlock.getChainID()), intToBytes2(nowCommitBlock.getCommitBlockNo()));
            response = "putSuccess";
            logger.info("Store successful blockNumber is {}:", nowCommitBlock.getCommitBlockNo());
        } else {
            prevBlockNum--;
            stub.putState(getTrustTreeHeight(nowCommitBlock.getChainID()), intToBytes2(prevBlockNum));
            response = "putError";
            logger.info("Validation Storage Failure is {}:", nowCommitBlock.getCommitBlockNo());
        }
        return response;
    }

    public String getBlock(String selfAddress,String chainId) {
        WebResource.Builder builder = client.resource(selfAddress).type(MediaType.APPLICATION_JSON);
        String param = "{\"jsonrpc\":\"2.0\", \"method\":\"eth_getMaxCommitBlockHash\",\"params\":[\""+chainId+"\"],\"id\":67}";
        ClientResponse clientResponse = builder.entity(param).post(ClientResponse.class);
        String result = clientResponse.getEntity(String.class);
        return result;
    }

    private String getCommitKey(String chainId, int blockNum) {
        return chainId + ":" + blockNum;

    }

    private String getTrustTreeHeight(String chainId) {
        return chainId + "-trustTransactionHeight";

    }
}
