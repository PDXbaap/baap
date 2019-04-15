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
package biz.pdxtech.baap.db.service;

import biz.pdxtech.baap.db.MybatisManager;
import biz.pdxtech.baap.db.mapper.DeployInfoMapper;
import biz.pdxtech.baap.util.file.DeployInfo;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class DeployInfoService {

    public static void save(DeployInfo deployInfo) {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisManager.openSession();
            DeployInfoMapper deployInfoMapper = sqlSession.getMapper(DeployInfoMapper.class);
            deployInfoMapper.insert(deployInfo);
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (sqlSession != null) {
                sqlSession.rollback();
            }
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

	public static List<DeployInfo> queryByChaincodeName(String channel, String chaincodeName) {
		SqlSession sqlSession = null;
		List<DeployInfo> infos = null;
		try {
			sqlSession = MybatisManager.openSession();
			DeployInfoMapper mapper = sqlSession.getMapper(DeployInfoMapper.class);
            infos = mapper.selectByChaincodeName(channel, chaincodeName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sqlSession != null) {
				sqlSession.close();
			}
		}
		return infos;
	}

    public static DeployInfo query(String channel, String chaincodeId, byte status) {
        SqlSession sqlSession = null;
        DeployInfo deployInfo = null;
        try {
            sqlSession = MybatisManager.openSession();
            DeployInfoMapper mapper = sqlSession.getMapper(DeployInfoMapper.class);
            deployInfo = mapper.selectByStatus(channel, chaincodeId, status);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return deployInfo;
    }

    public static void updateTx(DeployInfo info) {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisManager.openSession();
            DeployInfoMapper mapper = sqlSession.getMapper(DeployInfoMapper.class);
            mapper.updateTx(info);
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (sqlSession != null) {
                sqlSession.rollback();
            }
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    public static void updateStream(DeployInfo info) {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisManager.openSession();
            DeployInfoMapper mapper = sqlSession.getMapper(DeployInfoMapper.class);
            mapper.updateStream(info);
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (sqlSession != null) {
                sqlSession.rollback();
            }
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    public static void delete(String channel, String chaincodeId) {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisManager.openSession();
            DeployInfoMapper mapper = sqlSession.getMapper(DeployInfoMapper.class);
            mapper.delete(channel, chaincodeId);
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (sqlSession != null) {
                sqlSession.rollback();
            }
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    public static List<DeployInfo> queryAll() {
        SqlSession sqlSession = null;
        List<DeployInfo> deployInfo = null;
        try {
            sqlSession = MybatisManager.openSession();
            DeployInfoMapper mapper = sqlSession.getMapper(DeployInfoMapper.class);
            deployInfo = mapper.selectAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return deployInfo;
    }
}
