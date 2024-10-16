package com.yz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

@Configuration
@MapperScan("com.yz.mapper")
public class MybatisPlusConfig {
	    @Bean
	    public MybatisPlusInterceptor innerInterceptor() {
	        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
	        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
	        return interceptor;
	    }
}
