package controller;

import config.Application;
import dao.Account;
import dao.User;
import exception.PersistedEntityException;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.AccountService;
import service.UserService;
import spark.Spark;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.with;
import static org.eclipse.jetty.http.HttpStatus.Code.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AccountControllerTest {
    private static final int SPARK_JAVA_DEFAULT_PORT = 4567;
    private static final String ACCOUNT_ENDPOINT = "/account";
    private static final String ACCOUNT_GET_ENDPOINT_FORMAT = ACCOUNT_ENDPOINT + "/%s";

    private UUID userId;

    @BeforeAll
    static void beforeAllSetup() {
        RestAssured.port = SPARK_JAVA_DEFAULT_PORT;
    }

    @BeforeEach
    void beforeEachSetup() throws PersistedEntityException, SQLException {
        Application.main(new String[]{});
        Spark.awaitInitialization();

        String uniquePhoneNumber = new Random().ints(48, 57 + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPhoneNumber(uniquePhoneNumber);
        user.setDateOfBirth(LocalDate.of(1995, 9, 4));

        new UserService().store(user);

        userId = user.getId();
    }

    @Test
    @DisplayName("Account with a valid User, minimum balance should be created in the database")
    void testPostMethodPasses() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT).then().statusCode(CREATED.getCode());
    }

    @Test
    @DisplayName("Account with an invalid User should not be created")
    void testPostMethodFailsWithInvalidUser() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(UUID.randomUUID());

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT).then().statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Account with balance below minimum_balance should not be created")
    void testPostMethodFailsWith() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(2999));
        testAccount.setUserId(UUID.randomUUID());

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT).then().statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Multiple accounts may be created for a single user")
    void testPostMethodPassesWithMultipleAccounts() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);

        assertDoesNotThrow(() -> new AccountService().store(testAccount));

        testAccount.setId(null);
        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT).then().statusCode(CREATED.getCode());
    }

    @Test
    @DisplayName("Get with valid ID should return expected user")
    void testGetMethodPasses() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);

        assertDoesNotThrow(() -> new AccountService().store(testAccount));

        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, testAccount.getId())).then().statusCode(OK.getCode());
    }

    @Test
    @DisplayName("Get with invalid ID should fail")
    void testGetMethodFailsWithInvalidId() {
        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, UUID.randomUUID())).then().statusCode(NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Get without ID parameter should return all the records")
    void testGetWithoutIdParameterPasses() {
        get(ACCOUNT_ENDPOINT).then().statusCode(OK.getCode());
    }

    @Test
    @DisplayName("PUT with valid Account Id should successfully update the record")
    void testPutPasses() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);

        assertDoesNotThrow(() -> new AccountService().store(testAccount));

        testAccount.setBlocked(true);
        with().body(testAccount).when().request(Method.PUT, ACCOUNT_ENDPOINT).then().statusCode(OK.getCode());
    }

    @Test
    @DisplayName("PUT with invalid User Id should fail")
    void testPutFailsWithInvalidUserId() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);

        testAccount.setId(UUID.randomUUID());
        with().body(testAccount).when().request(Method.PUT, ACCOUNT_ENDPOINT).then().statusCode(BAD_REQUEST.getCode());
    }
}
