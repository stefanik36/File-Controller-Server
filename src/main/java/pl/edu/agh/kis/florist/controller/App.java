package pl.edu.agh.kis.florist.controller;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.stop;
import static spark.Spark.delete;
import static spark.Spark.put;
import static spark.Spark.secure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import pl.edu.agh.kis.florist.dao.AuthorDAO;
import pl.edu.agh.kis.florist.dao.BookDAO;
import pl.edu.agh.kis.florist.dao.FileDAO;
import pl.edu.agh.kis.florist.dao.FolderDAO;
import pl.edu.agh.kis.florist.dao.SessionDAO;
import pl.edu.agh.kis.florist.dao.UserDAO;
import pl.edu.agh.kis.florist.exceptions.PathDoesNotExistsException;
import pl.edu.agh.kis.florist.exceptions.UnsuccessfulLoginException;
import pl.edu.agh.kis.florist.exceptions.AlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.AuthorizationRequiredException;
import pl.edu.agh.kis.florist.exceptions.InvalidPathParameterException;
import pl.edu.agh.kis.florist.exceptions.ParameterFormatException;
import pl.edu.agh.kis.florist.model.AuthorizationRequiredError;
import pl.edu.agh.kis.florist.model.InvalidParameterError;
import pl.edu.agh.kis.florist.model.ParameterFormatError;
import pl.edu.agh.kis.florist.model.PathDoesNotExistsError;
import pl.edu.agh.kis.florist.model.UnsuccessfulLoginError;
import pl.edu.agh.kis.florist.model.AlreadyExistsError;
import spark.Request;
import spark.ResponseTransformer;

public class App {

	final static private Logger LOGGER = LoggerFactory.getILoggerFactory().getLogger("requests");

