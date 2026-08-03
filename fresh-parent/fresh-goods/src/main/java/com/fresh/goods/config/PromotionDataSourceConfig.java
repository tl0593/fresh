package com.fresh.goods.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.fresh.goods.mapper.promotion", sqlSessionFactoryRef = "promotionSqlSessionFactory")
public class PromotionDataSourceConfig {

    @Bean(name = "promotionDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.promotion")
    public DataSource promotionDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "promotionSqlSessionFactory")
    public SqlSessionFactory promotionSqlSessionFactory(@Qualifier("promotionDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.fresh.goods.entity.promotion");
        factory.setGlobalConfig(buildGlobalConfig());
        return factory.getObject();
    }

    @Bean(name = "promotionSqlSessionTemplate")
    public SqlSessionTemplate promotionSqlSessionTemplate(@Qualifier("promotionSqlSessionFactory") SqlSessionFactory factory) {
        return new SqlSessionTemplate(factory);
    }

    @Bean(name = "promotionTransactionManager")
    public PlatformTransactionManager promotionTransactionManager(@Qualifier("promotionDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private GlobalConfig buildGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setLogicDeleteField("delFlag");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        return globalConfig;
    }
}
