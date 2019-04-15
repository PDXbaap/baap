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
package biz.pdxtech.baap.conf;

import biz.pdxtech.baap.api.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class BaapProperties {

    private static Logger logger= LoggerFactory.getLogger(BaapProperties.class);
    private static Properties properties = new Properties();
    private static String baapHomeDir = System.getenv(Constants.PDX_BAAP_HOME);

    static {
        try {
            if (true) {
                properties.load(BaapProperties.class.getClassLoader().getResourceAsStream("baap.properties"));
                logger.info("properties load from classpath!");
            } else {
                if (!baapHomeDir.endsWith(File.separator)) {
                    baapHomeDir = baapHomeDir + File.separator;
                }
                File file=new File(baapHomeDir + "conf/baap.properties");
                if (file.exists()){
                    properties.load(new FileInputStream(file));
                    logger.info("properties load from ${PDX_BAAP_HOME}");
                }else {
                    properties.load(BaapProperties.class.getClassLoader().getResourceAsStream("baap.properties"));
                    logger.info("${PDX_BAAP_HOME}/baap.properties does not exists, properties load from classpath!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
