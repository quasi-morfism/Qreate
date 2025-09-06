package com.morfism.aiappgenerator;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.morfism.aiappgenerator.mapper")
public class AiAppGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAppGeneratorApplication.class, args);
    }

}
