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


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandUtil {
    private static final Logger log = LoggerFactory.getLogger(CommandUtil.class);
    private static final String LINUX = "linux";
    private static final String WINDOWS = "windows";
    private static final String JVM_XMS = "256m";
    private static final String JVM_XMX = "256m";
    private static final String MOUNT_SIZE = "100M";
    // private static final String TEMP = Constants.BAAP_CHAINCODE_DATA_TEMP;
    private static final String TEMP = "/temp";
    private static final String osName = System.getProperty("os.name").toLowerCase();
    //    private static final String runJarCmd = "%s java -Xms%s -Xmx%s -jar -Djava.security.manager -Djava.security.policy=%s %s %s > %s &";
    private static final String runJarCmd = "%s java -Xms%s -Xmx%s -jar %s %s > %s &";
    private static final String mountCmd = "sudo mount -t tmpfs -o size=%s tmpfs %s";
    private static final String umountCmd = "sudo umount %s";


    public static Pair<Boolean, String> runJar(String jar, String[] args) {
        if (osName.startsWith(LINUX)) {
            return runJarForLinux(jar, JVM_XMS, JVM_XMX, args);
        } else if (osName.startsWith(WINDOWS)) {
            return runJarForWin(jar, JVM_XMS, JVM_XMX, args);
        } else {
            return Pair.of(false, "don't support system " + osName);
        }
    }

    public static boolean stopJar(String filePath, String pid) {
        if (osName.startsWith(LINUX)) {
            umountForLinux(filePath);
            return stopJarForLinux(pid);
        } else if (osName.startsWith(WINDOWS)) {
            return stopJarForWin(pid);
        } else {
            log.info("stopJar:don't support system {}" + osName);
            return false;
        }
    }

    public static Pair<Boolean, String> runJarForLinux(String jar, String jvmXms, String jvmXmx, String[] args) {
        try {
            String jarName = jar.substring(jar.lastIndexOf("/") + 1);
            String jarPath = jar.substring(0, jar.lastIndexOf("/"));
            mountForLinux(jarPath);
//        String cmd = String.format(runJarCmd, "nohup", jvmXms, jvmXmx, policyFilePath, jarName, StringUtils.join(args, " "), jarPath + File.separator + TEMP + File.separator + jarName.replace(".jar", ".log"));
            String cmd = String.format(runJarCmd, "nohup", jvmXms, jvmXmx, jarName, StringUtils.join(args, " "), jarPath + File.separator + TEMP + File.separator + jarName.replace(".jar", ".log"));
            log.info(cmd);
            execute(cmd, new File(jar.substring(0, jar.lastIndexOf("/"))));
            Thread.sleep(5000);
            return isAlive(args[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Pair<Boolean, String> runJarForWin(String jar, String jvmXms, String jvmXmx, String[] args) {
        String jarName = jar.substring(jar.lastIndexOf("/") + 1);
        String jarPath = jar.substring(0, jar.lastIndexOf("/"));
        String policyFilePath = createCcPolicy(jarPath);
        String cmd = String.format(runJarCmd, "start /b", jvmXms, jvmXmx, policyFilePath, jarName, StringUtils.join(args, " "), jarName.replace(".jar", ".log"));
        execute(cmd, new File(jar.substring(0, jar.lastIndexOf("/"))));
        return isAlive(jarName);
    }

    public static Pair<Boolean, String> isAlive(String pid) {
        log.info("ps -ef | grep -w " + pid + " | grep -v grep | awk '{print $2}'");
        Pair<Boolean, String> pair = execute("ps -ef | grep -w " + pid + " | grep -v grep | awk '{print $2}'");
        if (StringUtils.isBlank(pair.getRight())) {
            return Pair.of(false, "");
        }
        return pair;
    }

    public static boolean stopJarForLinux(String pid) {
        return execute("kill " + pid).getLeft();
    }

    public static boolean stopJarForWin(String pid) {
        return execute("taskkill /f /im " + pid).getLeft();
    }

    public static boolean mountForLinux(String filePath) {
        filePath = filePath + File.separator + TEMP;
        try {
            FileUtils.forceMkdir(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return execute(String.format(mountCmd, MOUNT_SIZE, filePath)).getLeft();
    }

    public static boolean umountForLinux(String filePath) {
        return execute(String.format(umountCmd, filePath + File.separator + TEMP)).getLeft();
    }

    public static Pair<Boolean, String> execute(String shell) {
        return execute(shell, null);
    }

    public static Pair<Boolean, String> execute(String shell, File file) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", shell);
            if (file != null) pb.directory(file);
            pb.environment();
            pb.redirectErrorStream(true);
            process = pb.start();
            process.waitFor(5, TimeUnit.MINUTES);
            return Pair.of(true, execOut(process.getInputStream()));
        } catch (Exception e) {
            e.printStackTrace();
            return Pair.of(false, e.getMessage());
        } finally {
            if (process != null) process.destroy();
        }
    }

    private static String execOut(InputStream in) {
        try {
            List<String> list = IOUtils.readLines(in, "UTF-8");
            if (list == null) return "";
            StringBuilder lines = new StringBuilder();
            for (String line : list) {
                lines.append(line).append("\n");
            }
            log.info(lines.toString());
            return lines.toString().replace("\n", "");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String createCcPolicy(String jarPath) {
        try {
            InputStream inputStream = CommandUtil.class.getClassLoader().getResourceAsStream("custom.policy");
            String content = IOUtils.toString(inputStream, "UTF-8");
            String newContent = content.replace("${cc.fileDir}", jarPath);
            String policyName = jarPath + File.separator + "custom.policy";
            FileUtils.writeByteArrayToFile(new File(policyName), newContent.getBytes());
            return policyName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        Pair<Boolean, String> pair = execute("ip route|awk '/default/ { print $3 }'", null);
        System.out.println(pair.getLeft());
        System.out.println(pair.getRight());
    }


}
