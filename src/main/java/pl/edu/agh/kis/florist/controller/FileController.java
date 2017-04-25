package pl.edu.agh.kis.florist.controller;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import pl.edu.agh.kis.florist.dao.FileDAO;
import pl.edu.agh.kis.florist.dao.FolderDAO;
import pl.edu.agh.kis.florist.exceptions.AuthorizationRequiredException;
import pl.edu.agh.kis.florist.exceptions.AlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.FolderAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.InvalidPathParameterException;
import pl.edu.agh.kis.florist.exceptions.PathDoesNotExistsException;
import pl.edu.agh.kis.florist.exceptions.RecordDoesNotExistsException;
import pl.edu.agh.kis.florist.model.AlreadyExistsError;
import pl.edu.agh.kis.florist.model.File;
import pl.edu.agh.kis.florist.model.FileContent;
import pl.edu.agh.kis.florist.model.Folder;
import spark.Request;
import spark.Response;

//this controller is 
public class FileController {

	private static final int CREATED = 201;
	private static final int LISTED = 200;
	private static final int DELETED = 204;

	private final FolderDAO folderDAO;
	private final FileDAO fileDAO;
	private final Gson gson = new Gson();

	public final String FILES_PATH = "/files/";

	public FileController(FolderDAO folderDAO, FileDAO fileDAO) {
		this.folderDAO = folderDAO;
		this.fileDAO = fileDAO;
	}

	public FileController() {
		this(new FolderDAO(), new FileDAO());
	}

	// GET FOLDER CONTENT//
	public Object listFolderContent(Request request, Response response) {
		boolean recursive = isRecursive(request.queryParams("recursive"));

		Object[] result = new Object[2];
		System.out.println("-------------before getting path");
		String path = getFileOrFolderPath(request.params("path"));
		System.out.println("-------------before getting folder");
		try {
			Folder f = folderDAO.getFolder(path);
			System.out.println("-------------before check access");
			checkAccesToFolderOrFile(request, f.getOwnerId());

			Integer fId = f.getFolderId();
			if (fId == null) {
				throw new PathDoesNotExistsException("The path is invalid.");
			}
			if (recursive == false) {
				System.out.println("-------------before margeFoldersAndFiles");
				result = margeFoldersAndFiles(fId);
			} else {
				result = margeFoldersAndFilesRecursive(fId);
			}
			response.status(LISTED);
			return result;
		} catch (RecordDoesNotExistsException e) {
			throw new PathDoesNotExistsException(e.getMessage());
		}

	}

	private Object[] margeFoldersAndFilesRecursive(Integer fId) {
		Object[] result = new Object[2];
		result[0] = folderDAO.getRecursiveFoldersFromFolder(fId);
		result[1] = folderDAO.getRecursiveFilesFromFolder(fId);
		return result;
	}

	private Object[] margeFoldersAndFiles(Integer fId) {
		Object[] result = new Object[2];
		System.out.println("-------------before getFoldersFrom");
		result[0] = folderDAO.getFoldersFrom(fId);
		System.out.println("-------------before getFilesFrom");
		result[1] = fileDAO.getFilesFrom(fId);
		return result;
	}
	// END//

	// GET META DATA//
	public Object getFileOrFolderMetadata(Request request, Response response) {

		try {
			String path = getFileOrFolderPath(request.params("path"));

			if (isFolder(path)) {

				System.out.println("-------bf getFolder");
				Folder f = folderDAO.getFolder(path);
				System.out.println("-------af getFolder");
				checkAccesToFolderOrFile(request, f.getOwnerId());
				System.out.println("-------af check access");
				response.status(LISTED);
				return f;
			} else {
				File f = fileDAO.getFile(fileDAO.getFileId(path));
				checkAccesToFolderOrFile(request, f.getOwnerId());
				response.status(LISTED);
				return f;
			}
		} catch (RecordDoesNotExistsException e) {
			throw new PathDoesNotExistsException("The path is invalid.");
		} catch (InvalidPathParameterException e) {
			throw new PathDoesNotExistsException("The path is invalid.");
		} catch (AuthorizationRequiredException e) {
			throw new AuthorizationRequiredException(e.getMessage());
		}
	}
	// END//

