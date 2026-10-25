package com.godoy.corebanking.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s %s não existe".formatted(resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}