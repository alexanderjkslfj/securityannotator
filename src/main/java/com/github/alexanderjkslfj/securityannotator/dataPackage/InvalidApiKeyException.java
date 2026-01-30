package com.github.alexanderjkslfj.securityannotator.dataPackage;

public class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException(String message) {
        super(message);
    }
}
