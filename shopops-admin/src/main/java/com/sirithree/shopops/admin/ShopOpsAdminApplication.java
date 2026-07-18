package com.sirithree.shopops.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.sirithree.shopops.admin.persistence.mapper")
@SpringBootApplication(scanBasePackages = "com.sirithree.shopops")
public class ShopOpsAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopOpsAdminApplication.class, args);
    }
}
