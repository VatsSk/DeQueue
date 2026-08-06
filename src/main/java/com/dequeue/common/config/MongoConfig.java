package com.dequeue.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
            new InstantToDateConverter(),
            new DateToInstantConverter()
        ));
    }
    
    static class InstantToDateConverter implements Converter<Instant, Date> {
        @Override
        public Date convert(Instant source) {
            return Date.from(source);
        }
    }
    
    static class DateToInstantConverter implements Converter<Date, Instant> {
        @Override
        public Instant convert(Date source) {
            if (source == null) return null;
            return source.toInstant();
        }
    }
}
