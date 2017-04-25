package pl.edu.agh.kis.florist.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64.*;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;

import pl.edu.agh.kis.florist.dao.FolderDAO;
import pl.edu.agh.kis.florist.dao.SessionDAO;
import pl.edu.agh.kis.florist.dao.UserDAO;
import pl.edu.agh.kis.florist.exceptions.AuthorizationRequiredException;
import pl.edu.agh.kis.florist.exceptions.InvalidPathParameterException;
import pl.edu.agh.kis.florist.exceptions.RecordDoesNotExistsException;
import pl.edu.agh.kis.florist.exceptions.SessionExpiredException;
import pl.edu.agh.kis.florist.exceptions.UnsuccessfulLoginException;
import pl.edu.agh.kis.florist.exceptions.AlreadyExistsException;
import pl.edu.agh.kis.florist.model.Folder;
import pl.edu.agh.kis.florist.model.Session;
import pl.edu.agh.kis.florist.model.User;
import spark.Request;
import spark.Response;

public class UserController {

	UserDAO userDAO;
	SessionDAO sessionDAO;
	private static final int CREATED = 201;
	private static final int SETTED = 200;

	public UserController(UserDAO userDAO, SessionDAO sessionDAO) {
		this.userDAO = userDAO;
		this.sessionDAO = sessionDAO;
	}

	private final Gson gson = new Gson();

	public final String USERS_PATH = "/users/";

	public Object createUser(Request request, Response response) {
		try {
			String userName = getName(request.body());
			String userPass = getPass(request.body());
			User u = userDAO.createNewUser(userName, userPass);
			new FolderDAO().createDirectoryIn(1, u.getUserName(), u.getId());
			response.status(CREATED);
			return u;
		} catch (InvalidPathParameterException e) {
			throw new InvalidPathParameterException(e.getMessage());
		} catch (AlreadyExistsException e) {
			throw new AlreadyExistsException(e.getMessage());
		}

	}

	private String getPass(String body) {
		String[] parts = checkBodyPattern(body);
		String[] passParts = parts[1].split("=");
		String passPattern = "[a-zA-Z0-9_-]+";
		boolean isMatch = Pattern.compile(passPattern).matcher(passParts[1]).matches();
		if (!isMatch) {
			System.out.println("-----------------fail pass----------------getPass pass: " + passParts[1]);
			throw new InvalidPathParameterException("Mismatched pattern in path.");
		}
		return passParts[1];
	}

	private String[] checkBodyPattern(String body) {
		String bodyPattern = "user_name=([a-zA-Z0-9_-]+)&user_pass=([a-zA-Z0-9_-]+)";

		if (!Pattern.compile(bodyPattern).matcher(body).matches()) {
			System.out.println("--------------body pattern--------------- body: " + body);
			throw new InvalidPathParameterException("Mismatched pattern in path.");
		}

		String[] parts = body.split("&");
		return parts;
	}

	private String getName(String body) {
		String[] parts = checkBodyPattern(body);
		String[] nameParts = parts[0].split("=");
		String namePattern = "[a-zA-Z0-9_-]+";
		boolean isMatch = Pattern.compile(namePattern).matcher(nameParts[1]).matches();
		if (!isMatch) {
			System.out.println("-----------------fail name----------------getName name: " + nameParts[1]);
			throw new InvalidPathParameterException("Mismatched pattern in path.");
		}
		return nameParts[1];
	}

	public Object checkAccess(Request request, Response response) {
		try {
			String sessionId = request.cookie("sessionid");
			System.out.println("---------check access-------sessionid:: " + sessionId);
			try {
				sessionDAO.getSession(sessionId);
			} catch (RecordDoesNotExistsException e) {
				throw new UnsuccessfulLoginException("Unsuccessful login.");
			}
			return provideAccess(request, response);

		} catch (RecordDoesNotExistsException e) {
			throw new InvalidPathParameterException(e.getMessage());
		} catch (SessionExpiredException e) {
			return createSession(request, response);
		}

	}

