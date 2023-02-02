package com.mogudiandian.sharding.datasource.test;

import lombok.Getter;
import lombok.Setter;

/**
 * 企业DTO
 * @author sunbo
 */
@Getter
@Setter
public class CompanyDTO {

    /**
     * 企业ID
     */
    private Long id;

    /**
     * 数据源配置
     */
    private CompanyDatasourceConfig datasourceConfig;

}
