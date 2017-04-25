package pl.edu.agh.kis.florist.dao;

import static pl.edu.agh.kis.florist.db.Tables.FOLDER_METADATA;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;

import pl.edu.agh.kis.florist.db.tables.records.FolderMetadataRecord;
import pl.edu.agh.kis.florist.exceptions.AlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.FolderAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.InvalidParentIdException;
import pl.edu.agh.kis.florist.exceptions.PathDoesNotExistsException;
import pl.edu.agh.kis.florist.exceptions.RecordDoesNotExistsException;
import pl.edu.agh.kis.florist.model.File;
import pl.edu.agh.kis.florist.model.Folder;

public class FolderDAO {

	private final String DB_URL = "jdbc:sqlite:test.db";
	private final FileDAO fileRepository = new FileDAO();

	// ADDING RECORDS//
	public Folder storeFolder(Folder folder) {
		try (DSLContext create = DSL.using(DB_URL)) {
			FolderMetadataRecord record = create.newRecord(FOLDER_METADATA, folder);
			record.store();
			return record.into(Folder.class);
		}
	}

	// public FolderFolderContent storeFolderFolderContent(FolderFolderContent
	// ffc) {
	// try (DSLContext create = DSL.using(DB_URL)) {
	// FolderFolderContentsRecord record =
	// create.newRecord(FOLDER_FOLDER_CONTENTS, ffc);
	// record.store();
	// return record.into(FolderFolderContent.class);
	// }
	// }
	// END//

