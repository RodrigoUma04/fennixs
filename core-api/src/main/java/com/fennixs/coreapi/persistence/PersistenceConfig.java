package com.fennixs.coreapi.persistence;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PersistenceConfig {

    @Bean
    RlsTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new RlsTransactionManager(entityManagerFactory);
    }
}
