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
package biz.pdxtech.baap.chaincode.driver.ethereum;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.conf.BaapProperties;
import biz.pdxtech.baap.driver.BlockChainDriverFactory;
import biz.pdxtech.baap.driver.BlockchainDriver;
import biz.pdxtech.baap.driver.BlockchainDriverException;
import biz.pdxtech.baap.driver.ethereum.EthereumBlockchainDriver;
import biz.pdxtech.baap.util.ethereum.EthJRPCUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Driver {
	private static Logger log = LoggerFactory.getLogger(Driver.class);
	
	private static ConcurrentHashMap<String, BlockchainDriver> drivers = new ConcurrentHashMap<>();
	
	public static BlockchainDriver getInstance(String chain) throws BlockchainDriverException {
		if (!drivers.containsKey(chain)) {
			synchronized (Driver.class) {
				if (!drivers.containsKey(chain)) {
					EngineConf engine = EngineList.getEngine(chain);
					if (engine == null) {
						throw new BlockchainDriverException("Engine Empty");
					}
					Properties properties = new Properties();
					properties.setProperty(Constants.BAAP_ENGINE_TYPE_KEY, Constants.BAAP_ENGINE_TYPE_ETHEREUM);
					properties.setProperty(Constants.BAAP_ENGINE_URL_HOST_KEY, "http://" + engine.getHost() + ":" + engine.getRpcport());
					properties.setProperty(Constants.BAAP_SENDER_PRIVATE_KEY, BaapProperties.getProperty("pdx.baap.privateKey"));
					log.info("{}={}:{}", Constants.BAAP_ENGINE_URL_HOST_KEY, engine.getHost(), engine.getRpcport());
					
					if (!drivers.containsKey(chain)) {
						drivers.put(chain, BlockChainDriverFactory.get(properties));
					}
					
				}
			}
		}
		return drivers.get(chain);
	}
	
	
	public static Map call(String chain, String method, String... data) {
		try {
			BlockchainDriver driver = getInstance(chain);
			String response = ((EthereumBlockchainDriver) driver).call(method, data);
			return EthJRPCUtil.ofResult(response);
		} catch (BlockchainDriverException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		Pair<String, String[]> pair = EthJRPCUtil.ofTxByTxid("0x5a4a9d514db47216931f8f31c0419f82707169237aec8b1aa5c2a7ef4458a0e3");
		Map map = Driver.call("739", pair.getLeft(), pair.getRight());
		System.out.println(map);
		String blockNumber = ((String) ((Map) map.get("result")).get("blockNumber")).substring(2);
		System.out.println(Integer.parseInt(blockNumber, 16));
		
		String value = ((String) ((Map) map.get("result")).get("value")).substring(2);
		System.out.println(Integer.parseInt(value, 16));
		
		
		map = Driver.call("739", EthJRPCUtil.ofBlockNumber());
		System.out.println(Integer.parseInt(((String) map.get("result")).substring(2), 16));
	}
}
