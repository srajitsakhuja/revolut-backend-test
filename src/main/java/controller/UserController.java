package controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import dao.User;
import exception.PersistedEntityException;
import org.eclipse.jetty.http.MimeTypes;
import package_.tables.records.UserRecord;
import service.UserService;
import spark.Route;

import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.jetty.http.HttpStatus.*;

public class UserController {
    private UserService service;
    private ObjectMapper objectMapper;

    @Inject
    public UserController(UserService service, ObjectMapper mapper) {
        this.service = service;
        objectMapper = mapper;
    }

    public Route createRoute = (request, response) ->
    {
        User user;
        try {
            user = objectMapper.readValue(request.body(), User.class);
            service.store(user);
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
        return objectMapper.writeValueAsString(user);
    };

    public Route updateRoute = (request, response) ->
    {
        User user;
        String responseBody;
        try {
            user = objectMapper.readValue(request.body(), User.class);
            service.update(user);
            user = new User(service.findById(user.getId()));
            responseBody = objectMapper.writeValueAsString(user);
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
        UserRecord record;
        String responseBody;

        try {
            record = service.findById(id);
            responseBody = objectMapper.writeValueAsString(new User(record));
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
        Collection<UserRecord> records;
        String responseBody;

        try {
            records = service.find();
            responseBody = objectMapper.writeValueAsString(records.stream().map(User::new).collect(Collectors.toList()));
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
