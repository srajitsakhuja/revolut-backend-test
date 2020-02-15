import controller.AccountController;
import controller.UserController;

import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static spark.Spark.get;
import static spark.Spark.post;

public class ApplicationClass {
    public static void main(String[] args) {
        UserController userController = new UserController();
        AccountController accountController = new AccountController();

        post("/user", APPLICATION_JSON.asString(), userController.createRoute);
        get("/user/:id", userController.findRoute);

        post("/account", APPLICATION_JSON.asString(), accountController.createRoute);
        get("account/:id", accountController.findRoute);
    }
}
