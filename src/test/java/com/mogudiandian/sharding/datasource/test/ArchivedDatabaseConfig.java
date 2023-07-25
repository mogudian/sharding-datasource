package com.mogudiandian.sharding.datasource.test;

import com.mogudiandian.sharding.datasource.config.JDBCConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * 归档库的配置
 * @author Joshua Sun
 */
@Getter
@Setter
public class ArchivedDatabaseConfig {

    /**
     * 起始年份
     */
    private int startYear;

    /**
     * 结束年份（包含）
     */
    private int endYear;

    /**
     * 数据库配置
     */
    private JDBCConfig databaseConfig;

}