	public Object getAccess(Request request, Response response) {
		try {
			String sessionId = request.cookie("sessionid");
			System.out.println("-------------------------------------catched session: " + sessionId);

			try {
				sessionDAO.getSession(sessionId);
			} catch (RecordDoesNotExistsException e) {
				System.out.println("----------------catched--------------e: " + e);
				try {
					return createSession(request, response);
				} catch (InvalidPathParameterException ex) {
					System.out.println("------------------------------------------------------create session1");
					throw new InvalidPathParameterException(e.getMessage());
				}
			}
			System.out.println("--------------------------------------provide access " + sessionId);
			return provideAccess(request, response);
		} catch (RecordDoesNotExistsException e) {
			throw new InvalidPathParameterException(e.getMessage());
		} catch (AuthorizationRequiredException e) {
			throw new AuthorizationRequiredException(e.getMessage());
		} catch (SessionExpiredException e) {
			try {
				return createSession(request, response);
			} catch (InvalidPathParameterException ex) {
				System.out.println("------------------------------------------------------create session2");
				throw new InvalidPathParameterException(e.getMessage());
			}
		}
	}

	private Object provideAccess(Request request, Response response) {
		Session session = sessionDAO.updateSession(getSessionId(request.cookie("sessionid")));
		response.cookie("sessionid", session.getSessionId());
		request.attribute("ownerid", session.getUserId());
		response.status(SETTED);
		return session;
	}

	private Session createSession(Request request, Response response) {
		try {
			String authorization = request.headers("authorization");
			System.out.println("---------------------------authorization------------------: " + authorization);
			if (authorization == null) {
				throw new AuthorizationRequiredException("Authorization requitred.");
			}
			String[] parts = splitToNameAndPass(request.headers("authorization"));
			System.out.println("parts: " + parts);
			if (parts.length != 2) {
				throw new InvalidPathParameterException("Authorization header invalid.");
			}
			String name = getNameOrPasswordAuthorization(parts[0]);
			String pass = getNameOrPasswordAuthorization(parts[1]);
			System.out.println("name: " + name + " pass: " + pass);
			Session session = sessionDAO.createNewSession(name, pass);
			response.cookie("sessionid", session.getSessionId());
			response.status(SETTED);
			return session;
		} catch (InvalidPathParameterException e) {
			throw new InvalidPathParameterException(e.getMessage());
		}
	}

	private String getNameOrPasswordAuthorization(String nameOrPass) {

		String namePattern = "[a-zA-Z0-9_-]+";

		boolean isMatch = Pattern.compile(namePattern).matcher(nameOrPass).matches();
		if (!isMatch) {
			System.out.println("-----------------fail name----------------getNameAuthorization name: " + nameOrPass);
			throw new InvalidPathParameterException("Mismatched pattern in path.");
		}
		return nameOrPass;
	}

	private String[] splitToNameAndPass(String authorizationString) {
		try {
			System.out.println("before decoding");
			String decoded = encodeAndGetAuthorization(authorizationString);
			System.out.println("decoded: " + decoded);
			String[] parts = decoded.split(":");
			System.out.println("splited: " + parts[0] + " " + parts[1]);
			return parts;
		} catch (InvalidPathParameterException e) {
			System.out.println("--------------------invalid parameter throwed");
			throw new InvalidPathParameterException(e.getMessage());
		}
	}

	private String encodeAndGetAuthorization(String authorizationString) {
		if (authorizationString == null) {
			throw new InvalidPathParameterException("Path does not exists.");
		}

		try {
			System.out.println("1authorizationString: " + authorizationString);
			String[] encodedParts = authorizationString.split(" ");

			if (encodedParts.length != 2) {
				throw new InvalidPathParameterException("Mismatched pattern in authorization.");
			}

			System.out.println("2encodedParts[0]: " + encodedParts[0] + " 2encodedParts[1]: " + encodedParts[1]);

			byte[] decodedBytes = Base64.decodeBase64(encodedParts[1].getBytes());
			System.out.println("3decodedBytes " + new String(decodedBytes));

			String decoded = new String(decodedBytes);
			System.out.println("4decoded: " + decoded);
			String bodyPattern = "([a-zA-Z0-9_-]+):([a-zA-Z0-9_-]+)";

			if (!Pattern.compile(bodyPattern).matcher(decoded).matches()) {
				System.out.println("------fail 1---getPassAuthorization decoded to string: " + decoded.toString());
				throw new InvalidPathParameterException("Mismatched pattern in path.");
			}
			return decoded;
		} catch (IllegalArgumentException e) {
			throw new InvalidPathParameterException(e.getMessage());
		}
	}

	private String getSessionId(String sessionId) {
		String sessionIdPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

		if (!Pattern.compile(sessionIdPattern).matcher(sessionId).matches()) {
			System.out.println("-------fail sess----------------getSessionIdAuthorization sessionId: " + sessionId);
			throw new InvalidPathParameterException("Mismatched pattern in sessionId.");
		}

		return sessionId;
	}

}
