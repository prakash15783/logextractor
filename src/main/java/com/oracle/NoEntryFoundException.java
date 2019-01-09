package com.oracle;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoEntryFoundException extends RuntimeException {

    public NoEntryFoundException(String message) {
        super(message);
    }
}


