package com.mogudiandian.sharding.datasource;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多数据源
 * @author Joshua Sun
 */
public class KeyRoutingDataSource<K extends Serializable> extends RoutingDataSource implements DataSource {

    /**
     * 切换数据源的标记
     */
    private final ThreadLocal<K> datasourceKey;

    /**
     * 数据源标识 -> 数据源
     */
    protected Map<K, DataSource> datasourceMap = Collections.synchronizedMap(new HashMap<>());

    protected DataSource defaultDatasource;

    public KeyRoutingDataSource(ThreadLocal<K> datasourceKey) {
        Objects.requireNonNull(datasourceKey, "Datasource key must be specified");
        this.datasourceKey = datasourceKey;
    }

    public KeyRoutingDataSource(ThreadLocal<K> datasourceKey, DataSource defaultDatasource) {
        this(datasourceKey);
        this.defaultDatasource = defaultDatasource;
    }

    public void putDatasource(K key, DataSource dataSource) {
        datasourceMap.put(key, dataSource);
    }

    @Override
    public DataSource route() {
        // 根据map中的key查询，如果可以查到说明有相应的数据源，否则看是否有默认的数据源，如果有则使用，没有则抛异常
        K key = datasourceKey.get();

        DataSource datasource = datasourceMap.get(key);
        if (datasource == null) {
            datasource = defaultDatasource;
        }
        if (datasource == null) {
            throw new RuntimeException("No such datasource: " + key);
        }
        return datasource;
    }

    @Override
    public void close() {
        DataSourceUtils.close(defaultDatasource);
        datasourceMap.values().forEach(DataSourceUtils::close);
    }
}