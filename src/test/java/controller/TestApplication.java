package controller;

import com.google.inject.Guice;
import com.google.inject.Injector;
import config.RoutingConfiguration;

public class TestApplication {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new TestModule());
        RoutingConfiguration.configureRoutes(injector);
    }
}