	public static void main(String[] args) {

		final String AUTHORS_PATH = "/authors";
		final String BOOKS_PATH = "/books";
		final String BOOK_PATH = "/books/:bookid";

		// ADDED//
		final String FILE_PATH = "/files";
		final String USERS_PATH = "/users";

		// list_folder_content
		final String LIST_FOLDER_CONTENT_CMD = "/list_folder_content";
		final String LIST_FOLDER_CONTENT = FILE_PATH + "/:path" + LIST_FOLDER_CONTENT_CMD;
		// final String LIST_FOLDER_CONTENT_ROOT = ROOT_PATH +
		// LIST_FOLDER_CONTENT_CMD;

		// get_meta_data
		final String GET_META_DATA_CMD = "/get_meta_data";
		final String GET_META_DATA = FILE_PATH + "/:path" + GET_META_DATA_CMD;
		// final String GET_META_DATA_ROOT = ROOT_PATH + GET_META_DATA_CMD;

		// delete
		final String DELETE_CMD = "/delete";
		final String DELETE = FILE_PATH + "/:path" + DELETE_CMD;

		// create_directory
		final String CREATE_DIRECTORY_CMD = "/create_directory";
		final String CREATE_DIRECTORY = FILE_PATH + "/:path" + CREATE_DIRECTORY_CMD;

		// move file or directory
		final String MOVE_CMD = "/move";
		final String MOVE = FILE_PATH + "/:path" + MOVE_CMD;

		// rename file or directory
		final String RENAME_CMD = "/rename";
		final String RENAME = FILE_PATH + "/:path" + RENAME_CMD;

		// upload file
		final String UPLOAD_CMD = "/upload";
		final String UPLOAD = FILE_PATH + UPLOAD_CMD;

		// upload file
		final String DOWNLOAD_CMD = "/download";
		final String DOWNLOAD = FILE_PATH + DOWNLOAD_CMD;

		// create user
		final String CREATE_USER_CMD = "/create_user";
		final String CREATE_USER = USERS_PATH + CREATE_USER_CMD;

		// access
		final String ACCESS_CMD = "/access";
		final String ACCESS = USERS_PATH + ACCESS_CMD;

		// responses
		final int USER_EXISTS = 400;
		final int AUTHORYZATION_REQUIRED = 401;
		final int UNSUCCESSFUL_LOGIN = 403;
		final int PATH_DOESNT_EXISTS = 404;
		final int MISMATCHED_PATTERN = 405;
		// END//

		final Gson gson = new Gson();
		final ResponseTransformer json = gson::toJson;

		final BookController bookController = new BookController(new AuthorDAO(), new BookDAO());
		final FileController fileController = new FileController(new FolderDAO(), new FileDAO());
		final UserController userController = new UserController(new UserDAO(), new SessionDAO());

		// Changes port on which server listens
		port(4567);

		// ssl connection
		String keyStoreLocation = "deploy/keystore.jks";
		String keyStorePassword = "password";
		secure(keyStoreLocation, keyStorePassword, null, null);

		// registers filter before processing of any request with special
		// metothod stated below
		// this method is run to log request with logger
		// but similar method can be used to check user authorisation
		before("/*/", (req, res) -> {
			info(req);
		});

		before("/files/*", (req, res) -> {
			System.out.println("-------------------------DEFAULT ACCESS---------------------------------req: " + req);
			System.out.println("-----------------------------------------------------------------resp: " + res);
			userController.checkAccess(req, res);
		});

		// registers HTTP GET on resource /atuthors
		// and delegates processing into BookController
		get(AUTHORS_PATH, (request, response) -> {
			return bookController.handleAllAuthors(request, response);
		}, json);

		// registers HTTP POST on resource /atuthors
		// and delegates processing into BookController
		post(AUTHORS_PATH, (request, response) -> {
			return bookController.handleCreateNewAuthor(request, response);
		}, json);

		// registers HTTP GET on resource /books
		// and delegates processing into BookController
		get(BOOKS_PATH, (request, response) -> {
			return bookController.hadleAllBooks(request, response);
		}, json);

		// registers HTTP GET on resource /books/{bookId}
		// and delegates processing into BookController
		get(BOOK_PATH, (request, response) -> {
			return bookController.handleSingleBook(request, response);
		}, json);

		// handleSingleBook can throw ParameterFromatException which will be
		// processed
		// gracefully instead of 500 Internal Server Error
		exception(ParameterFormatException.class, (ex, request, response) -> {
			response.status(403);
			response.body(gson.toJson(new ParameterFormatError(request.params())));
		});

		// ADDED//

		// list folders and files from subfolders
		get(LIST_FOLDER_CONTENT, (request, response) -> {
			System.out.println("-----------------------------LIST-------------------------------req: " + request);
			System.out.println("-----------------------------------------------------------------resp: " + response);
			return fileController.listFolderContent(request, response);
		}, json);

		// shows metadata of file or folder
		get(GET_META_DATA, (request, response) -> {
			System.out.println("------------------------------GET_META--------------------------------req: " + request);
			System.out.println("---------------------------------------------------------------resp: " + response);
			return fileController.getFileOrFolderMetadata(request, response);
		}, json);

		// deletes file or directory
		delete(DELETE, (request, response) -> {
			System.out.println("-------------------------------DELETE------------------------------req: " + request);
			System.out.println("--------------------------------------------------------------resp: " + response);
			return fileController.daleteFileOrRecursiveFolder(request, response);
		}, json);

		// creates directory
		put(CREATE_DIRECTORY, (request, response) -> {
			System.out.println("---------------------------CREATE--------------------------------req: " + request);
			System.out.println("---------------------------------------------------------resp: " + response);
			return fileController.createDirectory(request, response);
		}, json);

		// move file or directory
		put(MOVE, (request, response) -> {
			System.out.println("---------------------------MOVE---------------------------------req: " + request);
			System.out.println("-----------------------------------------------------------------resp: " + response);
			return fileController.moveFileOrRecursiveFolder(request, response);
		}, json);

		// rename file or directory
		put(RENAME, (request, response) -> {
			System.out.println("-------------------------RENAME---------------------------------req: " + request);
			System.out.println("-----------------------------------------------------------------resp: " + response);
			return fileController.renameFileOrRecursiveFolder(request, response);
		}, json);
		
		// upload file
		post(UPLOAD, (request, response) -> {
			System.out.println("-------------------------UPLOAD---------------------------------req: " + request);
			System.out.println("-----------------------------------------------------------------resp: " + response);
			return fileController.uploadFile(request, response);
		}, json);
		
		// create user
		post(CREATE_USER, (request, response) -> {
			System.out.println("-------------------------CREATE USER---------------------------------req: " + request);
			System.out.println("-----------------------------------------------------------------resp: " + response);
			return userController.createUser(request, response);
		}, json);

		// access
		get(ACCESS, (request, response) -> {
			System.out.println("-------------------------ACCESS---------------------------------req: " + request);
			System.out.println("-----------------------------------------------------------------resp: " + response);
			return userController.getAccess(request, response);
		}, json);

		// throws 400 already exists
		exception(AlreadyExistsException.class, (ex, request, response) -> {
			System.out.println("------------------------400------------------------------------req: " + request);
			System.out.println("------------------------------------------------------------resp: " + response);
			response.status(USER_EXISTS);
			response.body(gson.toJson(new AlreadyExistsError(request.params())));
		});

		// throws 401 Authorization required
		exception(AuthorizationRequiredException.class, (ex, request, response) -> {
			System.out.println("------------------------401------------------------------------req: " + request);
			System.out.println("------------------------------------------------------------resp: " + response);
			response.status(AUTHORYZATION_REQUIRED);
			response.body(gson.toJson(new AuthorizationRequiredError(request.params())));
		});

		// throws 403 unsuccessful login
		exception(UnsuccessfulLoginException.class, (ex, request, response) -> {
			System.out.println("------------------------403------------------------------------req: " + request);
			System.out.println("------------------------------------------------------------resp: " + response);
			response.status(UNSUCCESSFUL_LOGIN);
			response.body(gson.toJson(new UnsuccessfulLoginError(request.params())));
		});

		// throws 404 not found
		exception(PathDoesNotExistsException.class, (ex, request, response) -> {
			System.out.println("------------------------404------------------------------------req: " + request);
			System.out.println("------------------------------------------------------------resp: " + response);
			response.status(PATH_DOESNT_EXISTS);
			response.body(gson.toJson(new PathDoesNotExistsError(request.params())));
		});

		// throws 405 Method Not Allowed
		exception(InvalidPathParameterException.class, (ex, request, response) -> {
			System.out.println("------------------------405------------------------------------req: " + request);
			System.out.println("---------------------------------------------------------------resp: " + response);
			response.status(MISMATCHED_PATTERN);
			response.body(gson.toJson(new InvalidParameterError(request.params())));
		});

		// DELETE IT!!!
		get("/stop", (request, response) -> {
			stop();
			return null;
		}, json);

		// END//

	}

	private static void info(Request req) {
		LOGGER.info("{}", req);
	}

}
