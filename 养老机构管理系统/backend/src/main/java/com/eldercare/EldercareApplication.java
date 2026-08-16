package com.eldercare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 养老机构管理系统 启动类
 */
@SpringBootApplication
@MapperScan("com.eldercare.mapper")
public class EldercareApplication {

    public static void main(String[] args) {
        SpringApplication.run(EldercareApplication.class, args);
        System.out.println("========== 养老机构管理系统启动成功 ==========");
        System.out.println("========== 接口文档地址：http://localhost:8080/api ==========");
    }
}