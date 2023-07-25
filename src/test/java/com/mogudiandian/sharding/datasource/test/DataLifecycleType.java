package com.mogudiandian.sharding.datasource.test;

/**
 * 数据的生命周期类型
 * @author Joshua Sun
 */
public enum DataLifecycleType {

    /**
     * 处理中的
     */
    PROCESSING,

    /**
     * 已完成的
     */
    FINISHED,

    /**
     * 已归档的
     */
    ARCHIVED,

    /**
     * 未归档的（处理中的+已完成的）
     */
    UNARCHIVED;

}
