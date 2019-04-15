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
import biz.pdxtech.stream.BaapStream.StreamFrame;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class StreamClientListener implements StreamObserver<StreamFrame> {

	private static Logger logger= LoggerFactory.getLogger(StreamClientListener.class);

	@Override
	public void onCompleted() {
		logger.info("receive completed message!!!");
	}

	@Override
	public void onError(Throwable t) {
		t.printStackTrace();
	}

	@Override
	public void onNext(StreamFrame stream) {
		Map<String, ByteString> meta = stream.getMetaMap();
		String streamId = meta.get(Constants.BAAP_STREAM_ID).toStringUtf8();
		ChaincodeService.getInstance().stream2code(streamId, stream);
	}

}
