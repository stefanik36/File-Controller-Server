package pl.edu.agh.kis.florist.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static pl.edu.agh.kis.florist.db.Tables.FILE_METADATA;
import static pl.edu.agh.kis.florist.db.Tables.FOLDER_METADATA;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.edu.agh.kis.florist.model.File;
import pl.edu.agh.kis.florist.model.Folder;

public class FileDAOTest {

	private final String DB_URL = "jdbc:sqlite:test.db";
	private DSLContext create;

	@Before
	public void setUp() {
		create = DSL.using(DB_URL);
		// clean up all tables
		create.deleteFrom(FILE_METADATA).execute();
		create.deleteFrom(FOLDER_METADATA).execute();
	}

	@After
	public void tearDown() {
		create.close();
	}

	@Test
	public void storeSingleFile() {
		// setup:
		String name = "file1";
		String pathLower = "/files/file1";
		String pathDisplay = "/files/file1";
		int enclosingFolderId = 1;
		int size = 10;
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 5;
		File f = new File(name, pathLower, pathDisplay, enclosingFolderId, size, serverCreatedAt, serverChangedAt,
				ownerId);

		// when:
		File file = new FileDAO().storeFile(f);

		// then:
		assertNotNull(file);
		assertThat(file.getFileId()).isGreaterThan(0);
		assertThat(file).extracting(File::getName).containsOnly(name);
		assertThat(file).extracting(File::getPathLower).containsOnly(pathLower);
		assertThat(file).extracting(File::getPathDisplay).containsOnly(pathDisplay);
		assertThat(file).extracting(File::getEnclosingFolderId).containsOnly(enclosingFolderId);
		assertThat(file).extracting(File::getSize).containsOnly(size);
		assertThat(file).extracting(File::getServerCreatedAt).containsOnly(serverCreatedAt);
		assertThat(file).extracting(File::getServerChangedAt).containsOnly(serverChangedAt);
		assertThat(file).extracting(File::getOwnerId).containsOnly(ownerId);
	}

	// @Test
	// public void storeFolderFileContent() {
	// // setup:
	// int parentFolderId = 4;
	// int containedFileId = 5;
	// FolderFileContent ffc = new FolderFileContent(parentFolderId,
	// containedFileId);
	//
	// // when:
	// FolderFileContent ffcResult = new FileDAO().storeFolderFileContent(ffc);
	//
	// // then:
	// assertNotNull(ffcResult);
	// assertThat(ffcResult.getParentFolderId()).isEqualTo(parentFolderId);
	// assertThat(ffcResult.getContainedFileId()).isEqualTo(containedFileId);
	//
	// }

