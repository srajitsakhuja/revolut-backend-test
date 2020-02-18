package controller;

import java.time.LocalDate;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dao.User;
import io.restassured.http.Method;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.UserService;

import static config.RoutingConfiguration.USER_ENDPOINT;
import static controller.ControllerTestUtil.DUMMY_FIRST_NAME;
import static controller.ControllerTestUtil.DUMMY_LAST_NAME;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.with;
import static org.eclipse.jetty.http.HttpStatus.Code.BAD_REQUEST;
import static org.eclipse.jetty.http.HttpStatus.Code.CREATED;
import static org.eclipse.jetty.http.HttpStatus.Code.NOT_FOUND;
import static org.eclipse.jetty.http.HttpStatus.Code.OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static package_.Keys.GUARDIAN_FK;
import static package_.Keys.USER_FK;
import static package_.Tables.ACCOUNT;
import static package_.tables.User.USER;

public class UserControllerTest {
    private static final String USER_GET_ENDPOINT_FORMAT = USER_ENDPOINT + "/%s";
    private static final String FIRST_NAME_FIELD_NAME = "firstName";
    private static final String LAST_NAME_FIELD_NAME = "lastName";
    private static final String DOB_FIELD_NAME_FORMAT = "dateOfBirth[%d]";
    private static final String ID_FIELD_NAME = "id";
    private static final String GUARDIAN_ID_FIELD_NAME = "guardianId";
    private static final LocalDate ADULT_DOB = LocalDate.of(1995, 9, 4);
    private static final LocalDate MINOR_DOB = LocalDate.of(2005, 9, 4);

    private static DSLContext dslContext;
    private static UserService userService;

    @BeforeAll
    static void beforeAllSetup() {
        ControllerTestUtil.configureControllerTest();

        Injector injector = Guice.createInjector(new TestModule());
        dslContext = injector.getInstance(DSLContext.class);
        userService = injector.getInstance(UserService.class);
    }

    @BeforeEach
    void beforeEachSetup() {
        truncateUserTable();
    }

