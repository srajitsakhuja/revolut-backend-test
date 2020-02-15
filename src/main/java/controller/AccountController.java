package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import dao.Account;
import exception.AccountException;
import exception.UserException;
import org.eclipse.jetty.http.MimeTypes;
import package_.tables.records.AccountRecord;
import service.AccountService;
import spark.Route;

import java.util.UUID;

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
        try {
            account = objectMapper.readValue(request.body(), Account.class);
            service.store(account);
        } catch (Exception e) {
            response.status(BAD_REQUEST_400);
            return "Bad Request!";
        }

        response.status(CREATED_201);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return objectMapper.writeValueAsString(account);
    };

    public Route findRoute = (request, response) ->
    {
        UUID id = UUID.fromString(request.params(":id"));
        AccountRecord record;

        try {
            record = service.findById(id);
        } catch (Exception exception) {
            response.status(NOT_FOUND_404);
            return exception.getMessage();
        }

        response.status(OK_200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return objectMapper.writeValueAsString(new Account(record));
    };
}
