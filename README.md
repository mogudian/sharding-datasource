# sharding-datasource

提供 JDBC 原生`分片数据源/多数据源`支持，并可以用到 spring-boot / spring 项目中，为开发 SAAS 多租户数据隔离提供保障 

提供了 `4` 种分片数据源，对于简单场景可以按需使用其中一种，对于复杂场景可以进行嵌套使用

## 说明

### NamingDataSourcePool 根据名称路由的多数据源 通常用于固定数量的数据源

#### 例如系统有一个用户库 user 一个商品库 product 一个订单库 order
```java
```

### KeyRoutingDataSource 根据任意类型key路由的多数据源 通常用于根据id进行租户隔离

#### 例如一个SAAS系统服务于多个企业(company)，每个企业在签订合同后分配一个数据库
```java
```

### YearRoutingDataSource 根据年路由的多数据源 通常用于按照年份做归档数据

#### 例如一个业务的日志(log)按照年份进行数据划分
```java
```

### ColumnShardingDataSource 按列分片的数据源 通常用于分库分表

#### 例如一个商城系统的订单表(order)数量太大，需要根据用户唯一编码(user_id)进行水平拆分
```java
```

### 复杂场景

#### 我们的真实场景：一个SAAS系统需要支持多企业(company)，每个企业有多个业务线(business)，每个业务线包含了一批任务(task)，每个任务有任务对应的处理日志(log)，日志需要按任务ID进行分库分表，任务和日志需要分为热库、冷库、归档库，其中归档库按年存储
```java
```

## 引用项目（已发布至中央仓库）

```xml
<dependency>
    <groupId>com.mogudiandian</groupId>
    <artifactId>sharding-datasource</artifactId>
    <version>LATEST</version>
</dependency>
```

## 依赖三方库

| 依赖               | 版本号           | 说明  |
|------------------|---------------|-----|
| spring-boot-jdbc | 2.3.4.RELEASE |     |
| commons-lang3    | 3.11          |     |
| shardingsphere   | 4.1.1         |     |
| lombok           | 1.18.16       |     |
