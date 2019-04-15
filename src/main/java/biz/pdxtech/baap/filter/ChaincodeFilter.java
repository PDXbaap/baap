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
package biz.pdxtech.baap.filter;

import biz.pdxtech.baap.api.FilterContext;
import biz.pdxtech.baap.api.IFilter;
import biz.pdxtech.baap.chaincode.deploy.Deploy;
import biz.pdxtech.baap.chaincode.deploy.DeployInfo;
import biz.pdxtech.baap.command.IaasRemote;
import biz.pdxtech.baap.command.JnaConstants;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ChaincodeFilter implements IFilter {
	private static Logger logger = LoggerFactory.getLogger(ChaincodeFilter.class);
	
	/**
	 * Initialize chaincode filter.
	 */
	public void init(Map<String, String> conf) {
	}
	
	/**
	 * Destroy chaincode filter. Calling not guaranteed.
	 */
	public void destroy() {
	}
	
	/**
	 * Manipulating a <b>chaincode --> blockchain </b> message on the fly
	 *
	 * @param msg  msg to manipulate on the fly
	 * @param fctx next filter in the chaincode filter chain
	 * @return manipulated msg. null if black-holed (beware consequences)
	 */
	public ChaincodeMessage code2chain(ChaincodeMessage msg, FilterContext fctx) {
		
		String txid = msg.getTxid();
//		logger.info("filter code2chain {} enter", txid);
		try {
			
			
			switch (msg.getTypeValue()) {
				
				case ChaincodeMessage.Type.REGISTER_VALUE:
					
					// NOTE: If registered, REGISTERED but nothing in response payload; otherwise
					// connection is dropped.

					ChaincodeID chaincodeID = ChaincodeID.parseFrom(msg.getPayload());

					fctx.setChaincodeContext(new ChaincodeContext(fctx, chaincodeID));

					break;
				
				case ChaincodeMessage.Type.COMPLETED_VALUE:
					
					// in reply to INIT or TRANSACTION, Chaincode.Response as payload of msg
				{
				}
				
				break;
				
				case ChaincodeMessage.Type.ERROR_VALUE:
				
				{
				}
				
				break;
				
				case ChaincodeMessage.Type.GET_STATE_VALUE:
					
				{
				}
				break;
				
				case ChaincodeMessage.Type.DEL_STATE_VALUE:
					
				{
				}
				
				break;
				
				case ChaincodeMessage.Type.PUT_STATE_VALUE:
				
				{
				}
				
				break;
				
				case ChaincodeMessage.Type.GET_STATE_BY_RANGE_VALUE:
					
					// GetStateByRange in payload
					// IF COMPLETED, QueryResponse in payload.otherwise, ERROR return.
				{
				}
				
				break;
				
				case ChaincodeMessage.Type.QUERY_STATE_NEXT_VALUE:
					
					// QueryStateNext, in payload
					// IF COMPLETED, QueryResponse in payload. otherwise, ERROR return.
				{
				}
				break;
				
				case ChaincodeMessage.Type.QUERY_STATE_CLOSE_VALUE:
					
					// QueryStateClose in payload
					// IF COMPLETED, QueryResponse in payload.otherwise, ERROR return.
				{
				}
				break;
				
				case ChaincodeMessage.Type.GET_QUERY_RESULT_VALUE:
					
					// GetQueryResult in payload
					// IF COMPLETED, QueryResponse in payload.otherwise, ERROR return.
				{
				}
				break;
				
				case ChaincodeMessage.Type.GET_HISTORY_FOR_KEY_VALUE:
					
					// GetQueryResult in payload
					// IF COMPLETED, QueryResponse in payload.otherwise, ERROR return.
				{
				}
				
				break;
				
				case ChaincodeMessage.Type.INVOKE_CHAINCODE_VALUE:
					
					// invoke other chaincode
					// response to it is COMPLETED or ERROR type
				
				{
				}
				
				break;
				
				default:
					
					break;
			}
			
		} catch (InvalidProtocolBufferException e) {
			msg = null;
		}
//		logger.info("filter code2chain {} exit", txid);
		
		return fctx.code2chain(msg, fctx);
	}

	/**
	 * Manipulating a <b> blockchain --> chaincode </b> message on the fly
	 *
	 * @param msg   msg to manipulate on the fly
	 * @param chain next filter in the chaincode filter chain
	 * @return manipulated msg. null if block-holed (beware consequences)
	 */
	public ChaincodeMessage chain2code(ChaincodeMessage msg, FilterContext chain) {
		
		String txid = msg.getTxid();
//		logger.info("filter chain2code {} enter", txid);
		try {
			
			switch (msg.getTypeValue()) {
				
				case ChaincodeMessage.Type.REGISTERED_VALUE:
						if (!JnaConstants.defaultCC.contains(msg.getPayload().toStringUtf8())) {
							DeployInfo deployInfo = Deploy.getDeployInfoByCID(msg.getPayload().toStringUtf8(),msg.getChannelId());
							Map params=new HashMap();
							params.put("chaincodeAddress",deployInfo.getChaincodeAddress());
							params.put("channel",deployInfo.getChannel());
							params.put("status",JnaConstants.CCSTART);
							IaasRemote.iaasRemoteCall(IaasRemote.POST,IaasRemote.UPDATE_CHAINCODE_STATUS, params);
//							EngineList.getStartList().remove(deployInfo);
							Deploy.ccReconnect.remove(Deploy.getDeployInfoKey(deployInfo));
							Deploy.ccReconnectTimes.remove(Deploy.getDeployInfoKey(deployInfo));
						}
					// nothing in the payload, just type info
					break;
				case ChaincodeMessage.Type.INIT_VALUE:
					// NOTE: chaincode replies with ERROR or COMPLETED, referred to the same txid
					// IF error, response payload is error string; if completed, response payload is
					// Chaincode.Reponse
				{
				
				}
				break;
				
				case ChaincodeMessage.Type.TRANSACTION_VALUE:
					
					// NOTE: chaincode replies with ERROR or COMPLETED, referred to the same txid
					// IF error, response payload is error string; if completed, response payload is
					// Chaincode.Reponse
				
				{
				
				
				}
				break;
				
				case ChaincodeMessage.Type.COMPLETED_VALUE:
					
					// in reply to INIT or TRANSACTION, Chaincode.Response as payload of msg
				{
				
				}
				
				break;
				
				case ChaincodeMessage.Type.ERROR_VALUE:
				
				{
					break;
				}
				
				default:
					
					break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			msg = null;
		}
//		logger.info("filter chain2code {} exit", txid);
		
		return chain.chain2code(msg, chain);
	}
}
