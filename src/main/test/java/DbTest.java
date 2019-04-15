import biz.pdxtech.baap.db.service.DeployInfoService;
import biz.pdxtech.baap.util.file.DeployInfo;

import java.util.List;

public class DbTest {
	public static void main(String[] args) {
//		new FlywayManager().flyway(DataSourceManager.getInstance());
		List<DeployInfo> deployInfos = DeployInfoService.queryAll();
		System.out.println(deployInfos.size());
	}
}
