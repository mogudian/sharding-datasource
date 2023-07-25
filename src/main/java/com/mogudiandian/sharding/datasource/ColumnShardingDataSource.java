package com.mogudiandian.sharding.datasource;

import com.mogudiandian.sharding.datasource.config.JDBCConfig;
import com.mogudiandian.sharding.datasource.config.MasterSlaveJDBCConfig;
import com.mogudiandian.sharding.datasource.config.ShardingDatasourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shardingsphere.api.config.masterslave.MasterSlaveRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 按字段/列分库分表数据源
 * @author Joshua Sun
 */
public class ColumnShardingDataSource extends RoutingDataSource implements DataSource {

    /**
     * 内部数据源 也是真正的数据源
     */
    private final DataSource innerDataSource;

    @SneakyThrows
    public ColumnShardingDataSource(ShardingDatasourceConfig shardingDatasourceConfig) {
        String logicDatabaseName = shardingDatasourceConfig.getShardingLogicDatabaseName();

        // 主数据源 数据源名称 -> 数据源
        Map<String, DataSource> masterDataSourceMap = new HashMap<>();

        // 是否有从库
        boolean hasSlave;

        // 数据库数量
        int databaseCount;

        // 所有数据库在一个实例中
        if (shardingDatasourceConfig.isDatabasesOnOneInstance()) {
            hasSlave = ArrayUtils.isNotEmpty(shardingDatasourceConfig.getSlavesConfig());
            databaseCount = shardingDatasourceConfig.getDatabaseCount();

            // 主库
            for (int i = 0; i < databaseCount; i++) {
                // 数据源名 如果有主从就用主库名（从库配置会设置数据源规则名） 如果没有就用通用数据源规则名
                String datasourceName = hasSlave ? getMasterDatasourceName(logicDatabaseName, i) : getShardingDatasourceName(logicDatabaseName, i);
                masterDataSourceMap.put(datasourceName, getDataSource(shardingDatasourceConfig.getMasterConfig(), i, false));
            }
        } else {
            hasSlave = Arrays.stream(shardingDatasourceConfig.getDatabasesConfig())
                             .map(MasterSlaveJDBCConfig::getSlavesConfig)
                             .allMatch(ArrayUtils::isNotEmpty);
            databaseCount = shardingDatasourceConfig.getDatabasesConfig().length;

            // 主库
            for (int i = 0; i < databaseCount; i++) {
                String datasourceName = hasSlave ? getMasterDatasourceName(logicDatabaseName, i) : getShardingDatasourceName(logicDatabaseName, i);
                masterDataSourceMap.put(datasourceName, getDataSource(shardingDatasourceConfig.getDatabasesConfig()[i].getMasterConfig(), false));
            }
        }

        // 所有数据源 数据源名称 -> 数据源
        Map<String, DataSource> dataSourceMap = new HashMap<>(masterDataSourceMap);

        // 分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        // 有从库的情况
        if (hasSlave) {
            // 从库名 -> 数据源
            Map<String, DataSource> slaveDataSourceMap = new HashMap<>();
            // 主从读写分离规则
            List<MasterSlaveRuleConfiguration> masterSlaveRuleConfigurations = new ArrayList<>();

            // 所有数据库在一个实例中
            if (shardingDatasourceConfig.isDatabasesOnOneInstance()) {
                for (int i = 0; i < databaseCount; i++) {
                    JDBCConfig[] slaveConfigs = shardingDatasourceConfig.getSlavesConfig();
                    List<String> slaveDatasourceNames = new ArrayList<>(slaveConfigs.length);
                    for (int j = 0, len = slaveConfigs.length; j < len; j++) {
                        String datasourceName = getSlaveDatasourceName(logicDatabaseName, i, j);
                        slaveDataSourceMap.put(datasourceName, getDataSource(slaveConfigs[j], i, true));
                        slaveDatasourceNames.add(datasourceName);
                    }
                    masterSlaveRuleConfigurations.add(new MasterSlaveRuleConfiguration(getShardingDatasourceName(logicDatabaseName, i), getMasterDatasourceName(logicDatabaseName, i), slaveDatasourceNames));
                }
            } else {
                for (int i = 0; i < databaseCount; i++) {
                    JDBCConfig[] slaveConfigs = shardingDatasourceConfig.getDatabasesConfig()[i].getSlavesConfig();
                    List<String> slaveDatasourceNames = new ArrayList<>(slaveConfigs.length);
                    for (int j = 0, len = slaveConfigs.length; j < len; j++) {
                        String datasourceName = getSlaveDatasourceName(logicDatabaseName, i, j);
                        slaveDataSourceMap.put(datasourceName, getDataSource(slaveConfigs[j], true));
                        slaveDatasourceNames.add(datasourceName);
                    }
                    masterSlaveRuleConfigurations.add(new MasterSlaveRuleConfiguration(getShardingDatasourceName(logicDatabaseName, i), getMasterDatasourceName(logicDatabaseName, i), slaveDatasourceNames));
                }
            }

            // 加入从库映射
            dataSourceMap.putAll(slaveDataSourceMap);

            // 设置主从规则
            shardingRuleConfig.setMasterSlaveRuleConfigs(masterSlaveRuleConfigurations);
        }

        // 每个库中表的数量
        int tableCountPerDatabase = shardingDatasourceConfig.getTableCountPerDatabase();

        String logicTableName = shardingDatasourceConfig.getShardingLogicTableName();
        String shardingColumnName = shardingDatasourceConfig.getShardingColumnName();

        // 分库策略
        StandardShardingStrategyConfiguration databaseShardingStrategyConfig = new StandardShardingStrategyConfiguration(shardingColumnName, new PreciseShardingAlgorithm<String>() {
            @Override
            public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
                return getShardingDatasourceName(logicDatabaseName, (int) ((hash(preciseShardingValue.getValue()) % (databaseCount * tableCountPerDatabase)) / tableCountPerDatabase));
            }
        });

