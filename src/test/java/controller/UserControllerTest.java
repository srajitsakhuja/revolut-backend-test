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

import static config.Application.USER_ENDPOINT;
import static io.restassured.RestAssured.*;
import static org.eclipse.jetty.http.HttpStatus.Code.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {
    private static final List<UUID> USER_ID_LIST = new ArrayList<>();
    private static final int SPARK_JAVA_DEFAULT_PORT = 4567;
    private static final String USER_GET_ENDPOINT_FORMAT = USER_ENDPOINT + "/%s";
    private static final String DUMMY_FIRST_NAME = "foo";
    private static final String DUMMY_LAST_NAME = "bar";
    private static final String FIRST_NAME_FIELD_NAME = "firstName";
    private static final String LAST_NAME_FIELD_NAME = "lastName";
    private static final String DOB_FIELD_NAME_FORMAT = "dateOfBirth[%d]";
    private static final String ID_FIELD_NAME = "id";
    private static final String GUARDIAN_ID_FIELD_NAME = "guardianId";

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
        testUser.setFirstName(DUMMY_FIRST_NAME);
        testUser.setLastName(DUMMY_LAST_NAME);
        testUser.setPhoneNumber(uniquePhoneNumber);
    }

    @Test
    @DisplayName("Adult User with a unique phone number should be created in the database")
    @Order(1)
    void testPostMethodPasses() {
        testUser.setDateOfBirth(LocalDate.of(1995, 9, 4));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(CREATED.getCode())
                .body(FIRST_NAME_FIELD_NAME, is(DUMMY_FIRST_NAME))
                .body(LAST_NAME_FIELD_NAME, is(DUMMY_LAST_NAME))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 0), is(1995))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 1), is(9))
                .body(String.format(DOB_FIELD_NAME_FORMAT, 2), is(4));

        USER_ID_LIST.add(testUser.getId());
    }

    @Test
    @DisplayName("Adult User with a guardian should not be created in the database")
    @Order(2)
    void testPostMethodFailsWithGuardianForAdultUser() {
        testUser.setDateOfBirth(LocalDate.of(1995, 9, 4));
        testUser.setGuardianId(USER_ID_LIST.get(0));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with a valid guardian should be created in the database")
    @Order(3)
    void testPostMethodPassesWithValidMinorUser() {
        testUser.setDateOfBirth(LocalDate.of(2005, 9, 4));
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
        testUser.setDateOfBirth(LocalDate.of(2005, 9, 4));
        testUser.setGuardianId(USER_ID_LIST.get(1));

        with().body(testUser).when().request(Method.POST, USER_ENDPOINT)
                .then().assertThat()
                .statusCode(BAD_REQUEST.getCode());
    }

    @Test
    @DisplayName("Minor User with an invalid guardian should not be created in the database")
    @Order(4)
    void testPostMethodFailsWithInvalidGuardian() {
        testUser.setDateOfBirth(LocalDate.of(2005, 9, 4));
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
        get(USER_ENDPOINT).then().statusCode(OK.getCode());
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

}
