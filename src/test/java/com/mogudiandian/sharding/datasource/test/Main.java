package com.mogudiandian.sharding.datasource.test;

import com.alibaba.fastjson.JSON;
import com.mogudiandian.sharding.datasource.*;
import com.mogudiandian.sharding.datasource.config.JDBCConfig;
import com.mogudiandian.sharding.datasource.config.ShardingDatasourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.*;

public class Main {

    @Bean
    public DataSource censorshipDatasource() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        NamingDataSourcePool namingDataSourcePool = new NamingDataSourcePool(threadLocal);
        namingDataSourcePool.add("censorship", getHikariDataSource(null));
        namingDataSourcePool.add("company:test", getHikariDataSource(null));
        namingDataSourcePool.add("company:test:user:task:hot", getBusinessDatasource(null));
        namingDataSourcePool.add("company:test:user:task:cold", getBusinessDatasource(null));
        namingDataSourcePool.add("company:test:user:task:archived", getYearRoutingDatasource(null));
        namingDataSourcePool.add("company:test:user:task-data:hot", getTaskContextDatasource(null, null));
        namingDataSourcePool.add("company:test:user:task-data:archived", getYearRoutingDatasource(null));
        namingDataSourcePool.add("company:test:user:task-log:hot", getTaskContextDatasource(null, null));
        namingDataSourcePool.add("company:test:user:task-log:archived", getYearRoutingDatasource(null));
        return namingDataSourcePool;
    }

    @Bean
    public DataSource bastionDatasource() {
        KeyRoutingDataSource<Long> multiBusinessDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentCompanyId);
        List<CompanyDTO> companyDTOS = queryAllCompanyList();
        for (CompanyDTO companyDTO : companyDTOS) {
            multiBusinessDatasource.putDatasource(companyDTO.getId(), getCompanyDatasource(companyDTO.getId(), companyDTO.getDatasourceConfig()));
        }
        return multiBusinessDatasource;
    }

    private List<CompanyDTO> queryAllCompanyList() {
        return new ArrayList<>();
    }

    private List<BusinessDTO> queryBusinessListByCompany(Long companyId) {
        return new ArrayList<>();
    }

    /**
     * 获取单企业的数据源
     * @param companyId 企业ID
     * @param companyDatasourceConfig 企业数据源配置
     * @return 企业数据源
     */
    private KeyRoutingDataSource<CompanyDataObjectName> getCompanyDatasource(Long companyId, CompanyDatasourceConfig companyDatasourceConfig) {
        KeyRoutingDataSource<CompanyDataObjectName> companyDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentCompanyDataObject);
        // 企业全局的数据源
        HikariDataSource globalDatasource = getHikariDataSource(companyDatasourceConfig.getOtherDatabaseConfig());
        companyDatasource.putDatasource(CompanyDataObjectName.GLOBAL, globalDatasource);

        // 每一个业务的数据源配置 businessId -> 业务数据源配置
        Map<Long, BusinessDatasourceConfig> businessDatasourceConfigMap = Optional.ofNullable(companyDatasourceConfig.getDatasourceConfigs())
                                                                                  .orElseGet(ArrayList::new)
                                                                                  .stream()
                                                                                  .collect(HashMap::new, (m, e) -> m.put(e.getBusinessId(), e), Map::putAll);
        // 企业数据源的业务数据源
        KeyRoutingDataSource<Long> multiBusinessDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentBusinessId, globalDatasource);

        // 遍历企业下的所有业务
        List<BusinessDTO> businessDTOS = queryBusinessListByCompany(companyId);
        for (BusinessDTO businessDTO : businessDTOS) {
            // 获取该业务的独立数据源配置
            BusinessDatasourceConfig businessDatasourceConfig = businessDatasourceConfigMap.get(businessDTO.getId());
            if (businessDatasourceConfig != null) {
                // 如果业务存在独立的数据源，则用业务的数据源配置
                multiBusinessDatasource.putDatasource(businessDTO.getId(), getBusinessDatasource(businessDTO.getDatasourceConfig()));
            } else {
                // 如果业务不存在独立的数据源，则使用整个企业的
                multiBusinessDatasource.putDatasource(businessDTO.getId(), globalDatasource);
            }
        }
        // 业务独立的数据源
        companyDatasource.putDatasource(CompanyDataObjectName.BUSINESS_DATA, globalDatasource);

        return companyDatasource;
    }

    /**
     * 获取单业务的数据源
     * @param businessDatasourceConfig 业务数据源配置
     * @return 单业务的数据源
     */
    private KeyRoutingDataSource<TaskObjectName> getBusinessDatasource(BusinessDatasourceConfig businessDatasourceConfig) {
        // 任务表的数据源
        KeyRoutingDataSource<DataLifecycleType> taskDatasource = getTaskDatasource(businessDatasourceConfig.getTaskDatasourceConfig());
        KeyRoutingDataSource<TaskObjectName> businessDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentTaskDataObject, taskDatasource);
        businessDatasource.putDatasource(TaskObjectName.TASK, taskDatasource);
        businessDatasource.putDatasource(TaskObjectName.TASK_DATA, getTaskContextDatasource(businessDatasourceConfig.getTaskDataDatasourceConfig(), taskDatasource));
        businessDatasource.putDatasource(TaskObjectName.TASK_LOG, getTaskContextDatasource(businessDatasourceConfig.getTaskLogDatasourceConfig(), taskDatasource));
        return businessDatasource;
    }

    /**
     * 根据配置获取数据源
     * @param databaseConfig 数据源配置
     * @return 数据源
     */
    private HikariDataSource getHikariDataSource(JDBCConfig databaseConfig) {
        if (databaseConfig == null) {
            return null;
        }
        HikariConfig processingDatasourceConfig = new HikariConfig(databaseConfig.getProperties());
        processingDatasourceConfig.setJdbcUrl(databaseConfig.getJdbcUrl());
        processingDatasourceConfig.setUsername(databaseConfig.getUsername());
        processingDatasourceConfig.setPassword(databaseConfig.getPassword());
        return new HikariDataSource(processingDatasourceConfig);
    }

    /**
     * 获取任务数据源
     * @param taskDatasourceConfig 任务数据源配置
     * @return 任务数据源
     */
    private KeyRoutingDataSource<DataLifecycleType> getTaskDatasource(TaskDatasourceConfig taskDatasourceConfig) {
        HikariDataSource processingDatasource = getHikariDataSource(taskDatasourceConfig.getProcessingDatabaseConfig());
        KeyRoutingDataSource<DataLifecycleType> taskDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentDataLifecycle, processingDatasource);
        taskDatasource.putDatasource(DataLifecycleType.PROCESSING, processingDatasource);
        taskDatasource.putDatasource(DataLifecycleType.FINISHED, getHikariDataSource(taskDatasourceConfig.getFinishedDatabaseConfig()));
        taskDatasource.putDatasource(DataLifecycleType.ARCHIVED, getYearRoutingDatasource(taskDatasourceConfig.getArchivedDatabaseConfigs()));
        return taskDatasource;
    }

    /**
     * 获取按年分片的数据源
     * @param archivedDatabaseConfigs 数据源配置
     * @return 按年分片的数据源
     */
    private YearRoutingDataSource getYearRoutingDatasource(List<ArchivedDatabaseConfig> archivedDatabaseConfigs) {
        if (archivedDatabaseConfigs == null || archivedDatabaseConfigs.isEmpty()) {
            return null;
        }
        YearRoutingDataSource archiveRoutingDatasource = new YearRoutingDataSource(DatasourceContext.currentDataYear);
        for (ArchivedDatabaseConfig archivedDatabaseConfig : archivedDatabaseConfigs) {
            YearRange yearRange = new YearRange(archivedDatabaseConfig.getStartYear(), archivedDatabaseConfig.getEndYear());
            archiveRoutingDatasource.putDatasource(yearRange, getHikariDataSource(archivedDatabaseConfig.getDatabaseConfig()));
        }
        return archiveRoutingDatasource;
    }

    /**
     * 获取任务周边的数据源
     * @param taskContextDatasourceConfig 任务周边数据源配置
     * @param taskDatasource 任务数据源配置
     * @return 任务周边数据源
     */
    private KeyRoutingDataSource<DataLifecycleType> getTaskContextDatasource(TaskContextDatasourceConfig taskContextDatasourceConfig, DataSource taskDatasource) {
        if (taskContextDatasourceConfig == null) {
            return null;
        }
        KeyRoutingDataSource<DataLifecycleType> taskContextDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentDataLifecycle, taskDatasource);
        taskContextDatasource.putDatasource(DataLifecycleType.UNARCHIVED, new ColumnShardingDataSource(taskContextDatasourceConfig.getDatabaseConfig()));
        taskContextDatasource.putDatasource(DataLifecycleType.ARCHIVED, getYearRoutingDatasource(taskContextDatasourceConfig.getArchivedDatabaseConfigs()));
        return taskContextDatasource;
    }

    private static String jdbcUrl(String database) {
        return "jdbc:mysql://换成地址/" + database + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
    }

    public static void main(String[] args) {
        String username = "换成用户名", password = "换成密码";

        CompanyDatasourceConfig companyDatasourceConfig = new CompanyDatasourceConfig();
        JDBCConfig companyGlobalDatabaseConfig = new JDBCConfig();
        companyGlobalDatabaseConfig.setJdbcUrl(jdbcUrl("company_test"));
        companyGlobalDatabaseConfig.setUsername(username);
        companyGlobalDatabaseConfig.setPassword(password);
        companyDatasourceConfig.setOtherDatabaseConfig(companyGlobalDatabaseConfig);
        System.out.println(JSON.toJSONString(companyDatasourceConfig, true));

        BusinessDatasourceConfig businessDatasourceConfig = new BusinessDatasourceConfig();
        TaskDatasourceConfig taskDatasourceConfig = new TaskDatasourceConfig();

        JDBCConfig processingDatabaseConfig = new JDBCConfig();
        processingDatabaseConfig.setJdbcUrl(jdbcUrl("company_test_audit_task_hot"));
        processingDatabaseConfig.setUsername(username);
        processingDatabaseConfig.setPassword(password);
        taskDatasourceConfig.setProcessingDatabaseConfig(processingDatabaseConfig);

        businessDatasourceConfig.setTaskDatasourceConfig(taskDatasourceConfig);

        TaskContextDatasourceConfig taskDataDatasourceConfig = new TaskContextDatasourceConfig();
        ShardingDatasourceConfig taskDataShardingDatasourceConfig = new ShardingDatasourceConfig();
        taskDataShardingDatasourceConfig.setShardingLogicDatabaseName("audit_task_data");
        taskDataShardingDatasourceConfig.setShardingLogicTableName("audit_task_data");
        taskDataShardingDatasourceConfig.setShardingColumnName("object_id");
        taskDataShardingDatasourceConfig.setDatabaseCount(2);
        taskDataShardingDatasourceConfig.setTableCountPerDatabase(1);
        JDBCConfig masterConfig = new JDBCConfig();
        masterConfig.setJdbcUrl(jdbcUrl("company_test_user_audit_task_data_%d"));
        masterConfig.setUsername(username);
        masterConfig.setPassword(password);
        taskDataShardingDatasourceConfig.setMasterConfig(masterConfig);
        taskDataDatasourceConfig.setDatabaseConfig(taskDataShardingDatasourceConfig);
        businessDatasourceConfig.setTaskDataDatasourceConfig(taskDataDatasourceConfig);

        TaskContextDatasourceConfig taskLogDatasourceConfig = new TaskContextDatasourceConfig();
        ShardingDatasourceConfig taskLogShardingDatasourceConfig = new ShardingDatasourceConfig();
        taskLogShardingDatasourceConfig.setShardingLogicDatabaseName("audit_task_log");
        taskLogShardingDatasourceConfig.setShardingLogicTableName("audit_task_log");
        taskLogShardingDatasourceConfig.setShardingColumnName("object_id");
        taskLogShardingDatasourceConfig.setDatabaseCount(1);
        taskLogShardingDatasourceConfig.setTableCountPerDatabase(2);
        JDBCConfig taskLogMasterConfig = new JDBCConfig();
        taskLogMasterConfig.setJdbcUrl(jdbcUrl("company_test_user_audit_task_log_%d"));
        taskLogMasterConfig.setUsername(username);
        taskLogMasterConfig.setPassword(password);
        taskLogShardingDatasourceConfig.setMasterConfig(taskLogMasterConfig);
        taskLogDatasourceConfig.setDatabaseConfig(taskLogShardingDatasourceConfig);
        businessDatasourceConfig.setTaskLogDatasourceConfig(taskLogDatasourceConfig);

        System.out.println(JSON.toJSONString(businessDatasourceConfig, true));
    }

}
