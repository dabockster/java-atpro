package com.atproto.lexicon;

public class LexiconValidationException extends RuntimeException {
    public LexiconValidationException(String message) {
        super(message);
    }

    public LexiconValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
