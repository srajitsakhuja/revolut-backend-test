package config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import config.Module;
import controller.AccountController;
import controller.UserController;

import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static spark.Spark.get;
import static spark.Spark.post;

public class Application {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new Module());

        UserController userController = injector.getInstance(UserController.class);
        AccountController accountController = injector.getInstance(AccountController.class);

        post("/user", APPLICATION_JSON.asString(), userController.createRoute);
        get("/user/:id", userController.findRoute);

        post("/account", APPLICATION_JSON.asString(), accountController.createRoute);
        get("account/:id", accountController.findRoute);
    }
}
