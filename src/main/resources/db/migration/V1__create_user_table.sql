create table USER(
    ID UUID not null,
    FIRST_NAME VARCHAR,
    LAST_NAME VARCHAR,
    DATE_OF_BIRTH DATE,
    PHONE_NUMBER VARCHAR,
    IS_BLOCKED BOOLEAN,
    GUARDIAN_ID UUID,
    CONSTRAINT USER_PK PRIMARY KEY (ID),
    CONSTRAINT GUARDIAN_FK FOREIGN KEY (GUARDIAN_ID) REFERENCES USER(ID),
    CONSTRAINT PHONE_NUMBER_UK UNIQUE KEY (PHONE_NUMBER)
)