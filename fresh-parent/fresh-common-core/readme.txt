common-core 公共基础模块开发文档
1 模块定位与业务职责
1.1 模块定位
无独立启动服务，纯 Maven 公共依赖 Jar 包，所有 8 个微服务统一引入，提供全局通用能力，消除重复代码、统一系统标准、封装底层工具，是整个项目底层支撑基础模块。
1.2 核心职责
统一全局返回封装、分页通用实体、异常统一定义；
全局统一异常处理器、Feign 远程调用异常拦截；
通用工具类：雪花 ID、日期、加密、JSON、文件、Redis 工具、微信工具；
系统全局常量：订单状态、售后等级、消息类型、AI 类型、MQ Topic 常量；
通用 DTO/VO 基类、分页基类、树形结构通用实体；
逻辑删除、自动填充创建人 / 创建时间 MyBatis-Plus 公共插件；
统一请求上下文、用户信息透传工具；
全局自定义业务异常枚举、错误码规范统一管理。
1.3 依赖范围
仅依赖基础中间件 SDK、工具包，不依赖任何业务微服务，无 Nacos/Redis/RocketMQ 业务配置，所有微服务直接 scope=import 引入。


2 Maven 依赖 pom.xml（common-core）
xml




<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.fresh</groupId>
        <artifactId>fresh-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>../fresh-parent/pom.xml</relativePath>
    </parent>

    <artifactId>common-core</artifactId>
    <name>AI生鲜团购-公共基础模块</name>
    <description>所有微服务通用工具、实体、异常、常量、统一返回</description>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Spring Web 基础实体依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- MyBatis-Plus 公共插件、自动填充 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        <!-- OpenFeign 统一异常处理基类 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <!-- Redis通用工具封装依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <!-- RocketMQ 消息通用DTO、Topic常量 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-stream-rocketmq</artifactId>
        </dependency>
        <!-- JSON序列化工具 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>
        <!-- 雪花算法ID生成 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-core</artifactId>
        </dependency>
        <!-- 加密工具 MD5、AES -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-crypto</artifactId>
        </dependency>
        <!-- 树结构、集合工具 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-collection</artifactId>
        </dependency>
        <!-- lombok 简化实体代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>



3 整体包结构分层设计
plaintext
com.fresh.common
├── base                // 通用基类、统一返回、分页、树形实体
│   ├── Result.java     // 全局统一返回对象
│   ├── PageDTO.java    // 分页入参基类
│   ├── PageVO.java     // 分页出参基类
│   └── TreeEntity.java // 树形分类通用实体
├── constant            // 全局系统常量（状态、MQ、AI、订单）
│   ├── OrderConstant.java
│   ├── AfterSaleConstant.java
│   ├── RocketMQTopicConstant.java
│   ├── AiConstant.java
│   └── GlobalCodeConstant.java // 统一错误码
├── exception           // 全局异常、异常枚举、全局异常处理器
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   └── ErrorCodeEnum.java
├── plugin              // MyBatis-Plus公共插件
│   └── AutoFillHandler.java // 创建/更新时间自动填充
├── util                // 全套通用工具类
│   ├── IdUtil.java        // 雪花ID
│   ├── DateUtil.java      // 日期格式化
│   ├── RedisUtil.java     // Redis通用操作封装
│   ├── JsonUtil.java      // JSON序列化
│   ├── CryptoUtil.java    // 密码加密
│   ├── WechatUtil.java    // 微信小程序工具
│   └── ContextUtil.java   // 请求上下文用户信息透传
└── dto                 // MQ通用消息体、Feign公共传输对象
    ├── MqBaseDTO.java    // 所有MQ消息父类
    └── UserContextDTO.java // 网关透传用户上下文
4 核心通用类详细说明
4.1 统一返回 Result.java（全系统接口标准输出）
java
运行
@Data
public class Result<T> {
    private Integer code; // 200成功 4xx参数/权限 5xx服务异常
    private String msg;
    private T data;

