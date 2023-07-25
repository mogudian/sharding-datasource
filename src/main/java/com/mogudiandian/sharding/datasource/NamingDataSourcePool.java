package com.mogudiandian.sharding.datasource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 有名称的数据源
 * 和KeyRouting的区别在于这里的map是全局的，这个实例只需要创建一个，不断进行add/replace即可
 * @author Joshua Sun
 */
public final class NamingDataSourcePool extends RoutingDataSource {

    /**
     * 数据源名 -> 数据源
     */
    private static final Map<String, DataSource> DATASOURCE_MAP = new ConcurrentHashMap<>();

    /**
     * 切换数据源的标记
     */
    private final ThreadLocal<String> datasourceName;

    public NamingDataSourcePool(ThreadLocal<String> datasourceName) {
        this.datasourceName = datasourceName;
    }

    /**
     * 添加数据源
     * @param name 数据源名
     * @param dataSource 数据源
     * @param onlyIfAbsent 是否仅不存在时添加
     */
    private void add(String name, DataSource dataSource, boolean onlyIfAbsent) {
        // 如果存在该名称或添加后返回的不是null(返回的是已存在对象)，则抛出异常
        if (onlyIfAbsent && (DATASOURCE_MAP.get(name) != null || DATASOURCE_MAP.putIfAbsent(name, dataSource) != null)) {
            throw new RuntimeException("datasource " + name + " is exists");
        } else {
            DATASOURCE_MAP.put(name, dataSource);
        }
    }

    /**
     * 添加数据源
     * @param name 数据源名
     * @param dataSource 数据源
     * @return 如果重名会抛异常 否则返回添加的数据源
     */
    public DataSource add(String name, DataSource dataSource) {
        add(name, dataSource, true);
        return dataSource;
    }

    /**
     * 替换数据源
     * @param name 数据源名
     * @param dataSource 数据源
     * @return 如果重名会抛异常 否则返回添加的数据源
     */
    public DataSource replace(String name, DataSource dataSource) {
        add(name, dataSource, false);
        return dataSource;
    }

    /**
     * 移除数据源
     * @param name 数据源名
     * @return 移除的数据源 如果没有则返回null
     */
    public DataSource remove(String name) {
        return DATASOURCE_MAP.remove(name);
    }

    @Override
    public DataSource route() {
        String name = datasourceName.get();
        return DATASOURCE_MAP.get(name);
    }

    @Override
    public void close() {
        DATASOURCE_MAP.values().forEach(DataSourceUtils::close);
    }
}