        // 分表策略
        StandardShardingStrategyConfiguration tableShardingStrategyConfig = new StandardShardingStrategyConfiguration(shardingColumnName, new PreciseShardingAlgorithm<String>() {
            @Override
            public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
                return logicTableName + "_" + ((hash(preciseShardingValue.getValue()) % (databaseCount * tableCountPerDatabase)) % tableCountPerDatabase);
            }
        });

        // 分库分表表达式
        String actualDataNodes = String.format("ds_%s_$->{0..%d}.%s_$->{0..%d}", logicDatabaseName, databaseCount - 1, logicTableName, tableCountPerDatabase - 1);
        TableRuleConfiguration tableRuleConfiguration = new TableRuleConfiguration(logicTableName, actualDataNodes);
        tableRuleConfiguration.setDatabaseShardingStrategyConfig(databaseShardingStrategyConfig);
        tableRuleConfiguration.setTableShardingStrategyConfig(tableShardingStrategyConfig);
        shardingRuleConfig.setTableRuleConfigs(Stream.of(tableRuleConfiguration).collect(Collectors.toList()));

        // 初始化内部数据源
        innerDataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfig, null);
    }

    @Override
    public DataSource route() {
        return innerDataSource;
    }

    /**
     * 获取主库数据源名称
     * @param logicDatabaseName 逻辑库名
     * @param index 数据库索引
     * @return 数据源名称
     */
    private String getMasterDatasourceName(String logicDatabaseName, int index) {
        return logicDatabaseName + "-master-" + index;
    }

    /**
     * 获取从库数据源名称
     * @param logicDatabaseName 逻辑库名
     * @param masterIndex 主库的数据库索引
     * @param slaveIndex 从库的数据库索引
     * @return 数据源名称
     */
    private String getSlaveDatasourceName(String logicDatabaseName, int masterIndex, int slaveIndex) {
        return logicDatabaseName + "-slave-" + masterIndex + "-" + slaveIndex;
    }

    /**
     * 获取分库分表数据源名称
     * @param logicDatabaseName 逻辑库名
     * @param index 数据库索引
     * @return 数据源名称
     */
    private String getShardingDatasourceName(String logicDatabaseName, int index) {
        return "ds_" + logicDatabaseName + "_" + index;
    }

    /**
     * 根据配置获取分库分表数据源
     * @param shardingDatabaseConfig 数据源配置
     * @param readOnly 是否只读
     * @return 数据源
     */
    protected DataSource getDataSource(JDBCConfig shardingDatabaseConfig, boolean readOnly) {
        HikariConfig hikariConfig = new HikariConfig(shardingDatabaseConfig.getProperties());
        hikariConfig.setJdbcUrl(shardingDatabaseConfig.getJdbcUrl());
        hikariConfig.setUsername(shardingDatabaseConfig.getUsername());
        hikariConfig.setPassword(shardingDatabaseConfig.getPassword());
        if (readOnly) {
            hikariConfig.setReadOnly(true);
        }
        return new HikariDataSource(hikariConfig);
    }

    /**
     * 根据配置获取分库分表数据源
     * @param shardingDatabaseConfig 数据源配置
     * @param databaseIndex 第几个库
     * @param readOnly 是否只读
     * @return 数据源
     */
    private DataSource getDataSource(JDBCConfig shardingDatabaseConfig, int databaseIndex, boolean readOnly) {
        JDBCConfig jdbcConfig = new JDBCConfig();
        jdbcConfig.setJdbcUrl(shardingDatabaseConfig.getJdbcUrl().replace("{index}", Integer.toString(databaseIndex)));
        jdbcConfig.setUsername(shardingDatabaseConfig.getUsername());
        jdbcConfig.setPassword(shardingDatabaseConfig.getPassword());
        jdbcConfig.setProperties(shardingDatabaseConfig.getProperties());
        return getDataSource(jdbcConfig, readOnly);
    }

    /**
     * string的hash
     * 因为默认的hashCode会返回负数 这里不希望是负数 同时又考虑到hashCode可能为Integer.MAX_VALUE的情况不能取绝对值
     * @param str 字符串
     * @return hashCode
     */
    private long hash(String str) {
        return Math.abs(Long.valueOf(str.hashCode()));
    }

    @Override
    public void close() {
        DataSourceUtils.close(innerDataSource);
    }
}