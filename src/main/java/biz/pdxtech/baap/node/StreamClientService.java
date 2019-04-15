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

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import biz.pdxtech.baap.util.grpc.StreamClientTLS;
import com.google.protobuf.ByteString;

import biz.pdxtech.baap.api.Constants;
import biz.pdxtech.stream.BaapStream.StreamFrame;
import io.netty.handler.ssl.SslContext;

public class StreamClientService {

	private static StreamClientService instance;
	private Map<String, StreamClientTLS> streamClientMap = new ConcurrentHashMap<>();

	public static StreamClientService getInstance() {
		if (instance == null) {
			instance = new StreamClientService();
		}
		return instance;
	}

	private StreamClientService() {
	}

	// cc send stream to streamservice
	public void stream2Ss(StreamFrame stream) throws Exception{
		Map<String, ByteString> meta = stream.getMetaMap();
		String streamId = meta.get(Constants.BAAP_STREAM_ID).toStringUtf8();
		String reqType = meta.get(Constants.BAAP_STREAM_REQ_TYPE).toStringUtf8();
		if (Constants.BAAP_STREAM_DOWNLOAD_REQ.equals(reqType)){
			StreamClientTLS clientTLS = this.streamClientMap.get(streamId);
			if (clientTLS==null){
				clientTLS = connect();
			}
			if (clientTLS!=null){
				putStreamClient(streamId, clientTLS);
				clientTLS.stream(stream);
				return;
			}
			throw new Exception("client connect failed!");
		}
	}

	public void close(String streamId){
		this.streamClientMap.get(streamId).complete();
		this.streamClientMap.remove(streamId);
		ChaincodeService.getInstance().remChaincodeStream(streamId);
	}

	public void putStreamClient(String streamId, StreamClientTLS clientTLS) {
		this.streamClientMap.put(streamId, clientTLS);
	}

	private StreamClientTLS connect() {
		ClassLoader classLoader = StreamClientService.class.getClassLoader();
		try (InputStream trustCertIn = classLoader.getResourceAsStream("root.crt");
			 InputStream clientCertChainIn = classLoader.getResourceAsStream("client.crt");
			 InputStream clientPrivateKeyIn = classLoader.getResourceAsStream("client.key");) {
			String pwd = "pdxTech!";
			StreamClientListener listener = new StreamClientListener();
			SslContext sslContext = StreamClientTLS.buildSslContext(trustCertIn, clientCertChainIn, clientPrivateKeyIn, pwd);
			return new StreamClientTLS("stream.pdx.ltd",8076,sslContext,listener);
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
