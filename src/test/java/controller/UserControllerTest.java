package controller;

import config.Application;
import dao.User;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import spark.Spark;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.with;
import static org.eclipse.jetty.http.HttpStatus.Code.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {
    private static final List<UUID> USER_ID_LIST = new ArrayList<>();
    private static final int SPARK_JAVA_DEFAULT_PORT = 4567;
    private static final String USER_ENDPOINT = "/user";
    private static final String USER_GET_ENDPOINT_FORMAT = USER_ENDPOINT + "/%s";

    private User testUser;

    @BeforeAll
    static void beforeAllSetup() {
        RestAssured.port = SPARK_JAVA_DEFAULT_PORT;
    }

    @BeforeEach
    void beforeEachSetup() {
        Application.main(new String[]{});
        Spark.awaitInitialization();

        String uniquePhoneNumber = new Random().ints(48, 57 + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setFirstName("foo");
        testUser.setLastName("bar");
        testUser.setPhoneNumber(uniquePhoneNumber);
    }

    @Test
    @DisplayName("Adult User with a unique phone number should be created in the database")
    @Order(1)
    void testPostMethodPasses() {
        testUser.setDateOfBirth(LocalDate.of(1995, 9, 4));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT).then().statusCode(CREATED.getCode());

        USER_ID_LIST.add(testUser.getId());
    }

    @Test
    @DisplayName("Adult User with a guardian should not be created in the database")
    @Order(2)
    void testPostMethodFailsWithGuardianForAdultUser() {
        testUser.setDateOfBirth(LocalDate.of(1995, 9, 4));
        testUser.setGuardianId(USER_ID_LIST.get(0));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT).then().statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with a valid guardian should be created in the database")
    @Order(3)
    void testPostMethodPassesWithValidMinorUser() {
        testUser.setDateOfBirth(LocalDate.of(2005, 9, 4));
        testUser.setGuardianId(USER_ID_LIST.get(0));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT).then().statusCode(CREATED.getCode());
        USER_ID_LIST.add(testUser.getId());
    }

    @Test
    @DisplayName("Minor User with a minor guardian should not be created in the database")
    @Order(4)
    void testPostMethodFailsWithInvalidMinorUser() {
        testUser.setDateOfBirth(LocalDate.of(2005, 9, 4));
        testUser.setGuardianId(USER_ID_LIST.get(1));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT).then().statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with an invalid guardian should not be created in the database")
    @Order(4)
    void testPostMethodFailsWithInvalidGuardian() {
        testUser.setDateOfBirth(LocalDate.of(2005, 9, 4));
        testUser.setGuardianId(UUID.randomUUID());

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT).then().statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Get with valid ID should return expected user")
    @Order(5)
    void testGetMethodPasses() {
        get(String.format(USER_GET_ENDPOINT_FORMAT, USER_ID_LIST.get(0))).then().statusCode(OK.getCode());
    }

    @Test
    @DisplayName("Get with invalid ID should fail")
    @Order(6)
    void testGetMethodFailsWithInvalidId() {
        get(String.format(USER_GET_ENDPOINT_FORMAT, UUID.randomUUID())).then().statusCode(NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Get without ID parameter should return all the records")
    @Order(7)
    void foo() {
        get(USER_ENDPOINT).then().statusCode(OK.getCode());
    }

}
