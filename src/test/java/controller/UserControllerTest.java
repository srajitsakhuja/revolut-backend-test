package controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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
import static package_.Keys.GUARDIAN_FK;
import static package_.Keys.USER_FK;
import static package_.Tables.ACCOUNT;
import static package_.tables.User.USER;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {
    private static final List<UUID> USER_ID_LIST = new ArrayList<>();
    private static final String USER_GET_ENDPOINT_FORMAT = USER_ENDPOINT + "/%s";
    private static final String FIRST_NAME_FIELD_NAME = "firstName";
    private static final String LAST_NAME_FIELD_NAME = "lastName";
    private static final String DOB_FIELD_NAME_FORMAT = "dateOfBirth[%d]";
    private static final String ID_FIELD_NAME = "id";
    private static final String GUARDIAN_ID_FIELD_NAME = "guardianId";
    private static final LocalDate ADULT_DOB = LocalDate.of(1995, 9, 4);
    private static final LocalDate MINOR_DOB = LocalDate.of(2005, 9, 4);

    private User testUser;

    @BeforeAll
    static void beforeAllSetup() {
        ControllerTestUtil.configureControllerTest();
    }

    @BeforeEach
    void beforeEachSetup() {
        testUser = ControllerTestUtil.createDummyUser();
    }

    @Test
    @DisplayName("Adult User with a unique phone number should be created in the database")
    @Order(1)
    void testPostMethodPasses() {
        testUser.setDateOfBirth(ADULT_DOB);

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(FIRST_NAME_FIELD_NAME, is(DUMMY_FIRST_NAME))
                .body(LAST_NAME_FIELD_NAME, is(DUMMY_LAST_NAME))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 0), is(ADULT_DOB.getYear()))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 1), is(9))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 2), is(4));

        USER_ID_LIST.add(testUser.getId());
    }

    @Test
    @DisplayName("Adult User with a guardian should not be created in the database")
    @Order(2)
    void testPostMethodFailsWithGuardianForAdultUser() {
        testUser.setDateOfBirth(ADULT_DOB);
        testUser.setGuardianId(USER_ID_LIST.get(0));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with a valid guardian should be created in the database")
    @Order(3)
    void testPostMethodPassesWithValidMinorUser() {
        testUser.setDateOfBirth(MINOR_DOB);
        testUser.setGuardianId(USER_ID_LIST.get(0));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(FIRST_NAME_FIELD_NAME, is("foo"))
                .body(LAST_NAME_FIELD_NAME, is("bar"))
                .body(GUARDIAN_ID_FIELD_NAME, is(USER_ID_LIST.get(0).toString()));
        USER_ID_LIST.add(testUser.getId());
    }

    @Test
    @DisplayName("Minor User with a minor guardian should not be created in the database")
    @Order(4)
    void testPostMethodFailsWithInvalidMinorUser() {
        testUser.setDateOfBirth(MINOR_DOB);
        testUser.setGuardianId(USER_ID_LIST.get(1));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with an invalid guardian should not be created in the database")
    @Order(4)
    void testPostMethodFailsWithInvalidGuardian() {
        testUser.setDateOfBirth(MINOR_DOB);
        testUser.setGuardianId(UUID.randomUUID());

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then()
                .assertThat().statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Get with valid ID should return expected user")
    @Order(5)
    void testGetMethodPasses() {
        get(String.format(USER_GET_ENDPOINT_FORMAT, USER_ID_LIST.get(0)))
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(ID_FIELD_NAME, is(USER_ID_LIST.get(0).toString()))
                .body(FIRST_NAME_FIELD_NAME, is("foo"))
                .body(LAST_NAME_FIELD_NAME, is("bar"))
                .body(GUARDIAN_ID_FIELD_NAME, nullValue());
    }

    @Test
    @DisplayName("Get with invalid ID should fail")
    @Order(6)
    void testGetMethodFailsWithInvalidId() {
        get(String.format(USER_GET_ENDPOINT_FORMAT, UUID.randomUUID()))
                .then().assertThat()
                .statusCode(NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Get without ID parameter should return all the records")
    @Order(7)
    void testGetWithoutIdParameterPasses() {
        get(USER_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode())
                .body("$.size", is(2));
    }

    @Test
    @DisplayName("PUT with valid User Id should successfully update the record")
    @Order(8)
    void testPutPasses() {
        testUser.setId(USER_ID_LIST.get(0));
        testUser.setFirstName(DUMMY_LAST_NAME);
        testUser.setLastName(DUMMY_FIRST_NAME);
        with().body(testUser).when().request(Method.PUT, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(OK.getCode())
                .body(ID_FIELD_NAME, is(USER_ID_LIST.get(0).toString()))
                .body(FIRST_NAME_FIELD_NAME, is(DUMMY_LAST_NAME))
                .body(LAST_NAME_FIELD_NAME, is(DUMMY_FIRST_NAME))
                .body(GUARDIAN_ID_FIELD_NAME, nullValue());
    }

    @Test
    @DisplayName("PUT with invalid User Id should fail")
    @Order(9)
    void testPutFailsWithInvalidUserId() {
        testUser.setId(UUID.randomUUID());
        testUser.setFirstName(DUMMY_LAST_NAME);
        testUser.setLastName(DUMMY_FIRST_NAME);
        with().body(testUser).when().request(Method.PUT, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @AfterAll
    static void tearDown() {
        Injector injector = Guice.createInjector(new TestModule());
        DSLContext dslContext = injector.getInstance(DSLContext.class);
        dslContext.alterTable(ACCOUNT).drop(USER_FK.constraint()).execute();
        dslContext.alterTable(USER).drop(GUARDIAN_FK.constraint()).execute();
        dslContext.truncateTable(USER).execute();
        dslContext.alterTable(ACCOUNT).add(USER_FK.constraint()).execute();
        dslContext.alterTable(USER).add(GUARDIAN_FK.constraint()).execute();
    }
}
