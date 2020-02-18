# Revolut Backend Test

## Introduction

This project is a collection of RESTful APIs that can be used for performing several online banking activities.
Some examples of this would include:
1. Creating, Retrieving, Updating Users
2. Creating, Retrieving, Updating Accounts for those Users
3. Depositing Funds into Accounts
4. Transferring Funds from one Account to another

While the first two would be relevant to administrative banking activities, the 3rd and the 4th APIs would be invoked by
systems such as ATM and PoS machines on behalf of end-users.

## Tech Stack

The Tech stack for this project includes:
1. Flyway: used as DDL for writing migration (misnomer) scripts to create the User and Account tables and for defining database constraints.
2. JOOQ: used for mapping the database to generated Java classes which are then used for performing database (CRUD) operations.
3. Google Guice: used for injecting and retrieving dependencies from the context.
4. Spark Java: used as the Web Application Framework
5. Rest Assured: used for testing the REST endpoints.

## Package-Structure
The project has been delineated into the following packages to achieve separation of concerns:
1. main/java/config: contains the providers and module(s) for providing and binding dependencies. It also contains the
Application.java that specifies the routing for the application.
2. main/java/controller: contains the Controllers that parse and subsequently, pass on the HTTP requests to the service-layer and process the
results to configure the HTTP responses.
3. main/java/dao: contains POJOs that map directly to the request bodies and to the Record objects generated by JOOQ. These are
essentially the DTOs (Data Transfer Objects) that are received as request bodies and returned as response bodies by the
Controllers.
4. main/java/exception
5. main/java/service: contains the business logic as well as the boilerplate code to perform CRUD operations.
6. test/java/controller: contains rest-assured tests for testing the REST endpoints.

## Plugins
The project configures the following maven plugins:
1. flyway-maven-plugin: executed in the generate-sources phase using the goals 'clean' and 'migrate' which remove the 
existing schema and execute the migration scripts respectively.
2. jooq-codegen-maven: creates java mappings of the database in the form of records, constraints etc. 
and makes them accessible on the classpath.
3. exec-maven-plugin: executes the application.
4. maven-surefire-plugin: ensures that the tests are run during the maven test lifecycle-phase.

## Running the Application
The following command line statement can be executed to run the application on localhost:4567/
```
mvn clean compile exec:java
```
NOTE: Any existing files in the target/ folder are deleted, the 'generate-sources' lifecycle phase is invoked causing 
the flyway-maven-plugin and the jooq-codegen-maven to be executed and the exec-maven-plugin executes the application.

```
mvn clean test exec:java
```
could be used to also execute the tests before running the application.

## Known issues
1. The update queries should get fired up for only the non-null values in the request body (json).
2. Test ordering should be avoid in the UserControllerTest to achieve test isolation.
3. A bug has been highlighted in AccountControllerTest#testTransferFails (disabled).
4. Exceptions are not logged.
5. Db connection parameters are hard-coded. They should be injected as properties.
6. The code should be checked for check-style, pmd violations.
7. Transfers/Deposits from and to 'blocked' accounts/users should be prohibited.
8. Transfers/Deposits should be persisted in the Db.
9. There should be an endpoint for fetching all the accounts for a userId.  


## References
1. http://sparkjava.com/tutorials/application-structure
2. https://www.jooq.org/doc/latest/manual/sql-execution/crud-with-updatablerecords/optimistic-locking/
3. https://www.jooq.org/doc/3.12/manual/sql-building/sql-statements/insert-statement/insert-values/
4. https://www.jooq.org/doc/3.13/manual/sql-execution/fetching/many-fetching/
5. https://www.jooq.org/doc/latest/manual/sql-execution/transaction-management/
6. https://www.mojohaus.org/exec-maven-plugin/usage.html




