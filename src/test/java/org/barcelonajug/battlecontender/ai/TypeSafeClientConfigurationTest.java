package org.barcelonajug.battlecontender.ai;

import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TypeSafeClientConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TypeSafeClientConfiguration.class);

    @Test
    void createsClientWhenKeyIsSet() {
        contextRunner.withPropertyValues("TYPESAFE_API_KEY=dummy")
                .run(context -> assertThat(context).hasSingleBean(TypeSafeClient.class));
    }

    @Test
    void startsWithoutClientWhenKeyIsAbsent() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(TypeSafeClient.class));
    }
}
