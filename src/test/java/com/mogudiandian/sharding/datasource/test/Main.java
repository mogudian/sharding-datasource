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
     * ???????????????????????????
     * @param companyId ??????ID
     * @param companyDatasourceConfig ?????????????????????
     * @return ???????????????
     */
    private KeyRoutingDataSource<CompanyDataObjectName> getCompanyDatasource(Long companyId, CompanyDatasourceConfig companyDatasourceConfig) {
        KeyRoutingDataSource<CompanyDataObjectName> companyDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentCompanyDataObject);
        // ????????????????????????
        HikariDataSource globalDatasource = getHikariDataSource(companyDatasourceConfig.getOtherDatabaseConfig());
        companyDatasource.putDatasource(CompanyDataObjectName.GLOBAL, globalDatasource);

        // ????????????????????????????????? businessId -> ?????????????????????
        Map<Long, BusinessDatasourceConfig> businessDatasourceConfigMap = Optional.ofNullable(companyDatasourceConfig.getDatasourceConfigs())
                                                                                  .orElseGet(ArrayList::new)
                                                                                  .stream()
                                                                                  .collect(HashMap::new, (m, e) -> m.put(e.getBusinessId(), e), Map::putAll);
        // ?????????????????????????????????
        KeyRoutingDataSource<Long> multiBusinessDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentBusinessId, globalDatasource);

        // ??????????????????????????????
        List<BusinessDTO> businessDTOS = queryBusinessListByCompany(companyId);
        for (BusinessDTO businessDTO : businessDTOS) {
            // ???????????????????????????????????????
            BusinessDatasourceConfig businessDatasourceConfig = businessDatasourceConfigMap.get(businessDTO.getId());
            if (businessDatasourceConfig != null) {
                // ?????????????????????????????????????????????????????????????????????
                multiBusinessDatasource.putDatasource(businessDTO.getId(), getBusinessDatasource(businessDTO.getDatasourceConfig()));
            } else {
                // ??????????????????????????????????????????????????????????????????
                multiBusinessDatasource.putDatasource(businessDTO.getId(), globalDatasource);
            }
        }
        // ????????????????????????
        companyDatasource.putDatasource(CompanyDataObjectName.BUSINESS_DATA, globalDatasource);

        return companyDatasource;
    }

    /**
     * ???????????????????????????
     * @param businessDatasourceConfig ?????????????????????
     * @return ?????????????????????
     */
    private KeyRoutingDataSource<TaskObjectName> getBusinessDatasource(BusinessDatasourceConfig businessDatasourceConfig) {
        // ?????????????????????
        KeyRoutingDataSource<DataLifecycleType> taskDatasource = getTaskDatasource(businessDatasourceConfig.getTaskDatasourceConfig());
        KeyRoutingDataSource<TaskObjectName> businessDatasource = new KeyRoutingDataSource<>(DatasourceContext.currentTaskDataObject, taskDatasource);
        businessDatasource.putDatasource(TaskObjectName.TASK, taskDatasource);
        businessDatasource.putDatasource(TaskObjectName.TASK_DATA, getTaskContextDatasource(businessDatasourceConfig.getTaskDataDatasourceConfig(), taskDatasource));
        businessDatasource.putDatasource(TaskObjectName.TASK_LOG, getTaskContextDatasource(businessDatasourceConfig.getTaskLogDatasourceConfig(), taskDatasource));
        return businessDatasource;
    }

    /**
     * ???????????????????????????
     * @param databaseConfig ???????????????
     * @return ?????????
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
     * ?????????????????????
     * @param taskDatasourceConfig ?????????????????????
     * @return ???????????????
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
     * ??????????????????????????????
     * @param archivedDatabaseConfigs ???????????????
     * @return ????????????????????????
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
     * ??????????????????????????????
     * @param taskContextDatasourceConfig ???????????????????????????
     * @param taskDatasource ?????????????????????
     * @return ?????????????????????
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
        return "jdbc:mysql://????????????/" + database + "?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
    }

    public static void main(String[] args) {
        String username = "???????????????", password = "????????????";

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
