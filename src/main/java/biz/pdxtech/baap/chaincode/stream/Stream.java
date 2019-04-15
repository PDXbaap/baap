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
package biz.pdxtech.baap.chaincode.stream;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.conf.BaapProperties;
import biz.pdxtech.baap.fabric.shim.ChaincodeBaseX;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class Stream extends ChaincodeBaseX {
	
	private static Logger logger = LoggerFactory.getLogger(Stream.class);
	
	@Override
	public Response init(ChaincodeStub stub) {
		return newSuccessResponse();
	}

	public static void run(EngineConf ec) {
		int port = Integer.parseInt(BaapProperties.getProperty("pdx.baap.port"));
		StringBuffer i = new StringBuffer();
        i.append(Constants.BAAP_PDX_OWNER + ":");
        i.append(Constants.BAAP_STREAM_NAME + ":");
        i.append(Constants.BAAP_STREAM_VERSION);
		String[] args = new String[]{"-a", "127.0.0.1:" + port, "-i", i.toString(), "-c", ec.getId()};
		Stream streamingCc = new Stream();
		streamingCc.start(args);
		logger.info("stream chaincode in {} is running!", ec.getId());
	}
	
	@Override
	public Response invoke(ChaincodeStub stub) {
		HashMap<String, byte[]> meta = stub.getMeta();
		String response = "";
		try {
			String function = stub.getFunction();
			List<String> params = stub.getParameters();
			switch (function) {
				case "query":
					response = stub.getStringState(params.get(0));
					logger.info("get key : {} result : {}", params.get(0), response);
					break;
				case "put":
					stub.putStringState(stub.getTxId(), params.get(0));
					logger.info("put key : {} value : {}", stub.getTxId(), params.get(0));
					response = stub.getTxId();
					break;
				default:
					response = "function is empty!!!";
					logger.info(response);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return newSuccessResponse(response.getBytes(StandardCharsets.UTF_8));
	}

}