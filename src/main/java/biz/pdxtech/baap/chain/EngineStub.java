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

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.*;

public class EngineStub {
    @Getter
    private EngineConf engine;
    private ManagedChannel channel;

    @Getter
    private StreamObserver<ChaincodeMessage> to_blochchain;

    public EngineStub(EngineConf engine, ManagedChannel channel, StreamObserver<ChaincodeMessage> to_blockchain) {
        this.engine = engine;
        this.channel = channel;
        this.to_blochchain = to_blockchain;
    }
    
    public void code2chain(ChaincodeMessage msg) {
        this.to_blochchain.onNext(msg);
    }
    
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
