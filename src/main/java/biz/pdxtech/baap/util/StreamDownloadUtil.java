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
package biz.pdxtech.baap.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import biz.pdxtech.stream.BaapStream.StreamFrame;
import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.baap.util.grpc.StreamClientTLS;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;


public class StreamDownloadUtil {

    private static Logger logger = LoggerFactory.getLogger(StreamDownloadUtil.class);
    
    public boolean download(SslContext sslContext, String host, int port, String filePath, String pubKey, String streamHash, String streamId) throws IOException, InterruptedException {
		StreamClientTLS client = null;
        
        File file = new File(filePath);
        OutputStream out = new FileOutputStream(file);
        CountDownLatch latch = new CountDownLatch(1);
		StreamObserver<StreamFrame> fromStream = new StreamObserver<StreamFrame>() {
			@Override
			public void onCompleted() {
				logger.debug("completed by stream");
			}

			@Override
			public void onError(Throwable t) {
				logger.debug("cancelled by stream");
				t.printStackTrace();
			}

			@Override
			public void onNext(StreamFrame msg) {
				logger.debug("message from stream!!!");
				Map<String, ByteString> respMeta = msg.getMetaMap();
				int stat = Integer.parseInt(respMeta.get(Constants.BAAP_STAT).toStringUtf8());
				if (stat != Constants.BAAP_STAT_SUCCESS) {
					return;
				}
				int index = Integer.parseInt(respMeta.get(Constants.BAAP_STREAM_INDEX).toStringUtf8());
				try {
					if (index > 0) {
						out.write(msg.getBody().toByteArray());
					}
					if (index == -1) {
						latch.countDown();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		client = new StreamClientTLS(host, port, sslContext, fromStream);
		
		Map<String, ByteString> reqMeta = new HashMap<>();
		reqMeta.put(Constants.BAAP_SENDER_PUBLIC_KEY, ByteString.copyFromUtf8(pubKey));
		reqMeta.put(Constants.BAAP_STREAM_HASH, ByteString.copyFromUtf8(streamHash));
		reqMeta.put(Constants.BAAP_STREAM_ID, ByteString.copyFromUtf8(streamId));
		reqMeta.put(Constants.BAAP_STREAM_REQ_TYPE, ByteString.copyFromUtf8(Constants.BAAP_STREAM_DOWNLOAD_REQ));
		reqMeta.put(Constants.BAAP_STREAM_TYPE, ByteString.copyFromUtf8(Constants.BAAP_STREAM_TYPE_DEPLOY));
		StreamFrame frame = StreamFrame.newBuilder().putAllMeta(reqMeta).build();
		client.stream(frame);
		client.complete();
		latch.await();
		out.close();
		client.shutdown();
		return true;
    }
    
    public static void main(String[] args) throws Exception {
//		String host = "stream.pdx.ltd";
		String host="10.0.0.6";
		int port = 8076;
		String filePath = "/opt/" + "testcc.jar";
		String pubKey = "038fce4c13e66d9b28f31dbf15119f5017ccf6d92b0a70678a6571eedb0dbea8b7";
		String streamHash = "aefebb88f84a85f4b885d19a3062330668ae34be06c1d0e1cce2d8b3dd54c748";
		String streamId = "24EBD07E";
		
		ClassLoader classLoader = StreamDownloadUtil.class.getClassLoader();
		InputStream trustCertIn = classLoader.getResourceAsStream("root.crt");
        InputStream clientCertChainIn = classLoader.getResourceAsStream("client.crt");
        InputStream clientPrivateKeyIn = classLoader.getResourceAsStream("client.key");
        String pwd = "pdxTech!";
        SslContext sslContext = StreamClientTLS.buildSslContext(trustCertIn, clientCertChainIn, clientPrivateKeyIn, pwd);
		
		StreamDownloadUtil s = new StreamDownloadUtil();
		boolean ret = s.download(sslContext, host, port, filePath, pubKey, streamHash, streamId);
		System.out.println(ret);
	}
}
