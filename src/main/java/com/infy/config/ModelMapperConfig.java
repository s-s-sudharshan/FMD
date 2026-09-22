package com.infy.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single, shared ModelMapper bean (plan.md "Tech Stack Additions"). Created
 * once here and injected into every service that needs entity<->DTO mapping;
 * never re-instantiated per module.
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
