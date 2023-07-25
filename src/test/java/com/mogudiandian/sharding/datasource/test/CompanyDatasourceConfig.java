package com.mogudiandian.sharding.datasource.test;

import com.mogudiandian.sharding.datasource.config.JDBCConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 业务数据源配置
 * @author Joshua Sun
 */
@Getter
@Setter
public class CompanyDatasourceConfig {

    /**
     * 业务数据源配置
     */
    private List<BusinessDatasourceConfig> datasourceConfigs;

    /**
     * 其他数据源配置
     */
    private JDBCConfig otherDatabaseConfig;

}
