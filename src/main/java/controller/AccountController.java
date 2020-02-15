package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dao.Account;
import exception.AccountException;
import exception.UserException;
import org.eclipse.jetty.http.MimeTypes;
import package_.tables.records.AccountRecord;
import service.AccountService;
import spark.Route;

import java.util.UUID;

public class AccountController {
    private AccountService service;
    private ObjectMapper objectMapper;

    public AccountController() {
        service = new AccountService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public Route createRoute = (request, response) ->
    {
        Account account;
        try {
            account = objectMapper.readValue(request.body(), Account.class);
            service.store(account);
        } catch (AccountException exception) {
            response.status(500);
            return exception.getMessage();
        } catch (Exception e) {
            response.status(400);
            return "Bad Request!";
        }

        response.status(201);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return objectMapper.writeValueAsString(account);
    };

    public Route findRoute = (request, response) ->
    {
        UUID id = UUID.fromString(request.params(":id"));
        AccountRecord record;

        try {
            record = service.findById(id);
        } catch (UserException exception) {
            response.status(500);
            return exception.getMessage();
        }

        response.status(200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return objectMapper.writeValueAsString(new Account(record));
    };
}
