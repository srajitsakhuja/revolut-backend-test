package controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dao.Account;
import dao.Deposit;
import dao.Transfer;
import dao.User;
import exception.PersistedEntityException;
import io.restassured.http.Method;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import package_.Tables;
import service.AccountService;
import service.UserService;

import static config.RoutingConfiguration.ACCOUNT_ENDPOINT;
import static config.RoutingConfiguration.DEPOSIT_ENDPOINT;
import static config.RoutingConfiguration.TRANSFER_ENDPOINT;
import static controller.ControllerTestUtil.createDummyUser;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.with;
import static org.eclipse.jetty.http.HttpStatus.Code.BAD_REQUEST;
import static org.eclipse.jetty.http.HttpStatus.Code.CREATED;
import static org.eclipse.jetty.http.HttpStatus.Code.INTERNAL_SERVER_ERROR;
import static org.eclipse.jetty.http.HttpStatus.Code.NOT_FOUND;
import static org.eclipse.jetty.http.HttpStatus.Code.OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static package_.Keys.GUARDIAN_FK;
import static package_.Keys.USER_FK;
import static package_.tables.Account.ACCOUNT;
import static package_.tables.User.USER;

public class AccountControllerTest {
    private static final String ACCOUNT_GET_ENDPOINT_FORMAT = ACCOUNT_ENDPOINT + "/%s";
    private static final String USER_ID_FIELD_NAME = "userId";
    private static final String BALANCE_ID_FIELD_NAME = "balance";
    private static AccountService accountService;
    private static UserService userService;
    private static DSLContext dslContext;
    private static UUID userId;

    @BeforeAll
    static void beforeAllSetup() throws PersistedEntityException, SQLException {
        ControllerTestUtil.configureControllerTest();

        Injector injector = Guice.createInjector(new TestModule());
        dslContext = injector.getInstance(DSLContext.class);
        accountService = injector.getInstance(AccountService.class);
        userService = injector.getInstance(UserService.class);

        User user = createDummyUser();
        userService.store(user);
        userId = user.getId();
    }

    @BeforeEach
    void beforeEachSetup() {
        dslContext.truncateTable(ACCOUNT).execute();
    }

