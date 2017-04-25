package pl.edu.agh.kis.florist.dao;

import static pl.edu.agh.kis.florist.db.Tables.FILE_METADATA;
import static pl.edu.agh.kis.florist.db.Tables.FOLDER_METADATA;
import static pl.edu.agh.kis.florist.db.Tables.FILE_CONTENTS;

import java.util.ArrayList;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import pl.edu.agh.kis.florist.db.tables.records.FileContentsRecord;
import pl.edu.agh.kis.florist.db.tables.records.FileMetadataRecord;
import pl.edu.agh.kis.florist.db.tables.records.FolderMetadataRecord;
import pl.edu.agh.kis.florist.exceptions.AlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.FolderAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.InvalidParentIdException;
import pl.edu.agh.kis.florist.exceptions.PathDoesNotExistsException;
import pl.edu.agh.kis.florist.exceptions.RecordDoesNotExistsException;
import pl.edu.agh.kis.florist.model.File;
import pl.edu.agh.kis.florist.model.FileContent;
import pl.edu.agh.kis.florist.model.Folder;

public class FileDAO {

	private final String DB_URL = "jdbc:sqlite:test.db";
	// private final FolderDAO folderRepository = new FolderDAO();

	// ADDING RECORDS//
	public File storeFile(File file) {
		try (DSLContext create = DSL.using(DB_URL)) {
			FileMetadataRecord record = create.newRecord(FILE_METADATA, file);
			record.store();
			return record.into(File.class);
		}
	}

	// public FolderFileContent storeFolderFileContent(FolderFileContent ffc) {
	// try (DSLContext create = DSL.using(DB_URL)) {
	// FolderFileContentsRecord record = create.newRecord(FOLDER_FILE_CONTENTS,
	// ffc);
	// record.store();
	// return record.into(FolderFileContent.class);
	// }
	// }
	// END//

