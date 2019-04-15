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
package biz.pdxtech.baap.chaincode.base;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chaincode.payment.Payment;
import biz.pdxtech.baap.conf.BaapProperties;
import biz.pdxtech.baap.fabric.shim.ChaincodeBaseX;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class Base extends ChaincodeBaseX {
    private static Logger logger = LoggerFactory.getLogger(Payment.class);

    @Override
    public Response init(ChaincodeStub stub) {
        return newSuccessResponse();
    }


    private Response initConfig(ChaincodeStub stub) {
        HashMap<String, byte[]> meta = stub.getMeta();
        try {
            String function = stub.getFunction();
            if (!function.equals("init")) {
                return newErrorResponse(String.format("Unknown function: %s", function));
            }
            List<String> params = stub.getParameters();
            if (params == null || params.size() != 2) {
                logger.warn("param error!!!");
                return newSuccessResponse("param error!!!".getBytes(StandardCharsets.UTF_8));
            }
            String statePubkey = stub.getStringState("pubkey");
            String senderPubkey = new String(meta.get(Constants.BAAP_SENDER_PUBLIC_KEY));
            if (StringUtils.isEmpty(statePubkey)) {
                stub.putStringState(params.get(0), params.get(1));
                stub.putStringState("pubkey", senderPubkey);
                logger.info("put key : {} value : {}", params.get(0), params.get(1));
            } else if (!statePubkey.equals(senderPubkey)) {
                logger.warn("init pubkey not equals sender pubkey!!!");
                return newSuccessResponse("init pubkey not equals sender pubkey!!!".getBytes(StandardCharsets.UTF_8));
            } else {
                stub.putStringState(params.get(0), params.get(1));
                logger.info("put key : {} value : {}", params.get(0), params.get(1));
            }
            return newSuccessResponse(stub.getTxId().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return newErrorResponse(e);
        }
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        String response;
        HashMap<String, byte[]>   meta = stub.getMeta();
        try {
            String function = stub.getFunction();
            List<String> params = stub.getParameters();
            switch (function) {
                case "init":
                    return initConfig(stub);
                case "query":
                    if (params == null || params.size() != 1) {
                        response = "param error!!!";
                        break;
                    }
                    response = stub.getStringState(params.get(0));
                    logger.info("query key : {} result : {}", params.get(0), response);
                    break;
                case "update":
                    if (params == null || params.size() != 2) {
                        response = "param error!!!";
                        break;
                    }
                    String statePubkey = stub.getStringState("pubkey");
                    if (StringUtils.isEmpty(statePubkey)) {
                        response = "init first";
                        break;
                    }
                    if (!statePubkey.equals(new String(meta.get(Constants.BAAP_SENDER_PUBLIC_KEY)))) {
                        response = "init pubkey not equals sender pubkey";
                        break;
                    }
                    stub.putStringState(params.get(0), params.get(1));
                    logger.info("update key : {} value : {}", params.get(0), params.get(1));
                    response = stub.getTxId();
                    break;
                default:
                    response = "function is empty!!!";
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return newErrorResponse(e);
        }
        logger.info("response:" + response);
        return newSuccessResponse(response.getBytes(StandardCharsets.UTF_8));
    }

    public static void run(EngineConf ec) {
        int port = Integer.parseInt(BaapProperties.getProperty("pdx.baap.port"));
        StringBuffer i = new StringBuffer();
        i.append(Constants.BAAP_PDX_OWNER + ":");
        i.append(Constants.BAAP_BASE_NAME + ":");
        i.append(Constants.BAAP_BASE_VERSION);
        String[] args = new String[]{"-a", "127.0.0.1:" + port, "-i", i.toString(), "-c", ec.getId()};
        Base baseCc = new Base();
        baseCc.start(args);
        logger.info("base chaincode in {} is running!", ec.getId());
    }

}
