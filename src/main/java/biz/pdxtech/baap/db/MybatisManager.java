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

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

public class MybatisManager {
	private static class MybatisManagerHolder {
		private static final SqlSessionFactory sqlSessionFactory = init();
		
		private static SqlSessionFactory init() {
			TransactionFactory transactionFactory = new JdbcTransactionFactory();
			Environment environment = new Environment("daap", transactionFactory, DataSourceManager.getInstance());
			Configuration configuration = new Configuration(environment);
			configuration.addMappers("biz.pdxtech.baap.db");
			
			return new SqlSessionFactoryBuilder().build(configuration);
		}
	}
	
	private MybatisManager() {
	
	}
	
	public static SqlSessionFactory sqlSessionFactory() {
		return MybatisManagerHolder.sqlSessionFactory;
	}
	
	public static SqlSession openSession() {
		return MybatisManagerHolder.sqlSessionFactory.openSession();
	}
}
