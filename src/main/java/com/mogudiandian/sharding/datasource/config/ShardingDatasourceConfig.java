package com.mogudiandian.sharding.datasource.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 分库分表的JDBC配置
 * @author sunbo
 */
@Getter
@Setter
public class ShardingDatasourceConfig  {

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
     * 所有的库是否在一个实例上
     * 如果为true 则使用masterConfig和slaveConfig url使用通配
     * 否则使用masterSlaveConfig来单独配置
     */
    private boolean databasesOnOneInstance = true;

    /**
     * 数据库数量 所有的库在一个实例上时使用
     */
    private Integer databaseCount;

    /**
     * 每个库中表的数量
     */
    private Integer tableCountPerDatabase;

    /**
     * 主库配置 所有的库在一个实例上时使用
     */
    private JDBCConfig masterConfig;

    /**
     * 从库配置 所有的库在一个实例上时使用
     */
    private JDBCConfig[] slavesConfig;

    /**
     * 主从配置 当库不在同一个实例上时使用
     */
    private MasterSlaveJDBCConfig[] databasesConfig;

}
