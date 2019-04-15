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



import biz.pdxtech.baap.conf.BaapProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class JnaConstants {
    /**
     * 工具集合
     */
    public static HashSet<String> deleteHashSet = new HashSet<>();
    /**
     * 启动脚本
     */
    public static String startDockerScript =BaapProperties.getProperty("pdx.baap.startDockerScript");

    public static String startCCrScript = BaapProperties.getProperty("pdx.baap.startCCrScript");

    public static String stopDockerScript = BaapProperties.getProperty("pdx.baap.stopDockerScript");

    public static String randomPortScript= BaapProperties.getProperty("pdx.baap.randomPortScript");

    /**
     *  环境变量
     */
    public static String engerDir = BaapProperties.getProperty("pdx.baap.engerDir");

    public static String engerFile = BaapProperties.getProperty("pdx.baap.engerFile");

    public static String iaasHost = BaapProperties.getProperty("pdx.iaas.host");

    public static String genesisDir = BaapProperties.getProperty("pdx.baap.genesisDir");

    public static String staticNodeDir = BaapProperties.getProperty("pdx.baap.staticNodeDir");

    public static String genesisFile = BaapProperties.getProperty("pdx.baap.genesisFile");

    public static String staticNodeFile = BaapProperties.getProperty("pdx.baap.staticNodeFile");

    public static String dappsDir = BaapProperties.getProperty("pdx.baap.dappsDir");


    /**
     * 合约服务状态
     */
    public static short CCDEPLOY = 1; //已部署

    public static short CCDROP = 3;  //已删除

    public static short CCSTART = 4; //已启动

    public static short CCSTOP = 5;  //已停止
    /**
     * 节点服务状态
     */
    public static short NODESTART = 1; //已启动

    public static short NODESTOP = 2; //已删除
    /**
     * 区块验证起始位置
     */
    public static short minBlock=2;


    public static long repeatTimes = 6;
    /**
     * 启动失败重试间隔
     */
    public static long startRepeat=60000;

    /**
     * 默认内置合约
     */
    public static List<String > defaultCC = new ArrayList<>(Arrays.asList(":baap-base:v1.0", ":baap-chainiaas:v1.0", ":baap-deploy:v1.0", ":baap-payment:v1.0", ":baap-stream:v1.0", ":baap-trusttree:v1.0"));

}
