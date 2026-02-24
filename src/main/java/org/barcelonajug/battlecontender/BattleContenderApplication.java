package org.barcelonajug.battlecontender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BattleContenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(BattleContenderApplication.class, args);
    }
}