	// DELETE//
	public Object daleteFileOrRecursiveFolder(Request request, Response response) {
		try {
			String path = getFileOrFolderPath(request.params("path"));
			if (isFolder(path)) {
				Folder f = folderDAO.getFolder(path);
				checkAccesToFolderOrFile(request, f.getOwnerId());
				folderDAO.deleteFolderWithContent(f.getFolderId());
			} else {
				File f = fileDAO.getFile(path);
				checkAccesToFolderOrFile(request, f.getOwnerId());
				fileDAO.deleteFile(f.getFileId());
			}
			response.status(DELETED);
			return null;
		} catch (RecordDoesNotExistsException e) {
			throw new PathDoesNotExistsException(e.getMessage());
		} catch (InvalidPathParameterException e) {
			throw new InvalidPathParameterException(e.getMessage());
		}
	}
	// END//

	// CREATE_DIRECTORY//
	public Object createDirectory(Request request, Response response) {
		String path = getFileOrFolderPath(request.params("path"));
		String parentPath = getParentFolderPath(path);
		String newFolderName = getFileOrFolderNameFromPath(path);
		try {
			Folder parentFolder = folderDAO.getFolder(parentPath);
			checkAccesToFolderOrFile(request, parentFolder.getOwnerId());
			Integer parentFilderId = parentFolder.getFolderId();
			if (parentFilderId == null) {
				throw new PathDoesNotExistsException("The path is invalid.");
			} else {
				try {
					Folder newFolder = folderDAO.createDirectoryIn(parentFilderId, newFolderName);
					response.status(CREATED);
					return newFolder;
				} catch (FolderAlreadyExistsException e) {
					throw new AlreadyExistsException(e.getMessage());
				}
			}
		} catch (RecordDoesNotExistsException e) {
			throw new PathDoesNotExistsException(e.getMessage());
		}

	}

	private String getFileOrFolderNameFromPath(String path) {
		String[] parts = path.split("/");
		return parts[parts.length - 1];
	}

	// END//

	// MOVE//
	public Object moveFileOrRecursiveFolder(Request request, Response response) {
		String oldPath = getFileOrFolderPath(request.params("path"));
		String newPath = getFileOrFolderPath(request.queryParams("new_path"));

		String newParentPath = getParentFolderPath(newPath);
		String newName = getFileOrFolderNameFromPath(newPath);
		try {
			Folder newParentFolder = folderDAO.getFolder(newParentPath);
			checkAccesToFolderOrFile(request, newParentFolder.getOwnerId());
			if (!folderDAO.checkIfExists(newPath)) {
				if (isFolder(newPath) && isFolder(oldPath)) {
					Folder folderToMove = folderDAO.getFolder(oldPath);
					checkAccesToFolderOrFile(request, folderToMove.getOwnerId());

					Folder movedFolder = folderDAO.moveFolderWithContent(folderToMove.getFolderId(),
							newParentFolder.getFolderId(), newName);
					response.status(LISTED);
					return movedFolder;

				} else if (!isFolder(newPath) && !isFolder(oldPath)) {
					File fileToMove = fileDAO.getFile(oldPath);
					checkAccesToFolderOrFile(request, fileToMove.getOwnerId());

					File movedFile = fileDAO.moveFile(fileToMove.getFileId(), newParentFolder.getFolderId(), newName);
					response.status(LISTED);
					return movedFile;

				} else {
					throw new PathDoesNotExistsException("The path is invalid.");
				}
			} else {
				throw new AlreadyExistsException("Folder already exists");
			}
		} catch (

		RecordDoesNotExistsException ex) {
			throw new InvalidPathParameterException(ex.getMessage());
		}

	}
	// END//

	// RENAME//
	public Object renameFileOrRecursiveFolder(Request request, Response response) {
		try {
			String path = getFileOrFolderPath(request.params("path"));
			String newName = getName(request.queryParams("new_name"));
			if (isFolder(path)) {

				Folder f = folderDAO.getFolder(path);
				checkAccesToFolderOrFile(request, f.getOwnerId());
				Folder renamedFolder = folderDAO.renameRecursiveFolder(f.getFolderId(), newName,
						getParentFolderPath(path));
				response.status(LISTED);
				return renamedFolder;
			} else {
				File f = fileDAO.getFile(path);
				checkAccesToFolderOrFile(request, f.getOwnerId());
				File renamedFile = fileDAO.renameFile(f.getEnclosingFolderId(), newName, getParentFolderPath(path));
				response.status(LISTED);
				return renamedFile;
			}
		} catch (RecordDoesNotExistsException e) {
			throw new PathDoesNotExistsException(e.getMessage());
		} catch (InvalidPathParameterException e) {
			throw new InvalidPathParameterException(e.getMessage());
		}
	}
	// END//