	// GETTING RECORDS//
	public List<File> getFilesFrom(Integer folderId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<File> result = create.select(FILE_METADATA.fields()).from(FILE_METADATA)
					.where(FILE_METADATA.ENCLOSING_FOLDER_ID.equal(folderId)).fetchInto(File.class);

			return result;
		}
	}

	public List<File> loadAllFiles() {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<File> files = create.select(FILE_METADATA.fields()).from(FILE_METADATA).fetchInto(File.class);

			return files;
		}
	}

	public Integer getFileId(String path) throws PathDoesNotExistsException {
		try (DSLContext create = DSL.using(DB_URL)) {
			FileMetadataRecord record = create.selectFrom(FILE_METADATA)
					.where(FILE_METADATA.PATH_LOWER.equal(path.toLowerCase())).fetchOne();// added
																							// toLowerCase
			if (record == null) {
				throw new RecordDoesNotExistsException("This path is invalis");
			}
			File f = record.into(File.class);
			return f.getFileId();
		}
	}

	public File getFile(String path) {
		return getFile(getFileId(path));
	}

	public File getFile(Integer fileId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			FileMetadataRecord record = create.selectFrom(FILE_METADATA).where(FILE_METADATA.FILE_ID.equal(fileId))
					.fetchOne();
			if (record == null) {
				throw new RecordDoesNotExistsException("There is no file with this id.");
			}
			return record.into(File.class);

		}
	}

	public void deleteAllFilesFromFolder(Integer folderId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<File> filesToDelete = create.selectFrom(FILE_METADATA)
					.where(FILE_METADATA.ENCLOSING_FOLDER_ID.equal(folderId)).fetch().into(File.class);
			for (File singleFile : filesToDelete) {
				deleteFile(singleFile.getFileId());
			}

		}
	}

	public void deleteFile(Integer fileId) {
		try (DSLContext create = DSL.using(DB_URL)) {

			int delete = create.delete(FILE_METADATA).where(FILE_METADATA.FILE_ID.equal(fileId)).execute();

			if (delete == 0) {
				throw new PathDoesNotExistsException("File does not exists.");
			}

			// int deleteFFC = create.delete(FOLDER_FILE_CONTENTS)
			// .where(FOLDER_FILE_CONTENTS.CONTAINED_FILE_ID.equal(fileId)).execute();
			//
			// if (deleteFFC == 0) {
			// throw new PathDoesNotExistsException("File does not exists.");
			// }
		}
	}

	public List<File> moveAllFilesFromFolder(Integer newFolderId, Integer oldFolderId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<File> movedFiles = new ArrayList<>();
			List<File> fileList = create.selectFrom(FILE_METADATA)
					.where(FILE_METADATA.ENCLOSING_FOLDER_ID.equal(oldFolderId)).fetchInto(File.class);
			for (File f : fileList) {
				movedFiles.add(moveFile(f.getFileId(), newFolderId, f.getName()));
			}
			return movedFiles;
		}
	}

	public void fixFilesPaths(Folder folder) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<File> fileList = create.selectFrom(FILE_METADATA)
					.where(FILE_METADATA.ENCLOSING_FOLDER_ID.equal(folder.getFolderId())).fetchInto(File.class);

			for (File currentFile : fileList) {
				fixFilePath(folder, currentFile);
			}
		}
	}

	private void fixFilePath(Folder folder, File currentFile) {
		try (DSLContext create = DSL.using(DB_URL)) {
			String pathDisplay = folder.getPathDisplay() + currentFile.getName();
			String pathLower = pathDisplay.toLowerCase();
			int result = create.update(FILE_METADATA).set(FILE_METADATA.PATH_DISPLAY, pathDisplay)
					.set(FILE_METADATA.PATH_LOWER, pathLower)
					.where(FILE_METADATA.FILE_ID.equal(currentFile.getFileId())).execute();
			if (result == 0) {
				throw new RecordDoesNotExistsException("This file does not exists.");
			}
		}
	}

	public File moveFile(Integer fileId, Integer newParentFolderId, String newFileName) {
		try (DSLContext create = DSL.using(DB_URL)) {

			FileMetadataRecord record = create.selectFrom(FILE_METADATA).where(FILE_METADATA.NAME.equal(newFileName),
					FILE_METADATA.ENCLOSING_FOLDER_ID.equal(newParentFolderId)).fetchOne();
			if (record == null) {
				Folder newParentFolder = new FolderDAO().getFolder(newParentFolderId);
				int result = create.update(FILE_METADATA).set(FILE_METADATA.NAME, newFileName)
						.set(FILE_METADATA.PATH_LOWER, newParentFolder.getPathLower() + newFileName.toLowerCase())
						.set(FILE_METADATA.PATH_DISPLAY, newParentFolder.getPathDisplay() + newFileName)
						.set(FILE_METADATA.ENCLOSING_FOLDER_ID, newParentFolderId)
						.where(FILE_METADATA.FILE_ID.equal(fileId)).execute();

				if (result == 0) {
					throw new RecordDoesNotExistsException("This file does not exists.");
				}

				return getFile(fileId);

			} else {
				throw new FolderAlreadyExistsException("File with this name already exists.");
			}

		}
	}

	public File renameFile(Integer fileId, String newName, String parentPath) {
		try (DSLContext create = DSL.using(DB_URL)) {

			int result = create.update(FILE_METADATA).set(FILE_METADATA.NAME, newName)
					.set(FILE_METADATA.PATH_DISPLAY, parentPath + newName)
					.set(FILE_METADATA.PATH_LOWER, parentPath.toLowerCase() + newName.toLowerCase())
					.where(FILE_METADATA.FILE_ID.equal(fileId)).execute();

			if (result == 0) {
				throw new RecordDoesNotExistsException("This file does not exists.");
			}
			return getFile(fileId);
		}
	}

	// END//
	public File createFileIn(Integer folderId, String fileName, int size) {
		return createFileIn(folderId, fileName, size, null);
	}

	public File createFileIn(Integer folderId, String fileName, int size, Integer userId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			FileMetadataRecord record = create.selectFrom(FILE_METADATA)
					.where(FILE_METADATA.NAME.equal(fileName), FILE_METADATA.ENCLOSING_FOLDER_ID.equal(folderId))
					.fetchOne();
			if (record == null) {
				try {
					Folder parentFolder = new FolderDAO().getFolder(folderId);
					String pathLower = parentFolder.getPathLower() + fileName.toLowerCase();
					String pathDisplay = parentFolder.getPathDisplay() + fileName;
					int enclosingFolderId = folderId;
					String serverCreatedAt = new FolderDAO().getFormatedData();
					String serverChangedAt = serverCreatedAt;
					int ownerId = userId;
					if (userId == null) {
						ownerId = parentFolder.getOwnerId();
					} else {
						ownerId = userId;
					}
					File file = new File(fileName, pathLower, pathDisplay, enclosingFolderId, size, serverCreatedAt,
							serverChangedAt, ownerId);

					return storeFile(file);
				} catch (RecordDoesNotExistsException e) {
					throw new InvalidParentIdException("Parent folder doesnt exists.");
				}
			} else {
				throw new AlreadyExistsException("File with this name already exists.");
			}
		}
	}

	public FileContent uploadFileContent(byte[] content, Integer fileId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			// FileContent fileContent = new FileContent(fileId, content);
			FileContentsRecord record = create.selectFrom(FILE_CONTENTS).where(FILE_CONTENTS.FILE_ID.equal(fileId))
					.fetchOne();
			if (record == null) {
				FileContent result = create.insertInto(FILE_CONTENTS, FILE_CONTENTS.CONTENTS, FILE_CONTENTS.FILE_ID)
						.values(content, fileId).returning(FILE_CONTENTS.FILE_ID).fetchOne().into(FileContent.class);
				return result;
			} else {
				throw new AlreadyExistsException("File with this name already exists.");
			}
		}
	}

	// DELETE IT
	// public Integer getFileId(String path) {
	// try (DSLContext create = DSL.using(DB_URL)) {
	// FileMetadataRecord record = create.selectFrom(FILE_METADATA)
	// .where(FILE_METADATA.PATH_DISPLAY.equal(path)).fetchOne();
	// File f = record.into(File.class);
	// return f.getFileId();
	// }
	// }
}