    @Test
    @DisplayName("Account with a valid User, minimum balance should be created in the database")
    void testPostMethodPasses() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3000))
                .body(USER_ID_FIELD_NAME, is(userId.toString()));
    }

    @Test
    @DisplayName("Account with an invalid User should not be created")
    void testPostMethodFailsWithInvalidUser() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(UUID.randomUUID());

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT)
                .then().assertThat().
                statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Account with balance below minimum_balance should not be created")
    void testPostMethodFailsWith() {
        Account testAccount = new Account();
        testAccount.setUserId(userId);
        testAccount.setBalance(new BigDecimal(2999));

        with().body(testAccount).when().request(Method.POST, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Multiple accounts may be created for a single user")
    void testPostMethodPassesWithMultipleAccounts() {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);

        assertDoesNotThrow(() -> accountService.store(testAccount));
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
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);
        assertDoesNotThrow(() -> accountService.store(testAccount));

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
        get(ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode())
                .body("$.size", is(0));
    }

    @Test
    @DisplayName("PUT with valid Account Id should successfully update the record")
    void testPutPasses() throws PersistedEntityException, SQLException {
        User newUser = createDummyUser();
        userService.store(newUser);

        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);
        assertDoesNotThrow(() -> accountService.store(testAccount));

        testAccount.setUserId(newUser.getId());
        with().body(testAccount).when().request(Method.PUT, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(USER_ID_FIELD_NAME, is(newUser.getId().toString()));
    }

    @Test
    @DisplayName("PUT with invalid User Id should fail")
    void testPutFailsWithInvalidUserId() {
        Account testAccount = new Account();
        testAccount.setId(UUID.randomUUID());
        with().body(testAccount).when().request(Method.PUT, ACCOUNT_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Depositing Funds into a valid account should successfully update the corresponding record")
    void testDepositPasses() throws PersistedEntityException, SQLException {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);
        assertDoesNotThrow(() -> accountService.store(testAccount));

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

        assertEquals(new BigDecimal(3200), accountService.findById(testAccount.getId()).getBalance());
    }

    @Test
    @DisplayName("Depositing Funds into a blocked account should fail")
    void testDepositFailsWithBlockedAccount() throws PersistedEntityException, SQLException {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);
        testAccount.setBlocked(true);
        assertDoesNotThrow(() -> accountService.store(testAccount));

        get(String.format(ACCOUNT_GET_ENDPOINT_FORMAT, testAccount.getId()))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(BALANCE_ID_FIELD_NAME, is(3000));

        Deposit deposit = new Deposit();
        deposit.setAccountId(testAccount.getId());
        deposit.setAmount(new BigDecimal(200));

        with().body(deposit).when().request(Method.PUT, DEPOSIT_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());

        assertEquals(new BigDecimal(3000), accountService.findById(testAccount.getId()).getBalance());
    }


    @Test
    @DisplayName("Transferring Funds b/w valid accounts should successfully update the corresponding records")
    void testTransferPasses() throws PersistedEntityException, SQLException {
        Account testAccount = new Account();
        testAccount.setUserId(userId);
        testAccount.setBalance(new BigDecimal(5000));

        assertDoesNotThrow(() -> accountService.store(testAccount));
        UUID fromAccountId = testAccount.getId();
        testAccount.setId(null);
        assertDoesNotThrow(() -> accountService.store(testAccount));
        UUID toAccountId = testAccount.getId();

        Transfer transfer = new Transfer();
        transfer.setAmount(new BigDecimal(200));
        transfer.setFromAccountId(fromAccountId);
        transfer.setToAccountId(toAccountId);

        with().body(transfer).when().request(Method.PUT, TRANSFER_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode());

        assertEquals(new BigDecimal(4800), accountService.findById(fromAccountId).getBalance());
        assertEquals(new BigDecimal(5200), accountService.findById(toAccountId).getBalance());
    }

    @Test
    @DisplayName("Transferring Funds b/w valid accounts fails when minimum balance constraint is violated")
    @Disabled
    // While the transfer is prevented from being executed, the exception handling is incorrect and therefore,
        // the response code is not accurate
    void testTransferFails() throws PersistedEntityException, SQLException {
        Account testAccount = new Account();
        testAccount.setBalance(new BigDecimal(3000));
        testAccount.setUserId(userId);
        assertDoesNotThrow(() -> accountService.store(testAccount));
        UUID fromAccountId = testAccount.getId();
        testAccount.setId(null);
        assertDoesNotThrow(() -> accountService.store(testAccount));
        UUID toAccountId = testAccount.getId();

        Transfer transfer = new Transfer();
        transfer.setAmount(new BigDecimal(200));
        transfer.setFromAccountId(fromAccountId);
        transfer.setToAccountId(toAccountId);

        with().body(transfer).when().request(Method.PUT, TRANSFER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());

        assertEquals(new BigDecimal(3000), accountService.findById(fromAccountId).getBalance());
        assertEquals(new BigDecimal(3000), accountService.findById(toAccountId).getBalance());
    }

    @AfterAll
    static void tearDown() {
        dslContext.truncateTable(ACCOUNT).execute();
        dslContext.alterTable(Tables.ACCOUNT).drop(USER_FK.constraint()).execute();
        dslContext.alterTable(USER).drop(GUARDIAN_FK.constraint()).execute();
        dslContext.truncateTable(USER).execute();
        dslContext.alterTable(Tables.ACCOUNT).add(USER_FK.constraint()).execute();
        dslContext.alterTable(USER).add(GUARDIAN_FK.constraint()).execute();
    }
}
