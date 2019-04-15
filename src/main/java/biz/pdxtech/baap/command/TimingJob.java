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
package biz.pdxtech.baap.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 为各种定时任务执行统一提供服务 <br/>
 * <p>
 * 不提供持久化功能，只是一个单纯的任务计时执行工具。
 *
 * @author wuyj
 */
public class TimingJob extends Thread {

    public static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);

    public static final Logger log = LoggerFactory.getLogger("timeingJobLog");


    /**
     * 创建并执行一个在给定初始延迟后首次启用的定时任务，具有周期执行的功能。 <br/>
     * 任务在 initialDelay 后开始执行，接着在 initialDelay + period 后执行，依此类推。 <br/>
     * 如果任何一个任务花费的时间比周期还长，则将推迟后续任务执行，不会同时执行。 <br/>
     *
     * @param task
     * @param initialDelay 首次执行距当前时间的时间差
     * @param period       任务执行周期
     * @param unit         initialDelay和period的单位
     */
    public static void addTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (task == null) {
            log.warn("传入了空的周期任务");
            return;
        }
        pool.scheduleAtFixedRate(makeTask(task), initialDelay, period, unit);
        log.info("新增任务:" + task.getClass().getName() + ", 执行时间:" + new Date(System.currentTimeMillis() + initialDelay) + ", 周期:" + period + unit);
    }

    /**
     * 添加仅执行一次的任务，在给定延时后执行
     *
     * @param task
     * @param initialDelay 首次执行距当前时间的时间差（毫秒）
     */
    public static void addOneTimeTask(Runnable task, long initialDelay) {
        if (task == null) {
            log.warn("传入了空的一次性任务");
            return;
        }
        pool.schedule(makeTask(task), initialDelay, TimeUnit.MILLISECONDS);
        log.info("新增一次性任务:" + task.getClass().getName() + ", initialDelay=" + initialDelay);
    }

    /**
     * 立即执行任务
     *
     * @param task
     */
    public static void execute(Runnable task) {
        pool.schedule(makeTask(task), 0, TimeUnit.MILLISECONDS);
    }

    /**
     * 在给定的任务前后统一增加一些必要的逻辑<br/>
     * 现在任务前清除CacheFacade ThreadLocal中的数据
     *
     * @param task
     * @return
     */
    private static Runnable makeTask(final Runnable task) {
        Runnable newTask = new Runnable() {
            @Override
            public void run() {
                task.run();
            }
        };
        return newTask;
    }

    /**
     * 将给定的calendar的时间设置为time给定的时间
     *
     * @param time 每日执行时间点，格式 hh:mm:ss
     * @return
     */
    public static Calendar toCalendar(Calendar calendar, String time) {
        String[] timeTmp = time.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeTmp[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeTmp[1]));
        calendar.set(Calendar.SECOND, Integer.parseInt(timeTmp[2]));
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

}