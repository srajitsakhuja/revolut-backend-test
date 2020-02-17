package controller;

import config.Application;
import dao.Account;
import dao.Deposit;
import dao.Transfer;
import dao.User;
import exception.PersistedEntityException;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static config.Application.*;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.with;
import static org.eclipse.jetty.http.HttpStatus.Code.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AccountControllerTest {
    private static final int SPARK_JAVA_DEFAULT_PORT = 4567;
    private static final String ACCOUNT_GET_ENDPOINT_FORMAT = ACCOUNT_ENDPOINT + "/%s";
    private static final String USER_ID_FIELD_NAME = "userId";
    private static final String BALANCE_ID_FIELD_NAME = "balance";
    private static final String BLOCKED_FIELD_NAME = "blocked";


    private UUID userId;
    private Account testAccount;

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
        testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);
    }

    @Test
    @DisplayName("Account with a valid User, minimum balance should be created in the database")
    void testPostMethodPasses() {
        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3000))
                .body(USER_ID_FIELD_NAME, is(userId.toString()));
    }

    @Test
    @DisplayName("Account with an invalid User should not be created")
    void testPostMethodFailsWithInvalidUser() {
        testAccount.setUserId(UUID.randomUUID());

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT)
                .then().assertThat().
                statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Account with balance below minimum_balance should not be created")
    void testPostMethodFailsWith() {
        testAccount.setBalance(new BigDecimal(2999));
        testAccount.setUserId(UUID.randomUUID());

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Multiple accounts may be created for a single user")
    void testPostMethodPassesWithMultipleAccounts() {
        assertDoesNotThrow(() -> new AccountService().store(testAccount));
        testAccount.setId(null);

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3000))
                .body(USER_ID_FIELD_NAME, is(userId.toString()));
    }

    @Test
    @DisplayName("Get with valid ID should return expected user")
    void testGetMethodPasses() {
        assertDoesNotThrow(() -> new AccountService().store(testAccount));

        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, testAccount.getId()))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3000))
                .body(USER_ID_FIELD_NAME, is(userId.toString()));
    }

    @Test
    @DisplayName("Get with invalid ID should fail")
    void testGetMethodFailsWithInvalidId() {
        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, UUID.randomUUID()))
                .then().assertThat().
                statusCode(NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Get without ID parameter should return all the records")
    void testGetWithoutIdParameterPasses() {
        get(ACCOUNT_ENDPOINT).then().statusCode(OK.getCode());
    }

    @Test
    @DisplayName("PUT with valid Account Id should successfully update the record")
    void testPutPasses() {
        assertDoesNotThrow(() -> new AccountService().store(testAccount));

        testAccount.setBlocked(true);
        with().body(testAccount).when().request(Method.PUT, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3000))
                .body(USER_ID_FIELD_NAME, is(userId.toString()))
                .body(BLOCKED_FIELD_NAME, is(true));
    }

    @Test
    @DisplayName("PUT with invalid User Id should fail")
    void testPutFailsWithInvalidUserId() {
        testAccount.setId(UUID.randomUUID());
        with().body(testAccount).when().request(Method.PUT, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Depositing Funds into a valid account should successfully update the corresponding record")
    void testDepositPasses() {
        assertDoesNotThrow(() -> new AccountService().store(testAccount));

        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, testAccount.getId()))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3000));

        Deposit deposit = new Deposit();
        deposit.setAccountId(testAccount.getId());
        deposit.setAmount(new BigDecimal(200));

        with().body(deposit).when().request(Method.PUT, DEPOSIT_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode());

        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, testAccount.getId()))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3200));
    }

    @Test
    @DisplayName("Transferring Funds b/w valid accounts should successfully update the corresponding records")
    void testTransferPasses() {
        testAccount.setBalance(new BigDecimal(5000));

        assertDoesNotThrow(() -> new AccountService().store(testAccount));
        UUID fromAccountId = testAccount.getId();
        testAccount.setId(null);
        assertDoesNotThrow(() -> new AccountService().store(testAccount));
        UUID toAccountId = testAccount.getId();

        Transfer transfer = new Transfer();
        transfer.setAmount(new BigDecimal(200));
        transfer.setFromAccountId(fromAccountId);
        transfer.setToAccountId(toAccountId);

        with().body(transfer).when().request(Method.PUT, TRANSFER_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode());

        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, fromAccountId))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(4800));

        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, toAccountId))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(5200));
    }

    @Test
    @DisplayName("Transferring Funds b/w valid accounts fails when minimum balance constraint is violated")
    @Disabled
    //TODO - BUGFIX: The request should actually lead to a BAD_REQUEST (400) HTTP STATUS CODE.
    void testTransferFails() {
        assertDoesNotThrow(() -> new AccountService().store(testAccount));
        UUID fromAccountId = testAccount.getId();
        testAccount.setId(null);
        assertDoesNotThrow(() -> new AccountService().store(testAccount));
        UUID toAccountId = testAccount.getId();

        Transfer transfer = new Transfer();
        transfer.setAmount(new BigDecimal(200));
        transfer.setFromAccountId(fromAccountId);
        transfer.setToAccountId(toAccountId);

        with().body(transfer).when().request(Method.PUT, TRANSFER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }
}
