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

import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.command.JnaConstants;
import biz.pdxtech.baap.util.BaapPath;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;


public class GrpcServer {

    private static final Logger logger = LoggerFactory.getLogger(GrpcServer.class);

    private static final String GRPC_PORT = "9052";

    public static int port;

    public static String certChain;

    public static String privateK;

    private final Server server;

    private static GrpcServer instance = null;

    public static GrpcServer getInstance() {

        String port = System.getenv("BAAP_GRPC_PORT");

        if (port == null)
            port = System.getProperty("BAAP_GRPC_PORT", GRPC_PORT);

        if (instance == null)
            instance = new GrpcServer(Integer.parseInt(port));

        return instance;
    }

    private GrpcServer(int port) {

    	GrpcServer.port = port;

        ServerBuilder<?> builder = ServerBuilder.forPort(port);

        certChain = System.getenv("BAAP_GRPC_CERT");

        privateK = System.getenv("BAAP_GRPC_PKEY");

        if (certChain != null && privateK != null)
            builder = builder.useTransportSecurity(new File(certChain), new File(privateK));

        server = builder.addService(ChaincodeService.getInstance()).build();
    }

    public void start() throws IOException {

        server.start();

        logger.info("Server started, listening on {}" , port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            GrpcServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    // 监听engnie变化
    public void listenerEngnieList() throws Exception {

        WatchService watchService = FileSystems.getDefault().newWatchService();

//        Path path = Paths.get(BaapPath.getBaseDir() + "conf" + File.separator );
          Path path = Paths.get(JnaConstants.engerDir);

        // 注册更新事件
        path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY);

        WatchKey key;
        while ((key = watchService.take()) != null) {
            // 防止事件触发时,变更数据没有保存完,读区脏数据
            Thread.sleep(1*1000);
            for (WatchEvent<?> event : key.pollEvents()) {
                System.out.println( "Event kind:" + event.kind());
                // 更新engnieLIst
                EngineList.updateInstance();
            }
            key.reset();
        }
    }

}