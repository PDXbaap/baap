package biz.pdxtech.baap;



import biz.pdxtech.baap.chain.EngineList;
import biz.pdxtech.baap.chaincode.deploy.Deploy;
import biz.pdxtech.baap.chaincode.deploy.DeployInfo;
import biz.pdxtech.baap.command.JnaConstants;
import biz.pdxtech.baap.command.TimingJob;
import biz.pdxtech.baap.filter.ScheduledExecutor;
import biz.pdxtech.baap.node.GrpcServer;

import java.util.concurrent.TimeUnit;


public class Main {



	public static void main(String[] args) {
		try {

			GrpcServer server = GrpcServer.getInstance();
			server.start();

			EngineList.engineRun();
			// 开启一个定时任务
			ScheduledExecutor.start();

			server.listenerEngnieList();

			server.blockUntilShutdown();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
