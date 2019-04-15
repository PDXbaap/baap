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
package biz.pdxtech.baap.db.mapper;

import biz.pdxtech.baap.util.file.DeployInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeployInfoMapper {
    @Insert("INSERT INTO deployInfo" +
            " (fileId, fileName, fileHash, channel, chaincodeId, chaincodeName, chaincodeNameHash, chaincodeAddress, pbk, desc, alias, path, status, pId, deployTime)" +
            " values" +
            " (#{info.fileId}, #{info.fileName}, #{info.fileHash} ,#{info.channel}, #{info.chaincodeId}, #{info.chaincodeName}, #{info.chaincodeNameHash}, #{info.chaincodeAddress}, #{info.pbk}, #{info.desc}, #{info.alias}, #{info.path}, #{info.status}, #{info.pId}, #{info.deployTime})")
    void insert(@Param("info") DeployInfo info);

	@Select("select * from deployInfo where channel = #{channel} and chaincodeName = #{chaincodeName}")
	@ResultType(List.class)
	List<DeployInfo> selectByChaincodeName(@Param("channel") String channel, @Param("chaincodeName") String chaincodeName);

    @Select("select * from deployInfo where channel = #{channel} and chaincodeId = #{chaincodeId} and status = #{status} limit 1")
    @ResultType(DeployInfo.class)
    DeployInfo selectByStatus(@Param("channel") String channel, @Param("chaincodeId") String chaincodeId, @Param("status") byte status);

    @Update("update deployInfo set pbk = #{info.pbk} , desc = #{info.desc} , alias = #{info.alias} , resourceIp = #{info.resourceIp} , status = #{info.status} where channel=#{info.channel} and chaincodeId = #{info.chaincodeId}")
    void updateTx(@Param("info") DeployInfo info);

    @Update("update deployInfo set path = #{info.path} , status=#{info.status} , fileName=#{info.fileName} , fileHash=#{info.fileHash}  where channel=#{info.channel} and chaincodeId = #{info.chaincodeId}")
    void updateStream(@Param("info") DeployInfo info);

    @Delete("delete * from deployInfo where channel=#{channel} and chaincodeId = #{chaincodeId}")
    void delete(@Param("channel") String channel, @Param("chaincodeId") String chaincodeId);

    @Select("select * from deployInfo")
    @ResultType(List.class)
    List<DeployInfo> selectAll();

}
