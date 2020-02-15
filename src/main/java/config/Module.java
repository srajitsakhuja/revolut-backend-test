package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class).in(Singleton.class);
    }
}
