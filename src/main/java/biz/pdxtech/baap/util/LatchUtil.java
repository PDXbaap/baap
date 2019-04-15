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

import biz.pdxtech.baap.chain.EngineConf;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LatchUtil {

    private static Map<String,CountDownLatch> conditionMap= new HashMap<>();

    public static void wait(String key,long timeout,TimeUnit unit) throws InterruptedException {
        conditionMap.computeIfAbsent(key, k -> new CountDownLatch(1)).await(timeout,unit);
    }

    public static void signal(String key){
        conditionMap.computeIfAbsent(key, k -> new CountDownLatch(1)).countDown();
        conditionMap.remove(key);
    }


    public static boolean checkClient(EngineConf ec){
        try{
            Socket client = new Socket(ec.getHost(), ec.getPort());
            client.close();
            return true;
        }catch(Exception e){
            return false;
        }
    }
    
}
