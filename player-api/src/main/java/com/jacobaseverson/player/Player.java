package com.jacobaseverson.player;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Player implements RequestHandler<Input, Output>{

    private final String firstName;
    private final String lastName;

    @JsonCreator
    public Player(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @JsonProperty
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty
    public String getLastName() {
        return lastName;
    }

    // implementing the RequestHandler interface of AWS Lambda service
    @Override
    public Output handleRequest(Input in, Context context) {
        final Output out = new Output();
        out.in = in;
        out.fullName = in.firstName + "_" + in.lastName;
        return out;
    }

}
