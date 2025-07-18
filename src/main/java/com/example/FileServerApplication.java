package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class FileServerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(FileServerApplication.class, args);
        System.out.println("AWS Access Key: " + ctx.getEnvironment().getProperty("aws.access-key"));
        System.out.println("AWS Region: " + ctx.getEnvironment().getProperty("aws.s3.region"));
    }
}
