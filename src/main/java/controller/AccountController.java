package controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import dao.Account;
import exception.PersistedEntityException;
import org.eclipse.jetty.http.MimeTypes;
import package_.tables.records.AccountRecord;
import service.AccountService;
import spark.Route;

import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.jetty.http.HttpStatus.*;

public class AccountController {
    private AccountService service;
    private ObjectMapper objectMapper;

    @Inject
    public AccountController(AccountService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    public Route createRoute = (request, response) ->
    {
        Account account;
        String responseBody;

        try {
            account = objectMapper.readValue(request.body(), Account.class);
            service.store(account);
            responseBody = objectMapper.writeValueAsString(account);
        } catch (JsonProcessingException e) {
            response.status(BAD_REQUEST_400);
            return "Error in parsing input!";
        } catch (PersistedEntityException e) {
            response.status(BAD_REQUEST_400);
            return e.getMessage();
        } catch (SQLException e) {
            response.status(INTERNAL_SERVER_ERROR_500);
            return "An internal server error occurred";
        }

        response.status(CREATED_201);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return responseBody;
    };

    public Route updateRoute = (request, response) ->
    {
        Account account;
        String responseBody;
        try {
            account = objectMapper.readValue(request.body(), Account.class);
            service.update(account);
            account = new Account(service.findById(account.getId()));
            responseBody = objectMapper.writeValueAsString(account);
        } catch (JsonProcessingException e) {
            response.status(BAD_REQUEST_400);
            return "Error in parsing input!";
        } catch (PersistedEntityException e) {
            response.status(BAD_REQUEST_400);
            return e.getMessage();
        } catch (SQLException e) {
            response.status(INTERNAL_SERVER_ERROR_500);
            return "An internal server error occurred";
        }

        response.status(OK_200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return responseBody;
    };

    public Route findRoute = (request, response) ->
    {
        UUID id = UUID.fromString(request.params(":id"));
        AccountRecord record;
        String responseBody;

        try {
            record = service.findById(id);
            responseBody = objectMapper.writeValueAsString(new Account(record));
        } catch (PersistedEntityException exception) {
            response.status(NOT_FOUND_404);
            return exception.getMessage();
        } catch (SQLException | JsonProcessingException e) {
            response.status(INTERNAL_SERVER_ERROR_500);
            return "An internal server error occurred";
        }

        response.status(OK_200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return responseBody;
    };

    public Route findAllRoute = (request, response) ->
    {
        Collection<AccountRecord> records;
        String responseBody;

        try {
            records = service.find();
            responseBody = objectMapper.writeValueAsString(records.stream().map(Account::new).collect(Collectors.toList()));
        } catch (PersistedEntityException exception) {
            response.status(NOT_FOUND_404);
            return exception.getMessage();
        } catch (SQLException | JsonProcessingException e) {
            response.status(INTERNAL_SERVER_ERROR_500);
            return "An internal server error occurred";
        }

        response.status(OK_200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return responseBody;
    };
}