	// UPLOAD FILE//
	public Object uploadFile(Request request, Response response) {
		try {
			String session = getSession(request.queryParams("session"));
			String content = getFileContent(request.body());
			String path = request.queryParams("path");
			Folder parentFolder = folderDAO.getFolder(getParentFolderPath(path));
			checkAccesToFolderOrFile(request, parentFolder.getOwnerId());
			byte[] bytes = content.getBytes();
			File fileMetadata = fileDAO.createFileIn(parentFolder.getFolderId(),getFileOrFolderNameFromPath(path), bytes.length);
			FileContent fileContent = fileDAO.uploadFileContent(bytes, fileMetadata.getFileId());
			return fileMetadata;
		} catch (RecordDoesNotExistsException e) {
			throw new PathDoesNotExistsException(e.getMessage());
		} catch (InvalidPathParameterException e) {
			throw new InvalidPathParameterException(e.getMessage());
		}
	}
	// END//

	private String getFileContent(String body) {
		// TODO Auto-generated method stub
		return body;
	}

	private String getSession(String queryParam) {
		String pathPattern = "[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}";
		Pattern p = Pattern.compile(pathPattern);
		Matcher m = p.matcher(queryParam);

		boolean isMatch = m.matches();
		if (!isMatch) {
			System.out.println("-----------------fail----------------getSession: " + queryParam);
			throw new InvalidPathParameterException("Mismatched pattern in session.");
		}
		return queryParam;
	}

	private String getName(String newName) {
		String pathPattern = "[a-zA-Z0-9_-]+";
		Pattern p = Pattern.compile(pathPattern);
		Matcher m = p.matcher(newName);
		System.out.println("---------------------------------------------- new name: " + newName);
		boolean isMatch = m.matches();
		if (!isMatch) {
			System.out.println("------------------fail----------------------- new name: " + newName);
			throw new InvalidPathParameterException("Mismatched pattern in path.");
		}
		return newName;
	}

	private boolean isFolder(String name) {
		if (name.charAt(name.length() - 1) == '/') {
			return true;
		}
		return false;
	}

	private String getParentFolderPath(String path) {

		String[] parts = path.split("/");
		StringBuilder parentPathBuilder = new StringBuilder("");

		for (int i = 1; i < parts.length - 1; i++) {
			parentPathBuilder.append("/");
			parentPathBuilder.append(parts[i]);
		}
		parentPathBuilder.append("/");
		String parentFolderPath = parentPathBuilder.toString();

		String pathPattern = "(/[a-zA-Z0-9_-]+)+/";
		Pattern p = Pattern.compile(pathPattern);
		Matcher m = p.matcher(parentFolderPath);

		boolean isMatch = m.matches();
		if (!isMatch) {
			System.out.println("-----------------fail----------------getParentFolderPath: " + path);
			throw new InvalidPathParameterException("Mismatched pattern in path.");
		}
		return parentFolderPath;
	}

	private String getFileOrFolderPath(String param) {

		// String param = request.params("path");
		String path;
		if (param == null) {
			path = "";// FILES_PATH;
		} else {
			path = param; // FILES_PATH + param.toLowerCase();
		}

		String pathPattern = "(/[a-zA-Z0-9_-]+)+/?";
		Pattern p = Pattern.compile(pathPattern);
		Matcher m = p.matcher(path);

		boolean isMatch = m.matches();
		if (!isMatch) {
			System.out.println("-----------------fail----------------getFileOrFolderPath: " + path);
			throw new InvalidPathParameterException("Mismatched pattern in path.");
		}
		return path;
	}

	private boolean isRecursive(String r) {
		boolean recursive;
		if (r == null) {
			recursive = false;
		} else if (r.equals("true")) {
			recursive = true;
		} else {
			recursive = false;
		}
		return recursive;
	}

	private void checkAccesToFolderOrFile(Request request, int id) {
		if (!request.attribute("ownerid").equals(id)) {
			throw new AuthorizationRequiredException("You do not own this file or folder.");
		}
	}

}
