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
package biz.pdxtech.baap.command;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IaasRemote {
    private static Logger logger = LoggerFactory.getLogger(IaasRemote.class);
    /**
     * 修改节点状态
     */
    public static final String UPDATE_NODE_STATUS = "/rest/iaas/update_chain_node_status";
    /**
     * 从iaas查询staticNode
     */
    public static final String GET_STATICNODE = "/rest/chain/staticNodes";
    /**
     * 注册智能合约
     */
    public static final String CREATE_CHAINCODE = "/rest/deploy/create";
    /**
     * 删除智能合约
     */
    public static final String DELETE_CHAINCODE = "/rest/deploy/delete";
    /**
     * 修改智能合约运行状态
     */
    public static final String UPDATE_CHAINCODE_STATUS = "/rest/deploy/update";
    /**
     * 查询注册过的智能合约
     */
    public static final String GET_CHAINCODE = "/rest/deploy/queryDeployByChainCodeIdAndChannel";
    /**
     * 检查活跃节点
     */
    public static final String GET_HOSTS = "/rest/chain/hosts";
    /**
     * 请求协议类型
     */
    public static final String AGREEMENT = "http://";
    /**
     * 查看可启动的合约
     */
    public static final String DEPLOY_DOWNLOAD="/rest/deploy/undownload";


    /**
     * 请求类型
     */
    public static String POST = "POST"; //已部署

    public static String GET = "GET"; //已启动

    /**
     *
     * @param method  请求类型
     * @param iaasRest    iaas接口
     * @param params  请求参数
     * @return
     */
    public static String iaasRemoteCall(String method,String iaasRest, Object params) {
        Gson gson=new Gson();
        HttpClientUtil httpClientUtil=new HttpClientUtil();
        String paramsJson=gson.toJson(params);
        if(!checkMethod(method)){
            logger.error("Error request type");
        }
        StringBuilder requestUrl = new StringBuilder();
        requestUrl.append(AGREEMENT);
        requestUrl.append(JnaConstants.iaasHost);
        requestUrl.append(iaasRest);
        logger.info("The build request is:{} >>> params is {}",requestUrl,paramsJson);
        if(method.equals("GET")){
            return httpClientUtil.doGet(requestUrl.toString(),(Map<String,String>)params);
        }else {
            return httpClientUtil.doPost(requestUrl.toString(), params);
        }
    }


    public static boolean checkMethod(String method){
        if(method==null||!method.equals("GET")&&!method.equals("POST")){
            return false;
        }
        return true;
    }
}
