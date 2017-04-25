package pl.edu.agh.kis.florist.exceptions;

import java.util.Map;

public class AuthorizationRequiredException extends RuntimeException {

	public AuthorizationRequiredException(String message) {
		super(message);
	}


}
