package config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import controller.AccountController;
import controller.UserController;

import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

public class Application {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new Module());

        UserController userController = injector.getInstance(UserController.class);
        AccountController accountController = injector.getInstance(AccountController.class);

        post("/user", APPLICATION_JSON.asString(), userController.createRoute);
        get("/user/:id", userController.findRoute);
        get("/user", userController.findAllRoute);
        put("/user", APPLICATION_JSON.asString(), userController.updateRoute);

        post("/account", APPLICATION_JSON.asString(), accountController.createRoute);
        get("account/:id", accountController.findRoute);
        get("/account", accountController.findAllRoute);
        put("/account", APPLICATION_JSON.asString(), accountController.updateRoute);

    }
}
