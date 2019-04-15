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
package biz.pdxtech.baap.chaincode.payment;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.chaincode.driver.ethereum.Driver;
import biz.pdxtech.baap.conf.BaapProperties;
import biz.pdxtech.baap.fabric.shim.ChaincodeBaseX;
import biz.pdxtech.baap.util.ethereum.EthJRPCUtil;
import biz.pdxtech.baap.util.json.BaapJSONUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Payment extends ChaincodeBaseX {
	private static Logger logger = LoggerFactory.getLogger(Payment.class);
	
	private static Map<String, String> transferCache = new LinkedHashMap<String, String>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return this.size() > 10000;
		}
	};
	
	@Override
	public Response init(ChaincodeStub stub) {
		String function = stub.getFunction();
		if (!function.equals("init")) {
			return newErrorResponse(String.format("Unknown function: %s", function));
		}
		return newSuccessResponse();
	}

	@Override
	public Response invoke(ChaincodeStub stub) {
		HashMap<String, byte[]> meta = stub.getMeta();
		String response;
		try {
			String function = stub.getFunction();
			List<String> params = stub.getParameters();
			String chain = new String(meta.get(Constants.BAAP_ENGINE_ID));
			String engineType = getEngineType(chain);
			logger.info("engine type is {}", engineType);
			switch (function) {
				case "query":
					if (params == null || params.size() != 1) {
						logger.info("params is null !!!");
						return newErrorResponse("params error!!!".getBytes(StandardCharsets.UTF_8));
					}
					String payTxid = transferCache.get(params.get(0));
					if (StringUtils.isEmpty(payTxid)) {
						logger.info("payTxid is null !!!");
						response = "-1";
						break;
					}
					if (engineType.equals(EngineConf.EngineType.PDX.getName())) {
						Pair<String, String[]> pair = EthJRPCUtil.ofTxByTxid(payTxid.startsWith("0x") ? payTxid : "0x" + payTxid);
						Map callMap = Driver.call(chain, pair.getLeft(), pair.getRight());
						if (callMap == null || callMap.size() == 0) {
							logger.info("tx is null !!!");
							response = "-1";
							break;
						}
						String blockNum = (String) ((Map) callMap.get("result")).get("blockNumber");
						if (StringUtils.isEmpty(blockNum)) {
							logger.info("blockNum is null !!!");
							response = "-1";
							break;
						}
						int txBlockNum = Integer.parseInt((blockNum).substring(2), 16);
						String value = ((String) ((Map) callMap.get("result")).get("value"));
						
						// StreamCostUtil
						
						callMap = Driver.call(chain, EthJRPCUtil.ofBlockNumber());
						if (callMap == null || callMap.size() == 0) {
							response = "-1";
							break;
						}
						int currentBlockNum = Integer.parseInt(((String) callMap.get("result")).substring(2), 16);
						logger.info("currentBlockNum - txBlockNum = " + (currentBlockNum - txBlockNum) + "");
						if (currentBlockNum - txBlockNum >= 20) {
							response = "-1";
						} else {
							response = value;
						}
					} else {
						Transfer transfer = BaapJSONUtil.fromJson(stub.getStringState(params.get(0)), Transfer.class);
						
						BigDecimal per = new BigDecimal(1000000000000000000L);
						BigDecimal price = new BigDecimal(Double.parseDouble(transfer.getValue()));
						BigDecimal decimal = per.multiply(price);
						String hex = decimal.toBigInteger().toString(16);
						if (hex.length() % 2 == 1) {
							hex = String.format("0%s", hex);
						}
						response = hex;
					}
					logger.info("query paymentTx txid : {}, result : {}", params.get(0), response);
					break;
				case "transfer":
					Transfer transfer;
					if (engineType.equals(EngineConf.EngineType.PDX.getName())) {
						if (params == null || params.size() != 2) {
							logger.info("params error!!!");
							return newErrorResponse("params error!!!".getBytes(StandardCharsets.UTF_8));
						}
						transfer = Transfer.builder().engineType(engineType).value(params.get(1)).build();
						Thread t = new Thread(() -> {
							while (true) {
								try {
									Pair<String, String[]> data = EthJRPCUtil.ofTxByTxid("0x" + stub.getTxId());
									Map callMap = Driver.call(chain, data.getLeft(), data.getRight());
									if (callMap == null || callMap.size() == 0) {
										Thread.sleep(1000);
										continue;
									}
									String blockNum = (String) ((Map) callMap.get("result")).get("blockNumber");
									if (StringUtils.isEmpty(blockNum)) {
										Thread.sleep(1000);
										continue;
									}
									Pair<String, String[]> pair = EthJRPCUtil.ofSendRawTx(params.get(0).startsWith("0x") ? params.get(0) : "0x" + params.get(0));
									Map map = Driver.call(chain, pair.getLeft(), pair.getRight());
									assert map != null;
									if (map.get("error") != null) {
										String error = map.get("error").toString();
										logger.error("transfer error : {}", error);
										String containsVal = "known transaction:";
										if (error.contains(containsVal)) {
											String payTxId = error.substring(error.indexOf(containsVal) + containsVal.length(), error.length() - 1);
											transferCache.putIfAbsent(stub.getTxId(), payTxId.trim());
											logger.info("known transaction, engine type : {} transfer txId : {}", engineType, payTxId.trim());
										}
										break;
									}
									String payTxId = (String) map.get("result");
									transferCache.putIfAbsent(stub.getTxId(), payTxId);
									logger.info("engine type : {} transfer txId : {}", engineType, payTxId);
									break;
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
						t.start();
					} else {
						if (params == null || params.size() != 1) {
							return newErrorResponse("params error!!!".getBytes(StandardCharsets.UTF_8));
						}
						transfer = Transfer.builder().engineType(engineType).value(params.get(0)).build();
						transferCache.putIfAbsent(stub.getTxId(), stub.getTxId());
					}
					stub.putStringState(stub.getTxId(), BaapJSONUtil.toJson(transfer));
					response = stub.getTxId();
					break;
				default:
					response = "function is empty!!!";
					logger.info(response);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return newErrorResponse(e);
		}
		return newSuccessResponse(response.getBytes(StandardCharsets.UTF_8));
	}
	
	private String getEngineType(String chain) {
		if (StringUtils.isEmpty(chain)) {
			return EngineConf.EngineType.FABRIC.getName();
		} else {
			EngineConf engineConf = EngineList.getEngine(chain);
			assert engineConf != null;
			return engineConf.getType();
		}
	}
	
	public static double getCost(String hexCost) {
		long base = 1000000000000000000L;
		BigInteger bigInteger = new BigInteger(hexCost, 16);
		BigDecimal cost = new BigDecimal(bigInteger);
		return cost.divide(new BigDecimal(base)).doubleValue();
	}
	

	@Setter
	@Getter
	@Builder
	private static class Transfer {
		String engineType;
		String value;
	}
	
	public static void run(EngineConf ec) {
		int port = Integer.parseInt(BaapProperties.getProperty("pdx.baap.port"));
		StringBuffer i = new StringBuffer();
        i.append(Constants.BAAP_PDX_OWNER + ":");
        i.append(Constants.BAAP_PAYMENT_NAME + ":");
        i.append(Constants.BAAP_PAYMENT_VERSION);
		String[] args = new String[]{"-a", "127.0.0.1:" + port, "-i", i.toString(), "-c", ec.getId()};
		Payment paymentCc = new Payment();
		paymentCc.start(args);
		logger.info("payment chaincode in {} is running!", ec.getId());
	}
	
}