    @Test
    @DisplayName("Adult User with a unique phone number should be created in the database")
    void testPostMethodPasses() {
        User testUser = ControllerTestUtil.createDummyUser();
        testUser.setDateOfBirth(ADULT_DOB);

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(FIRST_NAME_FIELD_NAME, is(DUMMY_FIRST_NAME))
                .body(LAST_NAME_FIELD_NAME, is(DUMMY_LAST_NAME))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 0), is(ADULT_DOB.getYear()))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 1), is(9))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 2), is(4));
    }

    @Test
    @DisplayName("Adult User with a guardian should not be created in the database")
    void testPostMethodFailsWithGuardianForAdultUser() {
        User guardianUser = ControllerTestUtil.createDummyUser();
        guardianUser.setDateOfBirth(ADULT_DOB);
        assertDoesNotThrow(() -> userService.store(guardianUser));
        UUID guardianId = guardianUser.getId();

        User adultUser = ControllerTestUtil.createDummyUser();
        adultUser.setGuardianId(guardianId);

        with().body(adultUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with a valid guardian should be created in the database")
    void testPostMethodPassesWithValidMinorUser() {
        User guardianUser = ControllerTestUtil.createDummyUser();
        guardianUser.setDateOfBirth(ADULT_DOB);
        assertDoesNotThrow(() -> userService.store(guardianUser));
        UUID guardianId = guardianUser.getId();

        User minorUser = ControllerTestUtil.createDummyUser();
        minorUser.setDateOfBirth(MINOR_DOB);
        minorUser.setGuardianId(guardianId);

        with().body(minorUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(FIRST_NAME_FIELD_NAME, is("foo"))
                .body(LAST_NAME_FIELD_NAME, is("bar"))
                .body(GUARDIAN_ID_FIELD_NAME, is(guardianId.toString()));
    }

    @Test
    @DisplayName("Minor User with a minor guardian should not be created in the database")
    void testPostMethodFailsWithInvalidMinorUser() {
        User adultGuardianUser = ControllerTestUtil.createDummyUser();
        adultGuardianUser.setDateOfBirth(ADULT_DOB);
        assertDoesNotThrow(() -> userService.store(adultGuardianUser));
        UUID adultGuardianId = adultGuardianUser.getId();

        User minorGuardianUser = ControllerTestUtil.createDummyUser();
        minorGuardianUser.setDateOfBirth(MINOR_DOB);
        minorGuardianUser.setGuardianId(adultGuardianId);
        assertDoesNotThrow(() -> userService.store(minorGuardianUser));
        UUID minorGuardianId = minorGuardianUser.getId();

        User minorUser = ControllerTestUtil.createDummyUser();
        minorUser.setDateOfBirth(MINOR_DOB);
        minorUser.setGuardianId(minorGuardianId);

        with().body(minorUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with an invalid guardian should not be created in the database")
    void testPostMethodFailsWithInvalidGuardian() {
        User testUser = ControllerTestUtil.createDummyUser();
        testUser.setDateOfBirth(MINOR_DOB);
        testUser.setGuardianId(UUID.randomUUID());

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then()
                .assertThat().statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Get with valid ID should return expected user")
    void testGetMethodPasses() {
        User testUser = ControllerTestUtil.createDummyUser();
        testUser.setDateOfBirth(ADULT_DOB);
        assertDoesNotThrow(() -> userService.store(testUser));

        get(String.format(USER_GET_ENDPOINT_FORMAT, testUser.getId()))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(ID_FIELD_NAME, is(testUser.getId().toString()))
                .body(FIRST_NAME_FIELD_NAME, is(testUser.getFirstName()))
                .body(LAST_NAME_FIELD_NAME, is(testUser.getLastName()))
                .body(GUARDIAN_ID_FIELD_NAME, nullValue());
    }

    @Test
    @DisplayName("Get with invalid ID should fail")
    void testGetMethodFailsWithInvalidId() {
        get(String.format(USER_GET_ENDPOINT_FORMAT, UUID.randomUUID()))
                .then().assertThat()
                .statusCode(NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Get without ID parameter should return all the records")
    void testGetWithoutIdParameterPasses() {
        get(USER_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode())
                .body("$.size", is(0));
    }

    @Test
    @DisplayName("PUT with valid User Id should successfully update the record")
    void testPutPasses() {
        User testUser = ControllerTestUtil.createDummyUser();
        testUser.setDateOfBirth(ADULT_DOB);
        assertDoesNotThrow(() -> userService.store(testUser));

        String updatedFirstName = "updatedFirstName";
        String updatedLastName = "updatedLastName";
        testUser.setFirstName(updatedFirstName);
        testUser.setLastName(updatedLastName);
        with().body(testUser).when().request(Method.PUT, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(ID_FIELD_NAME, is(testUser.getId().toString()))
                .body(FIRST_NAME_FIELD_NAME, is(updatedFirstName))
                .body(LAST_NAME_FIELD_NAME, is(updatedLastName))
                .body(GUARDIAN_ID_FIELD_NAME, nullValue());
    }

    @Test
    @DisplayName("PUT with invalid User Id should fail")
    void testPutFailsWithInvalidUserId() {
        User testUser = ControllerTestUtil.createDummyUser();
        testUser.setId(UUID.randomUUID());
        testUser.setFirstName("updatedFirstName");

        with().body(testUser).when().request(Method.PUT, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @AfterAll
    static void truncateUserTable() {
        dslContext.alterTable(ACCOUNT).drop(USER_FK.constraint()).execute();
        dslContext.alterTable(USER).drop(GUARDIAN_FK.constraint()).execute();
        dslContext.truncateTable(USER).execute();
        dslContext.alterTable(ACCOUNT).add(USER_FK.constraint()).execute();
        dslContext.alterTable(USER).add(GUARDIAN_FK.constraint()).execute();
    }
}
