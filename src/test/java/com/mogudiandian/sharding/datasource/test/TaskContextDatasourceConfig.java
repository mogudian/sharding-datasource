package com.mogudiandian.sharding.datasource.test;

import com.mogudiandian.sharding.datasource.config.ShardingDatasourceConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 任务周边数据源配置
 * @author Joshua Sun
 */
@Getter
@Setter
public class TaskContextDatasourceConfig {

    /**
     * 未归档的任务数据库配置
     */
    private ShardingDatasourceConfig databaseConfig;

    /**
     * 已归档的任务数据库配置
     */
    private List<ArchivedDatabaseConfig> archivedDatabaseConfigs;

}
