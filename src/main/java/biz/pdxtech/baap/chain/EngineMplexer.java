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

import biz.pdxtech.baap.chaincode.deploy.Deploy;
import biz.pdxtech.baap.chaincode.deploy.DeployInfo;
import biz.pdxtech.baap.command.IaasRemote;
import biz.pdxtech.baap.command.JnaConstants;
import biz.pdxtech.baap.filter.FilterFramework;
import biz.pdxtech.baap.filter.ScheduledExecutor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc.ChaincodeSupportStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Forward messages coming from the chaincode to specific blockchain engine.
 *
 * @author jz
 */
public class EngineMplexer implements StreamObserver<ChaincodeMessage> {
	
	private static final Logger logger = LoggerFactory.getLogger(EngineMplexer.class);
	
	StreamObserver<ChaincodeMessage> to_chaincode;
	
	ChaincodeID chaincodeID;
	
	EngineStub engineStub = null;
	
	Map<String, String> txid2chain = new ConcurrentHashMap<>();


	public EngineMplexer(ChaincodeMessage msg, StreamObserver<ChaincodeMessage> to_chaincode, ChaincodeID chaincodeID, String chainId) {

		this.to_chaincode = to_chaincode;

		this.chaincodeID = chaincodeID;

		EngineConf engine = EngineList.getInstanceMapping().get(chainId);

		ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(engine.getHost(), engine.getPort());

		if (engine.isTls())
			builder = builder.useTransportSecurity();
		else
			builder = builder.usePlaintext(true);

		ManagedChannel channel = builder.build();

		ChaincodeSupportStub asyncStub = ChaincodeSupportGrpc.newStub(channel);

		StreamObserver<ChaincodeMessage> from_blockchain = new StreamObserver<ChaincodeMessage>() {
			@Override
			public void onCompleted() {
				logger.debug("completed by onCompleted");
				to_chaincode.onCompleted();
			}

			@Override
			public void onError(Throwable t) {
				engine.setConnect(false);
				logger.debug("cancelled by blockchain");
				t.printStackTrace();
				to_chaincode.onCompleted();
			}

			/**
			 * chain2code
			 */
			@Override
			public void onNext(ChaincodeMessage msg) {
				logger.debug("message from blockchain, massaging before forwarding to chaincode");

				msg = FilterFramework.getInstance().chain2code(msg);

				if (msg == null)
					return;

				if (msg.getType() == ChaincodeMessage.Type.INIT
						|| msg.getType() == ChaincodeMessage.Type.TRANSACTION || msg.getType() == ChaincodeMessage.Type.BAAP_QUERY) {
					txid2chain.put(msg.getTxid(), engine.getId());
				}

				to_chaincode.onNext(msg);
			}
		};

		StreamObserver<ChaincodeMessage> to_blockchain = asyncStub.register(from_blockchain);

		engineStub=new EngineStub(engine, channel, to_blockchain);
		to_blockchain.onNext(msg);
	}

	/**
	 * code2chain: called by ChaincodeStream on new messages from chaincode
	 */
	@Override
	public void onNext(ChaincodeMessage msg) {
		logger.debug("message from chainchode, massaging before forwarding to blockchain");
		msg = FilterFramework.getInstance().code2chain(msg);
		if (msg == null)
			return;
		
		if (msg.getType() == ChaincodeMessage.Type.REGISTER) {
			// forward to all blockchain engines
            engineStub.code2chain(msg);
            return;
		}
		
		String txid = msg.getTxid();
		
		// it cannot happen as all code->chain messages will have txid
		if (txid == null) {
			return;
		}
		
		// Figure out which chain, & forward accordingly.
		engineStub.code2chain(msg);
		
		// clean up txid-> engine mapping as we no longer need it
		if (msg.getType() == ChaincodeMessage.Type.RESPONSE || msg.getType() == ChaincodeMessage.Type.ERROR) {
			this.txid2chain.remove(txid);
		}
	}
	
	@Override
	public void onError(Throwable t) {
		DeployInfo deployInfo=Deploy.getDeployInfoByCID(chaincodeID.getName(),engineStub.getEngine().getId());
		Map params = new HashMap();
		params.put("chaincodeAddress", deployInfo.getChaincodeAddress());
		params.put("channel", deployInfo.getChannel());
		params.put("status", JnaConstants.CCSTOP);
		IaasRemote.iaasRemoteCall(IaasRemote.POST, IaasRemote.UPDATE_CHAINCODE_STATUS, params);
		Deploy.startDeploy(deployInfo);
		logger.error("engine:cancelled by chaincode");
		this.shutdown();
	}
	
	@Override
	public void onCompleted() {
		logger.debug("engine:completed by chaincode");
		this.shutdown();
	}
	
	public void shutdown() {
		logger.info("cancelled by shutdown");
		engineStub.shutdown();
	}

	public void closeTOBlochchain(){
		engineStub.getTo_blochchain().onCompleted();
	}

}
