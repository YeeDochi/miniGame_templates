package org.example.templets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.example.templets", "org.example.common"})
public class TempletApplication {
    public static void main(String[] args) {
        SpringApplication.run(TempletApplication.class, args);
    }
}
    