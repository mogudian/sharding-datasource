package com.mogudiandian.sharding.datasource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按年度分片数据源
 * @author Joshua Sun
 */
public final class YearRoutingDataSource extends RoutingDataSource implements DataSource {

    /**
     * 切换数据源的标记
     */
    private final ThreadLocal<Integer> datasourceYear;

    /**
     * 年 -> 数据源
     */
    private static final Map<Integer, DataSource> cache = new ConcurrentHashMap<>();

    /**
     * 年范围 -> 数据源
     */
    private final Map<YearRange, DataSource> datasourceMap = Collections.synchronizedMap(new TreeMap<>());

    public YearRoutingDataSource(ThreadLocal<Integer> datasourceYear) {
        Objects.requireNonNull(datasourceYear, "Datasource year must be specified");
        this.datasourceYear = datasourceYear;
    }

    public void putDatasource(YearRange yearRange, DataSource dataSource) {
        datasourceMap.put(yearRange, dataSource);
    }

    /**
     * 这里是真正的route方法
     * @param year 年
     * @return 数据源
     */
    private DataSource route(int year) {
        // 遍历每一个数据源 找到符合条件（范围内）的
        for (Map.Entry<YearRange, DataSource> entry : datasourceMap.entrySet()) {
            YearRange range = entry.getKey();
            if (range.getStart() <= year && range.getEnd() >= year) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("Not found datasource config for year " + year);
    }

    @Override
    public DataSource route() {
        int year = datasourceYear.get();

        // 这里用了缓存
        DataSource dataSource = cache.get(year);
        if (dataSource == null) {
            dataSource = route(year);
            // 这块不用 computeIfAbsent 是因为这个方法有性能问题 对于这种简单计算 就算被击穿也不会有问题 比compute要好
            cache.putIfAbsent(year, dataSource);
        }
        return dataSource;
    }

    @Override
    public void close() {
        datasourceMap.values().forEach(DataSourceUtils::close);
    }
}