package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import dao.Account;
import org.eclipse.jetty.http.MimeTypes;
import package_.tables.records.AccountRecord;
import service.AccountService;
import spark.Route;

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

    public Route findAllRoute = (request, response) ->
    {
        Collection<AccountRecord> records;
        String responseBody;

        try {
            records = service.find();
            responseBody = objectMapper.writeValueAsString(records.stream().map(Account::new).collect(Collectors.toList()));
        } catch (Exception exception) {
            response.status(NOT_FOUND_404);
            return exception.getMessage();
        }

        response.status(OK_200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return responseBody;
    };

    public Route updateRoute = (request, response) ->
    {
        Account account;
        try {
            account = objectMapper.readValue(request.body(), Account.class);
            service.update(account);
            account = new Account(service.findById(account.getId()));
        } catch (Exception e) {
            response.status(BAD_REQUEST_400);
            return BAD_REQUEST_400;
        }


        response.status(OK_200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return objectMapper.writeValueAsString(account);
    };
}
