package com.mogudiandian.sharding.datasource.config;

import com.mogudiandian.sharding.datasource.MasterSlaveDataSourceComposer;
import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;

/**
 * 分库分表的JDBC配置(新)
 * @author sunbo
 */
@Getter
@Setter
public class ShardingDatasourceConfig2 {

    /**
     * 逻辑库名
     */
    private String shardingLogicDatabaseName;

    /**
     * 逻辑表名
     */
    private String shardingLogicTableName;

    /**
     * 分库分表的字段/列名
     */
    private String shardingColumnName;

    /**
     * 每个库中表的数量
     */
    private Integer tableCountPerDatabase;

    /**
     * 主从配置 当库不在同一个实例上时使用
     */
    private MasterSlaveDataSourceComposer[] databases;

}
