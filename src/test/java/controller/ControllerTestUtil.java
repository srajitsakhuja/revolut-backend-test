package controller;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import dao.User;
import io.restassured.RestAssured;
import spark.Spark;

public class ControllerTestUtil {
    static final String DUMMY_FIRST_NAME = "foo";
    static final String DUMMY_LAST_NAME = "bar";

    private static final int SPARK_JAVA_DEFAULT_PORT = 4567;


    static User createDummyUser() {
        String uniquePhoneNumber = new Random().ints(48, 57 + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(DUMMY_FIRST_NAME);
        user.setLastName(DUMMY_LAST_NAME);
        user.setPhoneNumber(uniquePhoneNumber);
        user.setDateOfBirth(LocalDate.of(1995, 9, 4));
        return user;
    }

    static void configureControllerTest() {
        RestAssured.port = SPARK_JAVA_DEFAULT_PORT;
        TestApplication.main(new String[]{});
        Spark.awaitInitialization();
    }
}
