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
package biz.pdxtech.baap.node;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.api.Deployment;
import biz.pdxtech.baap.api.Invocation;
import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.chain.EngineMplexer;
import biz.pdxtech.baap.chaincode.driver.ethereum.Driver;
import biz.pdxtech.baap.driver.BlockchainDriver;
import biz.pdxtech.baap.util.encrypt.EncryptUtil;
import biz.pdxtech.baap.util.json.BaapJSONUtil;
import biz.pdxtech.baap.util.stream.StreamMessage;
import biz.pdxtech.stream.BaapStream.StreamFrame;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChaincodeStream implements StreamObserver<ChaincodeMessage> {
	
	private static final Logger logger = LoggerFactory.getLogger(ChaincodeStream.class);
	
	ChaincodeID chaincodeID;
	String chainId;
	
	protected StreamObserver<ChaincodeMessage> to_chaincode;
	
	protected EngineMplexer to_blockchain;

	public ChaincodeStream(StreamObserver<ChaincodeMessage> chaincode) {
		this.to_chaincode = chaincode;
	//  this.to_blockchain = new EngineMplexer(this.to_chaincode);
	}
	
	private volatile boolean stopped = false;
	
	public void shutdown() throws InterruptedException {
		this.stopped = true;
		this.to_blockchain.shutdown();
		ChaincodeService.getInstance().remChaincodeStream(chainId+this.chaincodeID.getName());
	}
	
	public void send2cc(ChaincodeMessage msg) {
		this.to_chaincode.onNext(msg);
	}
	
	@Override
	public void onNext(ChaincodeMessage msg) {
		
		logger.info("message from chainchode");
		
		if (msg.getType() == ChaincodeMessage.Type.REGISTER) {
			try {
				ChaincodeID chaincodeID = ChaincodeID.parseFrom(msg.getPayload());
				this.setChaincodeID(msg.getChannelId(),chaincodeID);
				String channelId = msg.getChannelId();
				if (StringUtils.isEmpty(channelId)) {
					EngineConf fabricEngine = EngineList.getFabricEngine();
					if (fabricEngine != null) {
						channelId = fabricEngine.getId();
					}
				}
				this.to_blockchain = new EngineMplexer(msg,this.to_chaincode, chaincodeID, channelId);
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
			return;
		}
		
		if (msg.getType() == ChaincodeMessage.Type.BAAP_STREAM) {
			ChaincodeService chaincodeService = ChaincodeService.getInstance();
			try {
				StreamFrame frame = StreamFrame.parseFrom(msg.getPayload());
				Map<String, ByteString> metaMap = frame.getMetaMap();
				String streamId = metaMap.get(Constants.BAAP_STREAM_ID).toStringUtf8();
				chaincodeService.putChaincodeStreamIfAbsent(streamId, this);
				ByteString index = metaMap.get(Constants.BAAP_STREAM_INDEX);
				if (index != null && index.toStringUtf8().equals(Constants.BAAP_STREAM_CLOSE)) {
					StreamClientService.getInstance().close(streamId);
				} else {
					String streamTxId = metaMap.get(Constants.BAAP_STREAM_TXID).toStringUtf8();
					String chainId = metaMap.get(Constants.BAAP_ENGINE_ID).toStringUtf8();
					try {
						BlockchainDriver driver;
						EngineConf engine = EngineList.getEngine(chainId);
						if (engine.getType().equals(Constants.BAAP_ENGINE_TYPE_FABRIC)) {
//							driver = FabricClient.instance().initDriver(String.format("http://127.0.0.1:%s", engine.getRpcport()));
							logger.error("Unsupported types!");
							return;
						} else {
							driver = Driver.getInstance(chainId);
						}
						
//						Chaincode streamChaincode = Chaincode.builder().name("baap-stream").version("v1.0").chain(chainId).build();
//						Transaction tx = Transaction.builder().fcn("query").params(new ArrayList<byte[]>() {{
//							add(streamTxId.getBytes());
//						}}).build();
						
						Deployment streamCc = Deployment.builder().owner(Constants.BAAP_PDX_OWNER)
			                    .name(Constants.BAAP_STREAM_NAME)
			                    .version(Constants.BAAP_STREAM_VERSION).build();
						Invocation inv = Invocation.builder().fcn("query").args(new ArrayList<byte[]>() {{
							add(streamTxId.getBytes());
						}}).build();
						
						byte[] query = driver.query(getToFromDpl(streamCc), inv);
						StreamMessage message = BaapJSONUtil.fromJson(new String(query), StreamMessage.class);
						if (message == null) {
							Map<String, ByteString> meta = new HashMap<>();
							meta.put(Constants.BAAP_STAT, ByteString.copyFromUtf8(Constants.BAAP_STAT_FAIL + ""));
							meta.put(Constants.BAAP_STAT_REASON, ByteString.copyFromUtf8(String.format("streamTxId : %s is not exist!", streamTxId)));
							StreamFrame streamFrame = StreamFrame.newBuilder().putAllMeta(meta).build();
							ChaincodeMessage chaincodeMessage = ChaincodeMessage.newBuilder().setType(ChaincodeMessage.Type.BAAP_STREAM).setPayload(streamFrame.toByteString()).build();
							send2cc(chaincodeMessage);
							return;
						}
						String chaincodeId = this.chaincodeID.getName();
//						Chaincode myChaincode = message.getChaincode();
//						String txChaincodeId = String.format("%s:%s", myChaincode.getName(), myChaincode.getVersion());
						String txChaincodeId = message.getCcAddress();
						if (!txChaincodeId.equals(EncryptUtil.keccak256ToAddress(chaincodeId))) {
							Map<String, ByteString> meta = new HashMap<>();
							meta.put(Constants.BAAP_STAT, ByteString.copyFromUtf8(Constants.BAAP_STAT_FAIL + ""));
							meta.put(Constants.BAAP_STAT_REASON, ByteString.copyFromUtf8(String.format("chaincodeId :%s do not match!", txChaincodeId)));
							StreamFrame streamFrame = StreamFrame.newBuilder().putAllMeta(meta).build();
							ChaincodeMessage chaincodeMessage = ChaincodeMessage.newBuilder().setType(ChaincodeMessage.Type.BAAP_STREAM).setPayload(streamFrame.toByteString()).build();
							send2cc(chaincodeMessage);
							return;
						}
						StreamClientService.getInstance().stream2Ss(frame);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		} else {
			// FORWARD message from chaincode to specific blockchain
			this.to_blockchain.onNext(msg);
		}
	}
	
	@Override
	public void onError(Throwable t) {
		logger.debug("cancelled by chaincode");
		this.to_blockchain.onError(t);
		this.to_chaincode.onError(t);
		ChaincodeService.getInstance().remChaincodeStream(chainId+this.chaincodeID.getName());
	}
	
	@Override
	public void onCompleted() {
		logger.debug("completed by chaincode");
		this.to_blockchain.onCompleted();
		this.to_chaincode.onCompleted();
		ChaincodeService.getInstance().remChaincodeStream(chainId+this.chaincodeID.getName());
	}
	
	public ChaincodeID getChaincodeID() {
		return chaincodeID;
	}
	
	public void setChaincodeID(String chainId,ChaincodeID chaincodeID) {
		this.chaincodeID = chaincodeID;
		this.chainId=chainId;
		ChaincodeService.getInstance().putChaincodeStream(chainId+chaincodeID.getName(), this);
	}

	public EngineMplexer getTo_blockchain(){
		return to_blockchain;
	}
	
	private String getToFromDpl(Deployment dpl) {
		return EncryptUtil.keccak256ToAddress(dpl.getOwner() + ":" + dpl.getName() + ":" + dpl.getVersion());
	}
}