package com.mogudiandian.sharding.datasource.test;

/**
 * 数据源的上下文
 * @author sunbo
 */
public final class DatasourceContext {

    private DatasourceContext() {
    }

    /**
     * 当前企业ID
     */
    public static ThreadLocal<Long> currentCompanyId = new ThreadLocal<>();

    /**
     * 当前业务ID
     */
    public static ThreadLocal<Long> currentBusinessId = new ThreadLocal<>();

    /**
     * 当前企业数据的对象
     */
    public static ThreadLocal<CompanyDataObjectName> currentCompanyDataObject = new ThreadLocal<>();

    /**
     * 当前任务数据的对象
     */
    public static ThreadLocal<TaskObjectName> currentTaskDataObject = new ThreadLocal<>();

    /**
     * 当前数据的生命周期
     */
    public static ThreadLocal<DataLifecycleType> currentDataLifecycle = new ThreadLocal<>();

    /**
     * 当前数据的年份（仅用于归档数据）
     */
    public static ThreadLocal<Integer> currentDataYear = new ThreadLocal<>();

}
