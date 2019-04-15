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


import java.io.InputStreamReader;
import java.io.LineNumberReader;


public class RuntiomeUtil {
    public   static String execCmd(String cmd){
        try {
            String[] cmdA = { "/bin/sh", "-c", cmd };
            Process process = Runtime.getRuntime().exec(cmdA);
            LineNumberReader br = new LineNumberReader(new InputStreamReader(process.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
//                System.out.println(line);
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public   static String[] execCmds(String[] cmd){
        try {
            String [] result=new String [cmd.length];
            for(int i=0;i<cmd.length;i++) {
                String[] cmdA = {"/bin/sh", "-c", cmd[i]};
                Process process = Runtime.getRuntime().exec(cmdA);
                LineNumberReader br = new LineNumberReader(new InputStreamReader(process.getInputStream()));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = br.readLine()) != null) {
//                System.out.println(line);
                    sb.append(line).append("\n");
                }
                result[i]=sb.toString();
            }
            return  result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}