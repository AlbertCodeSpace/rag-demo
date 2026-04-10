# Spring Boot 快速入门

Spring Boot 让 Spring 应用的创建和部署变得前所未有的简单。

## 核心特性

### 自动配置

Spring Boot 根据 classpath 中的依赖自动配置 Bean。例如，添加了 spring-boot-starter-web 依赖后，Spring Boot 会自动配置嵌入式 Tomcat 和 Spring MVC。

### 起步依赖

Starter 是一组预定义的依赖描述符，简化了 Maven 配置。常用的 Starter 包括：
- spring-boot-starter-web：Web 应用
- spring-boot-starter-data-jpa：JPA 数据访问
- spring-boot-starter-security：安全框架

## 配置管理

Spring Boot 支持多种配置方式：application.properties、application.yml、环境变量、命令行参数。配置项按优先级加载，高优先级覆盖低优先级。

## 监控与管理

Spring Boot Actuator 提供了生产级的监控端点，包括健康检查、指标收集、环境信息等。