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

import biz.pdxtech.stream.BaapStream.StreamFrame;
import io.grpc.stub.StreamObserver;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChaincodeService extends ChaincodeSupportGrpc.ChaincodeSupportImplBase {
    
    private static ChaincodeService instance;
    
    // streaming streamId -> streaming connection mapping
    Map<String, ChaincodeStream> chaincodeStreamMap = new ConcurrentHashMap<>();
    
    Map<String, String> chaincodeNameMap = new ConcurrentHashMap<>();
    
    public static ChaincodeService getInstance() {
        if (instance == null) {
            instance = new ChaincodeService();
        }
        return instance;
    }
    
    private ChaincodeService() { }
    
    public StreamObserver<ChaincodeMessage> register(io.grpc.stub.StreamObserver<ChaincodeMessage> responseObserver) {
        ChaincodeStream conn = new ChaincodeStream(responseObserver);
        return conn;
    }
    
    /**
     * called by StreamConn on streaming to chaincode
     * 
     * @param streamId
     * @param frame
     */
    public void stream2code(String streamId, StreamFrame frame) {
        // recv'd frame from a connected client
        // find the right client and send frame to it
        ChaincodeStream cc = this.chaincodeStreamMap.get(streamId);
        ChaincodeMessage msg = ChaincodeMessage.newBuilder()
        				.setType(ChaincodeMessage.Type.BAAP_STREAM)
        				.setTxid(streamId)
                        .setPayload(frame.toByteString()).build();
        cc.send2cc(msg);
    }
    
    public void shutdown() {
    }
    
    public void putChaincodeStream(String streamId, ChaincodeStream chaincodeStream) {
        this.chaincodeStreamMap.put(streamId, chaincodeStream);
    }

    public void putChaincodeStreamIfAbsent(String streamId, ChaincodeStream chaincodeStream) {
        this.chaincodeStreamMap.putIfAbsent(streamId, chaincodeStream);
    }
    
    public void remChaincodeStream(String streamId) {
        this.chaincodeStreamMap.remove(streamId);
    }

    public Map<String, ChaincodeStream> getChaincodeStream() {
        return this.chaincodeStreamMap;
    }

    public void pubChaincodeName(String stream, String chaincodeName) {
    	chaincodeNameMap.put(stream, chaincodeName);
    }
    
    public void remChaincodeName(String stream) {
    	chaincodeNameMap.remove(stream);
    }
    
    public String getChaincodeName(String stream) {
    	return chaincodeNameMap.get(stream);
    }
}
