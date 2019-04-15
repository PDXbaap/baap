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
package biz.pdxtech.baap.db;

import biz.pdxtech.baap.util.BaapPath;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceManager {
	private static class Holder {
		private static final DataSource dataSource = init();
		
		private static DataSource init() {
			HikariDataSource ds = new HikariDataSource();
			
			ds.setDriverClassName("org.sqlite.JDBC");
			ds.setJdbcUrl("jdbc:sqlite:" + BaapPath.getBaseDir() + ".baap.db");
			return ds;
			
		}
	}
	
	private DataSourceManager() {
	}
	
	public static DataSource
	getInstance() {
		return Holder.dataSource;
	}
}
