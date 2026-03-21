package com.cdd.db.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DbMigrationApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DbMigrationApplication.class, args);
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
