package org.barcelonajug.battlecontender.ai;

import org.springaicommunity.typesafe.TypeSafeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TypeSafeClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(TypeSafeClient.class)
    @ConditionalOnProperty(name = "TYPESAFE_API_KEY")
    TypeSafeClient typeSafeClient(Environment environment) {
        return TypeSafeClient.builder().apiKey(environment.getRequiredProperty("TYPESAFE_API_KEY")).build();
    }
}
