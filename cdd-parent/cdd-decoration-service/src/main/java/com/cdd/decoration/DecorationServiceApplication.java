package com.cdd.decoration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;

@SpringBootApplication(exclude = MybatisAutoConfiguration.class)
public class DecorationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DecorationServiceApplication.class, args);
    }
}
