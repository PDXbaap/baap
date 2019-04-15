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

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.function.Function;

public class FileUtil {

    private final static Function<String, String> filePath = chan -> BaapPath.getCcDir(chan) + "%s" + File.separator + "%s" + File.separator + "%s";

    public static boolean saveSlice(String chan, String ccId, String stream, int num, byte[] slice) {
        try {
            File file = new File(String.format(filePath.apply(chan), ccId, stream, stream + "-" + num));
            FileUtils.writeByteArrayToFile(file, slice);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String mergeSlices(String chan, String ccId, String fileId, String fileName) throws Exception {
        String path = BaapPath.getCcDir(chan) + ccId + File.separator + fileId + File.separator;
        File directory = new File(path);
        if (!directory.exists()) {
            throw new Exception("file : " + directory.getPath() + " do not exist!");
        }
        File file = new File(path + fileName);
        int total = directory.listFiles().length;
        for (int i = 1; i <= total; i++) {
            File sliceFile = new File(path + fileId + "-" + i);
            if (!sliceFile.exists()) {
                throw new Exception("stream:" + fileId + "," + i + " slice file do not exist!");
            }
            FileUtils.writeByteArrayToFile(file, FileUtils.readFileToByteArray(sliceFile), true);
            FileUtils.deleteQuietly(sliceFile);
        }
        return path+fileName;
    }

    public static void deleteDirectory(String chan, String ccId) {
        try {
            FileUtils.deleteDirectory(new File(BaapPath.getCcDir(chan) + ccId + File.separator));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成.json格式文件
     */
    public static boolean createJsonFile(String jsonString, String filePath, String fileName) {

        boolean flag = true;

        String fullPath = filePath + File.separator + fileName;

        try {

            File file = new File(fullPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            Writer write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            write.write(jsonString);
            write.flush();
            write.close();
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }



}
