package com.cdd.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;

@SpringBootApplication(exclude = {
        MybatisAutoConfiguration.class
})
public class MerchantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantServiceApplication.class, args);
    }
}
