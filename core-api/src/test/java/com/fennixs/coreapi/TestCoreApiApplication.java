package com.fennixs.coreapi;

import org.springframework.boot.SpringApplication;

public class TestCoreApiApplication {

    static void main(String[] args) {
        SpringApplication.from(CoreApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