	// GETTING RECORDS//
	public List<Folder> loadAllFolders() {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<Folder> folders = create.select(FOLDER_METADATA.fields()).from(FOLDER_METADATA)
					.fetchInto(Folder.class);
			return folders;
		}
	}

	public Integer getFolderId(String path) {
		try (DSLContext create = DSL.using(DB_URL)) {
			System.out.println("before to lower case ");
			String pathLower = path.toLowerCase();
			System.out.println("before record ");
			Result<FolderMetadataRecord> record = create.selectFrom(FOLDER_METADATA).where(FOLDER_METADATA.PATH_LOWER.equal(pathLower)).fetch();
			System.out.println("record: " + record);
			if (record == null) {
				System.out.println("record does not exists");
				throw new RecordDoesNotExistsException("There is no folder with this path.");
			}
			if(record.size()<1){
				throw new RecordDoesNotExistsException("There is no folder with this path.");
			}
			System.out.println("bef into");
			Folder f = record.get(0).into(Folder.class);
			System.out.println("af into");
			return f.getFolderId();
		}
	}

	public Folder getFolder(String path) {
		try {
			System.out.println("bef get folder id");
			Integer id = getFolderId(path);
			System.out.println("bef get folder");
			return getFolder(id);
		} catch (RecordDoesNotExistsException e) {
			throw new RecordDoesNotExistsException(e.getMessage());
		}
	}

	public List<Folder> getFoldersFrom(Integer fId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<Folder> result = create.select(FOLDER_METADATA.fields()).from(FOLDER_METADATA)
					.where(FOLDER_METADATA.PARENT_FOLDER_ID.equal(fId)).fetchInto(Folder.class);
			return result;
		}
	}

	public int getParentFolderId(Integer folderId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			FolderMetadataRecord record = create.selectFrom(FOLDER_METADATA)
					.where(FOLDER_METADATA.FOLDER_ID.equal(folderId)).fetchOne();
			Folder f = record.into(Folder.class);
			return f.getParentFolderId();
		}
	}

	public Folder getFolder(Integer folderId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			FolderMetadataRecord record = create.selectFrom(FOLDER_METADATA)
					.where(FOLDER_METADATA.FOLDER_ID.equal(folderId)).fetchOne();
			if (record == null) {
				throw new RecordDoesNotExistsException("There is no folder with this id.");
			}
			return record.into(Folder.class);

		}
	}

	public List<Folder> getRecursiveFoldersFromFolder(Integer folderId) {

		List<Folder> rFolders = new ArrayList<Folder>();
		getRcursiveFolders(folderId, rFolders);
		return rFolders;

	}

	private void getRcursiveFolders(Integer fId, List<Folder> rFolders) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<Folder> ffc = create.selectFrom(FOLDER_METADATA).where(FOLDER_METADATA.PARENT_FOLDER_ID.equal(fId))
					.fetchInto(Folder.class);
			for (Folder folder : ffc) {
				int fid = folder.getFolderId();
				Folder newFolder = create.selectFrom(FOLDER_METADATA).where(FOLDER_METADATA.FOLDER_ID.equal(fid))
						.fetchOne().into(Folder.class);
				rFolders.add(newFolder);
				getRcursiveFolders(fid, rFolders);

			}
		}
	}

	public List<File> getRecursiveFilesFromFolder(Integer folderId) {
		List<File> rFiles = new ArrayList<File>();
		getRcursiveFiles(folderId, rFiles);
		rFiles.addAll(fileRepository.getFilesFrom(folderId));
		return rFiles;
	}

	private void getRcursiveFiles(Integer fId, List<File> rFiles) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<Folder> ffc = create.selectFrom(FOLDER_METADATA).where(FOLDER_METADATA.PARENT_FOLDER_ID.equal(fId))
					.fetchInto(Folder.class);
			for (Folder folder : ffc) {
				int fid = folder.getFolderId();
				getRcursiveFiles(fid, rFiles);

				rFiles.addAll(fileRepository.getFilesFrom(fid));
			}

		}
	}

	public void deleteFolderWithContent(Integer id) {
		deleteRcursiveFolders(id);
	}

	private void deleteRcursiveFolders(Integer fId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<Folder> fFolderc = create.selectFrom(FOLDER_METADATA)
					.where(FOLDER_METADATA.PARENT_FOLDER_ID.equal(fId)).fetchInto(Folder.class);

			for (Folder folder : fFolderc) {
				int folderId = folder.getFolderId();
				deleteRcursiveFolders(folderId);
				fileRepository.deleteAllFilesFromFolder(folderId);

			}
			// System.out.println("------------------------------fol----------------------------bef:
			// "+getRecursiveFoldersFromFolder(0));
			deleteFolder(fId);
			// System.out.println("---------------------------------fol-------------------------aft:
			// "+getRecursiveFoldersFromFolder(0));
		}
	}

	private void deleteFolder(Integer folderId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			int delete = create.delete(FOLDER_METADATA).where(FOLDER_METADATA.FOLDER_ID.equal(folderId)).execute();

			if (delete == 0) {
				throw new PathDoesNotExistsException("Folder does not exists.");
			}
		}
	}

	public Folder moveFolderWithContent(Integer oldFolderId, Integer newParentFolderId, String newName) {
		return moveRcursiveFolders(oldFolderId, newParentFolderId, newName);
	}

	private Folder moveRcursiveFolders(Integer oldFolderId, Integer newParentFolderId, String newName) {
		try (DSLContext create = DSL.using(DB_URL)) {

			List<Folder> folderList = create.selectFrom(FOLDER_METADATA)
					.where(FOLDER_METADATA.PARENT_FOLDER_ID.equal(oldFolderId)).fetchInto(Folder.class);
			Folder movedFolder = moveFolder(oldFolderId, newParentFolderId, newName);
			System.out.println("----------------------------------bef: " + getRecursiveFilesFromFolder(4));
			fileRepository.moveAllFilesFromFolder(newParentFolderId, oldFolderId);
			System.out.println("-------------------------------------------aft: " + getRecursiveFilesFromFolder(4));

			int currentFolderId;
			for (Folder currentFolder : folderList) {
				currentFolderId = currentFolder.getFolderId();
				moveRcursiveFolders(currentFolderId, movedFolder.getFolderId(), currentFolder.getName());
			}
			return movedFolder;
		}
	}

	private Folder moveFolder(int folderId, Integer newParentFolderId, String newName) {
		try (DSLContext create = DSL.using(DB_URL)) {

			FolderMetadataRecord record = create.selectFrom(FOLDER_METADATA)
					.where(FOLDER_METADATA.PATH_LOWER.equal(getFolder(newParentFolderId).getPathLower()),
							FOLDER_METADATA.PARENT_FOLDER_ID.equal(newParentFolderId))
					.fetchOne();
			if (record == null) {
				Folder newParentFolder = getFolder(newParentFolderId);
				int result = create.update(FOLDER_METADATA).set(FOLDER_METADATA.NAME, newName)
						.set(FOLDER_METADATA.PATH_LOWER, newParentFolder.getPathLower() + newName.toLowerCase() + "/")
						.set(FOLDER_METADATA.PATH_DISPLAY, newParentFolder.getPathDisplay() + newName + "/")
						.set(FOLDER_METADATA.PARENT_FOLDER_ID, newParentFolderId)
						.where(FOLDER_METADATA.FOLDER_ID.equal(folderId)).execute();

				if (result == 0) {
					throw new RecordDoesNotExistsException("This file does not exists.");
				}

				return getFolder(folderId);
			} else {
				throw new FolderAlreadyExistsException("File with this name already exists.");
			}
		}

	}

	public Folder renameRecursiveFolder(Integer folderId, String newName, String parentFolderPath) {

		try (DSLContext create = DSL.using(DB_URL)) {

			String pathDisplay = parentFolderPath + newName + "/";
			String pathLower = parentFolderPath.toLowerCase() + newName.toLowerCase() + "/";

			int result = create.update(FOLDER_METADATA).set(FOLDER_METADATA.NAME, newName)
					.set(FOLDER_METADATA.PATH_DISPLAY, pathDisplay).set(FOLDER_METADATA.PATH_LOWER, pathLower)
					.where(FOLDER_METADATA.FOLDER_ID.equal(folderId)).execute();

			if (result == 0) {
				throw new RecordDoesNotExistsException("This file does not exists.");
			}
			fileRepository.fixFilesPaths(getFolder(folderId));
			fixFoldersPaths(getFolder(folderId));

			return getFolder(folderId);
		}
	}

	private void fixFoldersPaths(Folder folder) {
		try (DSLContext create = DSL.using(DB_URL)) {
			List<Folder> folderList = create.selectFrom(FOLDER_METADATA)
					.where(FOLDER_METADATA.PARENT_FOLDER_ID.equal(folder.getFolderId())).fetchInto(Folder.class);

			for (Folder currentFolder : folderList) {
				fixFolderPath(folder, currentFolder);
				fileRepository.fixFilesPaths(currentFolder);
				fixFoldersPaths(currentFolder);
			}
		}
	}

	private void fixFolderPath(Folder folder, Folder currentFolder) {
		try (DSLContext create = DSL.using(DB_URL)) {
			String pathDisplay = folder.getPathDisplay() + currentFolder.getName() + "/";
			String pathLower = pathDisplay.toLowerCase();
			int result = create.update(FOLDER_METADATA).set(FOLDER_METADATA.PATH_DISPLAY, pathDisplay)
					.set(FOLDER_METADATA.PATH_LOWER, pathLower)
					.where(FOLDER_METADATA.FOLDER_ID.equal(currentFolder.getFolderId())).execute();
			if (result == 0) {
				throw new RecordDoesNotExistsException("This file does not exists.");
			}
		}
	}

	public Folder createDirectoryIn(Integer parentFId, String newFolderName) {
		return createDirectoryIn(parentFId, newFolderName, null);
	}

	public Folder createDirectoryIn(Integer parentFId, String newFolderName, Integer userId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			FolderMetadataRecord record = create.selectFrom(FOLDER_METADATA)
					.where(FOLDER_METADATA.NAME.equal(newFolderName), FOLDER_METADATA.PARENT_FOLDER_ID.equal(parentFId))
					.fetchOne();
			if (record == null) {
				try {
					Folder parentFolder = getFolder(parentFId);
					String folderName = newFolderName;
					String folderPathLower = parentFolder.getPathLower() + newFolderName.toLowerCase() + "/";
					String folderPathDisplay = parentFolder.getPathDisplay() + newFolderName + "/";
					int parentFolderId = parentFId;
					String serverCreatedAt = getFormatedData();
					int ownerId;
					if (userId == null) {
						ownerId = parentFolder.getOwnerId();
					} else {
						ownerId = userId;
					}

					Folder result = create
							.insertInto(FOLDER_METADATA, FOLDER_METADATA.NAME, FOLDER_METADATA.PATH_LOWER,
									FOLDER_METADATA.PATH_DISPLAY, FOLDER_METADATA.PARENT_FOLDER_ID,
									FOLDER_METADATA.SERVER_CREATED_AT, FOLDER_METADATA.OWNER_ID)
							.values(folderName, folderPathLower, folderPathDisplay, parentFolderId, serverCreatedAt,
									ownerId)
							.returning(FOLDER_METADATA.FOLDER_ID).fetchOne().into(Folder.class);

					return getFolder(result.getFolderId());
				} catch (RecordDoesNotExistsException e) {
					throw new InvalidParentIdException("Parent folder doesnt exists.");
				}
			} else {
				throw new AlreadyExistsException("Folder with this name already exists.");
			}
		}
	}

	public String getFormatedData() {
		String ymd = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
		String serverCreatedAt = ymd + "T" + time + "Z";
		return serverCreatedAt;
	}

	public boolean checkIfExists(String path) {
		try (DSLContext create = DSL.using(DB_URL)) {
			String pathLower = path.toLowerCase();
			Result<FolderMetadataRecord> record = create.selectFrom(FOLDER_METADATA).where(FOLDER_METADATA.PATH_LOWER.equal(pathLower)).fetch();
			if (record == null) {
				return false;
			}
			if (record.isEmpty()) {
				return false;
			}
			return true;
		}
	}

	// END//

}
