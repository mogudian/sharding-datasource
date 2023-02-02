package com.mogudiandian.sharding.datasource.test;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务数据源配置
 * @author sunbo
 */
@Getter
@Setter
public class BusinessDatasourceConfig {

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 任务数据源配置
     */
    private TaskDatasourceConfig taskDatasourceConfig;

    /**
     * 任务数据数据源配置
     */
    private TaskContextDatasourceConfig taskDataDatasourceConfig;

    /**
     * 任务日志数据源配置
     */
    private TaskContextDatasourceConfig taskLogDatasourceConfig;

}
