package com.example.buildpro.config;

import java.util.Arrays;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Works around a Spring Boot 4.0.x autoconfiguration bug - reproduced on
// every one of 4.0.4, 4.0.4+resolutionStrategy.force(liquibase-core:4.33.0),
// and 4.0.8 with Java 21.0.2 (both locally and on Railway), so it is not a
// Liquibase-core version issue and not fixed by a Boot patch bump (see the
// "Liquibase / Spring Boot version note" in README.md and the matching
// CHANGELOG entry for the full history).
//
// LiquibaseAutoConfiguration imports DatabaseInitializationDependencyConfigurer,
// which is meant to make entityManagerFactory depend on liquibase (JPA should
// wait for migrations to run first - correct, and kept). For this app's bean
// combination, Boot 4.0.x also ends up wiring the reverse edge - liquibase
// depending on entityManagerFactory - which has no reason to exist (Liquibase
// doesn't need JPA to be ready). Spring's own circular-dependency check then
// rejects the cycle outright:
//   "Circular depends-on relationship between 'liquibase' and
//   'entityManagerFactory'"
// - crashing on every single startup attempt.
//
// This BeanFactoryPostProcessor runs before the context refreshes and simply
// removes "entityManagerFactory" from the liquibase bean definition's
// dependsOn list, if present. It does NOT touch entityManagerFactory's own
// dependsOn (still depends on liquibase), so migrations still run before JPA
// starts - only the bad reverse edge is gone, which is what breaks the cycle.
//
// If a future Spring Boot upgrade fixes this properly upstream, this class
// becomes a no-op (there will be nothing to strip) rather than a hazard, so
// it's safe to leave in place; remove it once a release has been confirmed
// not to need it.
@Configuration
public class LiquibaseJpaDependsOnFixConfig {

    private static final String LIQUIBASE_BEAN_NAME = "liquibase";
    private static final String ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

    @Bean
    static BeanFactoryPostProcessor liquibaseCircularDependsOnFix() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            if (!beanFactory.containsBeanDefinition(LIQUIBASE_BEAN_NAME)) {
                return;
            }
            BeanDefinition liquibaseDefinition = beanFactory.getBeanDefinition(LIQUIBASE_BEAN_NAME);
            String[] dependsOn = liquibaseDefinition.getDependsOn();
            if (dependsOn == null || dependsOn.length == 0) {
                return;
            }
            String[] filtered = Arrays.stream(dependsOn)
                    .filter(name -> !ENTITY_MANAGER_FACTORY_BEAN_NAME.equals(name))
                    .toArray(String[]::new);
            liquibaseDefinition.setDependsOn(filtered);
        };
    }
}
