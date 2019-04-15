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
package biz.pdxtech.baap.filter;

import biz.pdxtech.baap.chain.EngineConf;
import biz.pdxtech.baap.chain.EngineList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static biz.pdxtech.baap.chain.EngineList.tryConnect;

public class ScheduledExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledExecutor.class);

    public static Runnable chaincodeExecutor = () -> {
        logger.info("Timed task run is start ！！");

        LinkedList<EngineConf> engineList = EngineList.getEngine();
        engineList.forEach(ec -> {
            if (!ec.isConnect()) {
                tryConnect(ec);
            }
        });

        logger.info("Timed task run is end ！！");
    };

    public static void start() {
        logger.info("engnieList timed task thread is start ！！");
        // 开启一个定时任务
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        // 从现在开始30秒钟之后，每隔30秒钟执行一次job1
        service.scheduleWithFixedDelay(
                chaincodeExecutor, 5,
                5
                , TimeUnit.SECONDS);
    }

}
