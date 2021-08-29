package com.kienast.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason = "Test")

public class BusinessValidationException extends RuntimeException {
  private static final long serialVersionUID = 10L;
 
  public BusinessValidationException(String message) {
      super(message);
  }
}
