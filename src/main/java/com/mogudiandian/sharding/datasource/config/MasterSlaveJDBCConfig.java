package com.mogudiandian.sharding.datasource.config;

import lombok.Getter;
import lombok.Setter;

/**
 * JDBC的配置
 * @author Joshua Sun
 */
@Getter
@Setter
public class MasterSlaveJDBCConfig {

    /**
     * 主库配置
     */
    private JDBCConfig masterConfig;

    /**
     * 从库配置
     */
    private JDBCConfig[] slavesConfig;

}
