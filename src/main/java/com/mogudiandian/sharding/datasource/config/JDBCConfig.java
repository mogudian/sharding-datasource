package com.mogudiandian.sharding.datasource.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

/**
 * JDBC的配置
 * @author sunbo
 */
@Getter
@Setter
public class JDBCConfig {

    /**
     * 连接地址
     */
    private String jdbcUrl;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 其他参数
     */
    private Properties properties;
}
