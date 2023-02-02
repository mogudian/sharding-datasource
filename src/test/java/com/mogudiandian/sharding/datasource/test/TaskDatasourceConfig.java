package com.mogudiandian.sharding.datasource.test;

import com.mogudiandian.sharding.datasource.config.JDBCConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 任务数据源配置
 * @author sunbo
 */
@Getter
@Setter
public class TaskDatasourceConfig {

    /**
     * 进行中的任务数据库配置
     */
    private JDBCConfig processingDatabaseConfig;

    /**
     * 已完成的任务数据库配置
     */
    private JDBCConfig finishedDatabaseConfig;

    /**
     * 已归档的任务数据库配置
     */
    private List<ArchivedDatabaseConfig> archivedDatabaseConfigs;

}
