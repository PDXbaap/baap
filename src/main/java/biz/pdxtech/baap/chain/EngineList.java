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
package biz.pdxtech.baap.chain;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.chaincode.base.Base;
import biz.pdxtech.baap.chaincode.chainIaas.Chainiaas;
import biz.pdxtech.baap.chaincode.deploy.Deploy;
import biz.pdxtech.baap.chaincode.deploy.DeployInfo;
import biz.pdxtech.baap.chaincode.payment.Payment;
import biz.pdxtech.baap.chaincode.stream.Stream;
import biz.pdxtech.baap.chaincode.trustchain.TrustTree;
import biz.pdxtech.baap.command.IaasRemote;
import biz.pdxtech.baap.command.JnaConstants;
import biz.pdxtech.baap.node.ChaincodeService;
import biz.pdxtech.baap.node.ChaincodeStream;
import biz.pdxtech.baap.util.BaapPath;
import biz.pdxtech.baap.util.LatchUtil;
import biz.pdxtech.baap.util.json.BaapJSONUtil;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Nonnull
public class EngineList extends LinkedList<EngineConf> {
	
	private static final Logger logger = LoggerFactory.getLogger(EngineList.class);
	private static final long serialVersionUID = -7426155001147722195L;
	private static EngineList instance = new EngineList();
    private static Map<String,Boolean> repeatDeployMap=new ConcurrentHashMap<>();
	private static ArrayList<Integer> usePort = new ArrayList<>();
	private static Map<String,String> activEnode=new ConcurrentHashMap();
	static {
		usePort.add(30100);
		usePort.add(30150);
		usePort.add(30200);
	}
	private static Map<String, EngineConf> instanceMapping = new HashMap<>();
	private EngineList() {
	
	}
	
	public static EngineConf getEngine(String chain) {
		init();
		return instanceMapping.get(chain);
	}

	public static EngineConf getFabricEngine() {
		init();
		for (EngineConf engineConf:instanceMapping.values()){
			if (engineConf.getType().equals(Constants.BAAP_ENGINE_TYPE_FABRIC)){
				return engineConf;
			}
		}
		return null;
	}
	
	public static LinkedList<EngineConf> getEngine() {
		init();
		return instance;
	}
	public static Map<String,String>  getActivEnode() {
		return activEnode;
	}

    public static Map<String, Boolean>  getRepeatDeployMap() {
        return repeatDeployMap;
    }
	public static Map<String, EngineConf> getInstanceMapping() {
		return instanceMapping;
	}

	public static ArrayList<Integer> getUsePort() {
		return usePort;
	}
	public static void init() {
		if (instance == null || instance.size() == 0) {
			synchronized (EngineList.class) {
				if (instance == null || instance.size() == 0) {
					instance = getEngineList();
					instance.forEach(a -> instanceMapping.put(a.getId(), a));
				}
			}
		}
	}