	@Test
	public void getFilesFromFolderWithId() {
		// setup:
		// folder
		Folder folder = new Folder(1, "files", "/files", "/files", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(folder);
		// new FolderDAO()
		// .storeFolderFolderContent(new
		// FolderFolderContent(folder.getParentFolderId(),
		// folder.getFolderId()));
		String name1 = "file1";
		String name2 = "file2";
		String pathLower1 = "/files/file1";
		String pathLower2 = "/files/file2";
		String pathDisplay1 = "/files/file1";
		String pathDisplay2 = "/files/file2";
		int enclosingFolderId = storedf1.getFolderId();
		int size = 10;
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 5;
		// files
		File file1 = new File(name1, pathLower1, pathDisplay1, enclosingFolderId, size, serverCreatedAt,
				serverChangedAt, ownerId);
		File file2 = new File(name2, pathLower2, pathDisplay2, enclosingFolderId, size, serverCreatedAt,
				serverChangedAt, ownerId);

		new FileDAO().storeFile(file1);
		new FileDAO().storeFile(file2);

		// when:
		List<File> filesResult = new FileDAO().getFilesFrom(enclosingFolderId);

		// then:
		assertNotNull(filesResult);
		assertThat(filesResult).hasSize(2);
		assertThat(filesResult).extracting(File::getName).contains("file1", "file2");

	}

	@Test
	public void getFileById() {
		// setup:
		String name1 = "file1";
		String pathLower1 = "/files/file1";
		String pathDisplay1 = "/files/file1";
		int enclosingFolderId = 1;
		int size = 10;
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 5;
		File file1 = new File(name1, pathLower1, pathDisplay1, enclosingFolderId, size, serverCreatedAt,
				serverChangedAt, ownerId);
		FileDAO dao = new FileDAO();
		File storedf1 = dao.storeFile(file1);
		File file = dao.getFile(storedf1.getFileId());

		// then:
		assertNotNull(file);
		assertThat(file).extracting(File::getFileId).containsOnly(storedf1.getFileId());
		assertThat(file).extracting(File::getName).containsOnly(name1);
		assertThat(file).extracting(File::getPathLower).containsOnly(pathLower1);
		assertThat(file).extracting(File::getPathDisplay).containsOnly(pathDisplay1);
		assertThat(file).extracting(File::getEnclosingFolderId).containsOnly(enclosingFolderId);
		assertThat(file).extracting(File::getSize).containsOnly(size);
		assertThat(file).extracting(File::getServerCreatedAt).containsOnly(serverCreatedAt);
		assertThat(file).extracting(File::getServerChangedAt).containsOnly(serverChangedAt);
		assertThat(file).extracting(File::getOwnerId).containsOnly(ownerId);

	}

	@Test // (expected = PathDoesNotExistsException.class)
	public void deleteFile() {
		// setup:
		Folder folder = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(folder);
		String name1 = "file1";
		String pathLower1 = "/f/file1";
		String pathDisplay1 = "/f/file1";
		int enclosingFolderId = storedf1.getFolderId();
		int size = 10;
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 5;
		// files
		File file1 = new File(name1, pathLower1, pathDisplay1, enclosingFolderId, size, serverCreatedAt,
				serverChangedAt, ownerId);

		File storedFile1 = new FileDAO().storeFile(file1);
	
		// when:
		new FileDAO().deleteFile(storedFile1.getFileId());
		List<File> filesResult = new FileDAO().getFilesFrom(enclosingFolderId);

		// then:
		assertNotNull(filesResult);
		assertThat(filesResult).hasSize(0);

	}

	@Test 
	public void moveFile() {
		// setup:
		Folder folder1 = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(folder1);
		Folder folder2 = new Folder(2, "f2", "/f/f2/", "/f/f2/", 1, "2017.01", 1);
		Folder storedf2 = new FolderDAO().storeFolder(folder2);

		String name1 = "file1";
		String pathLower1 = "/f/file1";
		String pathDisplay1 = "/f/file1";
		int enclosingFolderId = storedf1.getFolderId();
		int size = 10;
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 5;
		File file1 = new File(name1, pathLower1, pathDisplay1, enclosingFolderId, size, serverCreatedAt,
				serverChangedAt, ownerId);
		File storedfile1 = new FileDAO().storeFile(file1);

		// when:
		File newFile = new FileDAO().moveFile(storedfile1.getFileId(), storedf2.getFolderId(), "newFile");

		// then:
		assertNotNull(newFile);
		assertThat(newFile).extracting(File::getFileId).containsOnly(storedfile1.getFileId());
		assertThat(newFile).extracting(File::getName).containsOnly("newFile");
		assertThat(newFile).extracting(File::getPathLower).containsOnly("/f/f2/newfile");
		assertThat(newFile).extracting(File::getPathDisplay).containsOnly("/f/f2/newFile");
		assertThat(newFile).extracting(File::getEnclosingFolderId).containsOnly(storedf2.getFolderId());
		assertThat(newFile).extracting(File::getSize).containsOnly(size);
		assertThat(newFile).extracting(File::getServerCreatedAt).containsOnly(serverCreatedAt);
		assertThat(newFile).extracting(File::getServerChangedAt).containsOnly(serverChangedAt);
		assertThat(newFile).extracting(File::getOwnerId).containsOnly(ownerId);

	}

	@Test
	public void moveAllFilesFromFolderTest() {
		// setup:
		Folder folder1 = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(folder1);
		Folder folder2 = new Folder("f2", "/f/f2/", "/f/f2/", storedf1.getFolderId(), "2017.01", 1);
		Folder storedf2 = new FolderDAO().storeFolder(folder2);
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 7;
		File file1 = new File("file1", "/f/file1", "/f/FILE1", storedf1.getFolderId(), 10, serverCreatedAt, serverChangedAt, ownerId);
		new FileDAO().storeFile(file1);
		File file2 = new File("file2", "/f/file2", "/f/filE2", storedf1.getFolderId(), 12, serverCreatedAt, serverChangedAt, ownerId);
		new FileDAO().storeFile(file2);

		// when:
		List<File> movedFiles = new FileDAO().moveAllFilesFromFolder(storedf2.getFolderId(), storedf1.getFolderId());
		List<File> result = new FileDAO().getFilesFrom(storedf2.getFolderId());

		// then:
		assertNotNull(movedFiles);
		assertThat(movedFiles).hasSize(2);
		assertNotNull(result);
		assertThat(result).hasSize(2);

	}

	@Test
	public void renameFileTest() {
		// setup:
		Folder folder1 = new Folder("F", "/F/", "/f/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(folder1);
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 7;
		File file1 = new File("file1", "/f/file1", "/F/FILE1", storedf1.getFolderId(), 10, serverCreatedAt, serverChangedAt, ownerId);
		File storedFile1 = new FileDAO().storeFile(file1);

		// when:
		File newNamed = new FileDAO().renameFile(storedFile1.getFileId(), "newName", "/F/");

		// then:
		assertNotNull(newNamed);
		assertThat(newNamed).extracting(File::getFileId).containsOnly(storedFile1.getFileId());
		assertThat(newNamed).extracting(File::getName).containsOnly("newName");
		assertThat(newNamed).extracting(File::getPathLower).containsOnly("/f/newname");
		assertThat(newNamed).extracting(File::getPathDisplay).containsOnly("/F/newName");
		assertThat(newNamed).extracting(File::getEnclosingFolderId).containsOnly(storedf1.getFolderId());
		assertThat(newNamed).extracting(File::getSize).containsOnly(10);
		assertThat(newNamed).extracting(File::getServerCreatedAt).containsOnly(serverCreatedAt);
		assertThat(newNamed).extracting(File::getServerChangedAt).containsOnly(serverChangedAt);
		assertThat(newNamed).extracting(File::getOwnerId).containsOnly(ownerId);

	}
}