    // 静态快捷方法
    public static <T> Result<T> success(T data) {}
    public static <T> Result<T> success() {}
    public static <T> Result<T> fail(Integer code, String msg) {}
    public static <T> Result<T> fail(ErrorCodeEnum error) {}
}
全网关、所有微服务接口强制使用该返回体，前端统一解析 code 判断业务状态。
4.2 全局错误码枚举 ErrorCodeEnum
统一定义全系统错误码，杜绝魔术数字：
200 成功
401 Token 失效未登录
403 无操作权限
400 参数校验失败
429 接口限流访问频繁
503 服务熔断降级
业务自定义：10001 库存不足、20001AI 服务异常、30001 订单已取消
4.3 全局异常处理器 GlobalExceptionHandler
所有微服务自动注入，统一捕获：
自定义业务异常 BusinessException
参数校验异常 MethodArgumentNotValidException
Feign 远程调用异常
SQL、空指针系统异常
捕获后自动封装为标准 Result 返回，避免重复 try-catch。
4.4 MQ 基础消息父类 MqBaseDTO
所有 RocketMQ 消息统一继承，统一携带追踪 ID、操作人、业务时间，方便日志链路追踪：
java
运行
@Data
public class MqBaseDTO implements Serializable {
    private String traceId; // 全链路追踪ID
    private Long operateUserId; // 操作人ID
    private LocalDateTime operateTime;
}
4.5 MyBatis-Plus 自动填充插件 AutoFillHandler
全局统一填充：create_time、update_time、del_flag，所有数据表无需手动赋值。
4.6 ContextUtil 请求上下文工具
网关解析 Token 后将用户信息存入 Request 上下文，各服务直接获取当前登录用户 ID、角色，无需重复解析 Token：
java
运行
// 获取当前登录小程序用户ID
Long userId = ContextUtil.getUserId();
// 获取后台管理员ID
Long adminId = ContextUtil.getAdminId();
5 全局常量规范（统一管控，禁止硬编码）
5.1 RocketMQTopicConstant
存放全部 Topic 名称，所有生产者 / 消费者统一引用，防止字符串写错：
ORDER_CREATE_TOPIC
AFTER_SALE_IMAGE_TOPIC
USER_BEHAVIOR_TOPIC
AI_RECOGNIZE_FINISH_TOPIC
5.2 OrderConstant 订单状态
0 待支付、1 待自提、2 已完成、3 已取消、4 售后中
5.3 AfterSaleConstant 损坏等级
1 轻微、2 中度、3 重度
5.4 AiConstant AI 会话、识别类型
客服对话、菜谱生成、图片理赔、文案生成
6 通用工具类能力说明
IdUtil：雪花算法生成分布式唯一 ID，替代数据库自增（可无缝切换分布式部署）；
RedisUtil：封装 set/get/incr/ 分布式锁 / 过期设置，各服务无需重复编写 Redis 模板代码；
CryptoUtil：用户密码 MD5 加盐加密、接口 AES 加密；
JsonUtil：统一序列化、反序列化，处理 LocalDateTime 时间格式；
WechatUtil：微信 code 换取 openid、小程序 access_token 封装；
DateUtil：日期格式化、延时时间换算、日期区间判断。
7 与其他微服务依赖规范
所有 8 个业务微服务 pom 强制引入 common-core，scope 默认 compile；
xml
<dependency>
    <groupId>com.fresh</groupId>
    <artifactId>common-core</artifactId>
    <version>1.0.0</version>
</dependency>
common-core禁止依赖任何业务服务，无 Feign 客户端、无 Mapper、无业务数据库；
3 公共模块仅提供工具与标准，不包含任何业务逻辑；
新增全局状态、错误码、MQ Topic 仅在此模块修改，一处修改全项目生效。
8 无部署说明
common-core 是依赖 Jar 包，无启动类、无 Docker、无 Nacos 配置、无数据库；
打包后上传私服，其他微服务直接引用，无需单独部署运行。
9 跨模块统一约束规范
所有接口返回必须使用Result<T>，禁止自定义返回体；
状态数字全部引用 Constant 常量类，禁止直接写 0/1/2；
抛出异常统一使用throw new BusinessException(ErrorCodeEnum.XXX)；
MQ 消息实体统一继承 MqBaseDTO；
分页入参统一继承 PageDTO；
获取登录用户必须使用 ContextUtil，禁止自行解析 Header Token；
Redis 操作统一调用 RedisUtil 工具，重复逻辑禁止重复封装。