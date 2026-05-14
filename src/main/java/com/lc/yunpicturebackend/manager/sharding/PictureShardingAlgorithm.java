package com.lc.yunpicturebackend.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     * 团队空间，私有空间，公共图库
     * @param availableTargetNames 当前逻辑表，配置中声明的「所有可用的物理节点集合」（数据源 + 表名）
     * @param preciseShardingValue 封装了分片建的精确值
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        // 编写分表逻辑，返回实际要查询的表名
        // picture_0 物理表，picture 逻辑表
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        //查询公共图库
        if(spaceId==null){
            return logicTableName;
        }
        String actualTableName =  "picture_" + spaceId;
        if(availableTargetNames.contains(actualTableName)){
            return actualTableName;
        }else{
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }



    @Override
    public void init(Properties properties) {

    }
}