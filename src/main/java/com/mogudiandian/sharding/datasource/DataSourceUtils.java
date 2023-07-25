package com.mogudiandian.sharding.datasource;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.lang.reflect.Method;

/**
 * 数据源工具类
 *
 * @author Joshua Sun
 */
@Slf4j
public final class DataSourceUtils {

    private DataSourceUtils() {
    }

    /**
     * 关闭数据源
     * @param dataSource 数据源
     */
    public static void close(DataSource dataSource) {
        // 为空忽略
        if (dataSource == null) {
            return;
        }
        // 如果数据源是AutoCloseable的子类，则调用关闭方法，我们的RoutingDataSource和Hikari的数据源都是可关闭的
        if (AutoCloseable.class.isAssignableFrom(dataSource.getClass())) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                log.warn("close datasource {} throws ", dataSource, e);
            }
        }
        // 否则探测是否有close方法，如果有则调用，很暴力，sharding-sphere里面就是这么写的。。。
        try {
            Method method = dataSource.getClass().getDeclaredMethod("close");
            method.setAccessible(true);
            method.invoke(dataSource);
        } catch (ReflectiveOperationException ignored) {
        }
    }

}
