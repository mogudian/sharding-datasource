package com.mogudiandian.sharding.datasource;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * 路由数据源
 * @author sunbo
 */
public abstract class RoutingDataSource implements DataSource, Closeable {

    /**
     * 路由方法
     * @return 路由到哪个数据源
     */
    public abstract DataSource route();

    /**
     * 是否存在数据源
     * @return 是否存在
     */
    public boolean hasDatasource() {
        DataSource route = route();
        if (route == null) {
            return false;
        }
        if (!(route instanceof RoutingDataSource)) {
            return true;
        }
        return ((RoutingDataSource) route).hasDatasource();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return route().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return route().getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return route().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return route().isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return route().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        route().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        route().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return route().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return route().getParentLogger();
    }
}