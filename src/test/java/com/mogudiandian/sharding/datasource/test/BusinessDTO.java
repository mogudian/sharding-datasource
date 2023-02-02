package com.mogudiandian.sharding.datasource.test;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务DTO
 * @author sunbo
 */
@Getter
@Setter
public class BusinessDTO {

    /**
     * 业务ID
     */
    private Long id;

    /**
     * 数据源配置
     */
    private BusinessDatasourceConfig datasourceConfig;

}