	public static EngineList getEngineList(){

		String conf = "";
		try {
			conf = IOUtils.toString(new FileInputStream(new File(JnaConstants.engerDir+JnaConstants.engerFile)));
			if (Strings.isEmpty(conf)) {
				conf = IOUtils.toString(new FileInputStream(BaapPath.getConfEngine()), "utf-8");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return BaapJSONUtil.fromJson(conf, EngineList.class);
	}

	// 监听变化后更新
	public static void updateInstance() {
		synchronized (EngineList.class) {
			LinkedList<EngineConf> updateInstance = getEngineList();
			// 新增时,只try新增到engconieConf
			LinkedList<EngineConf> list1 = new EngineList();
			list1.addAll(instance);
			LinkedList<EngineConf> list2 = new EngineList();
			list2.addAll(updateInstance);

			// // 只保留新增部分 删除 删除的节点配置
			for (int i = 0;i<list1.size();i++){
				int a = 0;
                    for (int j = 0;j<list2.size();j++){
					if(list1.get(i).getId().equals(list2.get(j).getId())){
						a++;
						updateInstance.remove(list2.get(j));
						break;
					}
				}
			 if (a == 0){
					// 在连配置被删除
					if (list1.get(i).isConnect()){
						// 关闭其链接
						Map<String, ChaincodeStream> chaincodeStreamMap = ChaincodeService.getInstance().getChaincodeStream();
						int finalI = i;
						chaincodeStreamMap.forEach((k, v)->{
							if (k.contains(list1.get(finalI).getId())){
								v.getTo_blockchain().closeTOBlochchain();
							}
						});
						logger.info("关闭engnioeConf配置链接,id:" + list1.get(i).getId() + ",host:" + list1.get(i).getHost() + ",prot:" + list1.get(i).getPort());
					}
					instance.remove(list1.get(i));
					instanceMapping.remove(list1.get(i).getId());
				}
			}

			// updateInstance追加到instance中
			updateInstance.forEach(ec -> {
				instance.add(ec);
				instanceMapping.put(ec.getId(), ec);
			});
			for (EngineConf engineConf:instance){
				System.out.println("chainId="+engineConf.getId()+"===========isConnection"+engineConf.connect);
			}
		}
	}


	public static void engineRun() throws IOException {
		getEngine();
		instance.forEach(ec -> tryConnect(ec));
	}

	/*
	* 尝试链接ec
	* */
	public static void tryConnect(EngineConf ec){
			if (!LatchUtil.checkClient(ec)){
				ec.setConnect(false);
				Gson gson=new Gson();
				String str;
				try {
					synchronized (EngineList.class) {
						if (JnaConstants.deleteHashSet.contains(ec.getId())) {
							JnaConstants.deleteHashSet.remove(ec.getId());
							LinkedList<EngineConf> updateInstance = EngineList.getEngineList();
							if (Objects.isNull(updateInstance)) {
								updateInstance = new LinkedList<>();
							}
							updateInstance.stream().filter(x-> x.getId() != ec.getId()).collect(Collectors.toList());
							str = gson.toJson(updateInstance);
							FileOutputStream fileOutputStream = new FileOutputStream(new File(JnaConstants.engerDir+JnaConstants.engerFile));
							fileOutputStream.write(str.getBytes());
							fileOutputStream.flush();
                            Map params=new HashMap();
                            params.put("chainId",ec.getId());
                            params.put("status",JnaConstants.NODESTOP);
                            IaasRemote.iaasRemoteCall(IaasRemote.POST,IaasRemote.UPDATE_NODE_STATUS,params);
						}
				    	}
					} catch (Exception e) {
						e.printStackTrace();
					}
				logger.info("失败：：：：链接ec：" + ec.getId() + ",host:" + ec.getHost() + ",port:"+ ec.getPort());
			} else {

				//TODO 通知中心化服务改变状态
				Payment.run(ec);
				Base.run(ec);
				Stream.run(ec);
				Deploy.run(ec);
				Chainiaas.run(ec);
				TrustTree.run(ec);
                Map params=new HashMap();
                params.put("chainId",ec.getId());
                params.put("status",JnaConstants.NODESTART);
                IaasRemote.iaasRemoteCall(IaasRemote.POST,IaasRemote.UPDATE_NODE_STATUS,params);
				JnaConstants.deleteHashSet.add(ec.getId());
				ec.setConnect(true);
				List<DeployInfo> downloadList = Chainiaas.waitDeployMap.get(ec.getId());
				if(!Objects.isNull(downloadList)) {
					List<DeployInfo> startList = Chainiaas.waitDeployMap.get(Chainiaas.getStartDeployInfo(ec.getId()));
					if (startList != null) {
						logger.info("Start Number of contracts to be initiated is {}", startList.size());
						for (int i = 0; i < startList.size(); i++) {
							DeployInfo deployInfo = startList.get(i);
							Deploy.startDeploy(deployInfo);
						}
					}
					Chainiaas.waitDeployMap.remove(Chainiaas.getDownloadDeployInfo(ec.getId()));
					Chainiaas.waitDeployMap.remove(Chainiaas.getStartDeployInfo(ec.getId()));
					Chainiaas.waitDeployMap.remove(Chainiaas.getChainIaasinfo(ec.getId()));
				}
				logger.info("成功：：：：链接ec：" + ec.getId() + ",host:" + ec.getHost() + ",port:"+ ec.getPort());
			}
	}

}