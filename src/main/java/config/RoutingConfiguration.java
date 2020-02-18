package config;

import com.google.inject.Injector;
import controller.AccountController;
import controller.UserController;

import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

public class RoutingConfiguration {
    public static final String USER_ENDPOINT = "/user";
    public static final String ACCOUNT_ENDPOINT = "/account";
    public static final String TRANSFER_ENDPOINT = "/transfer";
    public static final String DEPOSIT_ENDPOINT = "/deposit";

    public static void configureRoutes(Injector injector) {
        UserController userController = injector.getInstance(UserController.class);
        AccountController accountController = injector.getInstance(AccountController.class);

        post(USER_ENDPOINT, APPLICATION_JSON.asString(), userController.createRoute);
        get(USER_ENDPOINT + "/:id", userController.findRoute);
        get(USER_ENDPOINT, userController.findAllRoute);
        put(USER_ENDPOINT, APPLICATION_JSON.asString(), userController.updateRoute);

        post(ACCOUNT_ENDPOINT, APPLICATION_JSON.asString(), accountController.createRoute);
        get(ACCOUNT_ENDPOINT + "/:id", accountController.findRoute);
        get(ACCOUNT_ENDPOINT, accountController.findAllRoute);
        put(ACCOUNT_ENDPOINT, APPLICATION_JSON.asString(), accountController.updateRoute);

        put(TRANSFER_ENDPOINT, APPLICATION_JSON.asString(), accountController.transferFundsRoute);
        put(DEPOSIT_ENDPOINT, APPLICATION_JSON.asString(), accountController.depositFundsRoute);
    }
}
