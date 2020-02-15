package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dao.Account;
import dao.User;
import exception.AccountException;
import exception.UserException;
import org.eclipse.jetty.http.MimeTypes;
import package_.tables.records.UserRecord;
import service.UserService;
import spark.Route;

import java.util.UUID;

public class UserController {
    private UserService service;
    private ObjectMapper objectMapper;

    public UserController() {
        service = new UserService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public Route createRoute = (request, response) ->
    {
        User user;
        try {
            user = objectMapper.readValue(request.body(), User.class);
            service.store(user);
        } catch (Exception e) {
            response.status(400);
            System.out.println(e.getCause().getMessage());
            return "Bad Request!";
        }

        response.status(201);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return objectMapper.writeValueAsString(user);
    };


    public Route findRoute = (request, response) ->
    {
        UUID id = UUID.fromString(request.params(":id"));
        UserRecord record;
        String responseBody;

        try {
            record = service.findById(id);
            responseBody = objectMapper.writeValueAsString(new User(record));
        } catch (Exception exception) {
            response.status(500);
            return exception.getMessage();
        }

        response.status(200);
        response.type(MimeTypes.Type.APPLICATION_JSON.asString());
        return responseBody;
    };
}
