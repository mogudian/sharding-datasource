package com.mogudiandian.sharding.datasource;

import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;

/**
 * 主从数据源的组合
 * @author sunbo
 */
@Getter
@Setter
public class MasterSlaveDataSourceComposer {

    /**
     * 主库配置
     */
    private DataSource masterDataSource;

    /**
     * 从库配置
     */
    private DataSource[] slaveDataSources;

}
