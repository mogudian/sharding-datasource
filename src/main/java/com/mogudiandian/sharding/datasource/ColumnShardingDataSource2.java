package com.mogudiandian.sharding.datasource;

import com.mogudiandian.sharding.datasource.config.ShardingDatasourceConfig2;
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
 * 按字段/列分库分表数据源(新)
 * @author Joshua Sun
 */
public class ColumnShardingDataSource2 extends RoutingDataSource implements DataSource {

    /**
     * 内部数据源 也是真正的数据源
     */
    private final DataSource innerDataSource;

    @SneakyThrows
    public ColumnShardingDataSource2(ShardingDatasourceConfig2 shardingDatasourceConfig) {
        String logicDatabaseName = shardingDatasourceConfig.getShardingLogicDatabaseName();

        // 主数据源 数据源名称 -> 数据源
        Map<String, DataSource> masterDataSourceMap = new HashMap<>();

        // 是否有从库
        boolean hasSlave = Arrays.stream(shardingDatasourceConfig.getDatabases())
                                 .map(MasterSlaveDataSourceComposer::getSlaveDataSources)
                                 .allMatch(ArrayUtils::isNotEmpty);

        // 数据库数量
        int databaseCount = shardingDatasourceConfig.getDatabases().length;

        // 主库
        for (int i = 0; i < databaseCount; i++) {
            String datasourceName = hasSlave ? getMasterDatasourceName(logicDatabaseName, i) : getShardingDatasourceName(logicDatabaseName, i);
            masterDataSourceMap.put(datasourceName, shardingDatasourceConfig.getDatabases()[i].getMasterDataSource());
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

            for (int i = 0; i < databaseCount; i++) {
                DataSource[] slaveDataSources = shardingDatasourceConfig.getDatabases()[i].getSlaveDataSources();
                List<String> slaveDatasourceNames = new ArrayList<>(slaveDataSources.length);
                for (int j = 0, len = slaveDataSources.length; j < len; j++) {
                    String datasourceName = getSlaveDatasourceName(logicDatabaseName, i, j);
                    slaveDataSourceMap.put(datasourceName, slaveDataSources[j]);
                    slaveDatasourceNames.add(datasourceName);
                }
                masterSlaveRuleConfigurations.add(new MasterSlaveRuleConfiguration(getShardingDatasourceName(logicDatabaseName, i), getMasterDatasourceName(logicDatabaseName, i), slaveDatasourceNames));
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