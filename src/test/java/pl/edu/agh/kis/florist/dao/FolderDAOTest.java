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

public class FolderDAOTest {

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
	public void storeSingleFolder() {
		// setup:
		Folder f = new Folder("folder", "/files/", "/files/", 0, "2017.01", 1);

		// when:
		Folder folder = new FolderDAO().storeFolder(f);

		// then:
		assertNotNull(folder);
		assertThat(folder.getFolderId()).isGreaterThan(0);
		assertThat(folder).extracting(Folder::getName).containsOnly("folder");
		assertThat(folder).extracting(Folder::getPathLower).containsOnly("/files/");
		assertThat(folder).extracting(Folder::getPathDisplay).containsOnly("/files/");
		assertThat(folder).extracting(Folder::getParentFolderId).containsOnly(0);
		assertThat(folder).extracting(Folder::getServerCreatedAt).containsOnly("2017.01");
		assertThat(folder).extracting(Folder::getOwnerId).containsOnly(1);
	}

	@Test
	public void storeThreeFolders() {
		// setup:
		Folder f1 = new Folder("files", "/files", "/files", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(f1);
		Folder f2 = new Folder("folder1", "/files/folder1", "/files/folder1", storedf1.getFolderId(), "2017.01", 2);
		Folder storedf2 = new FolderDAO().storeFolder(f2);
		Folder f3 = new Folder("folder2", "/files/folder1/folder2", "/files/folder1/folder2", storedf2.getFolderId(), "2017.01", 2);
		new FolderDAO().storeFolder(f3);

		// when:
		List<Folder> folders = new FolderDAO().loadAllFolders();

		// then:
		assertNotNull(folders);
		assertThat(folders).hasSize(3);
		assertThat(folders).extracting(Folder::getName).contains("files", "folder1", "folder2");
	}

	@Test
	public void getRecursiveFoldersFromFolder() {
		// setup:
		Folder f1 = new Folder("files", "/files", "/files", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(f1);
		Folder f2 = new Folder("folder1", "/files/folder1", "/files/folder1", storedf1.getFolderId(), "2017.01", 2);
		Folder storedf2 = new FolderDAO().storeFolder(f2);
		Folder f3 = new Folder("folder2", "/files/folder1/folder2", "/files/folder1/folder2", storedf2.getFolderId(), "2017.01", 2);
		new FolderDAO().storeFolder(f3);
		
		
		// when:
		List<Folder> folders = new FolderDAO().getRecursiveFoldersFromFolder(storedf1.getFolderId());

		// then:
		assertNotNull(folders);
		assertThat(folders).hasSize(2);
		assertThat(folders).extracting(Folder::getName).contains("folder1", "folder2");
	}

	@Test
	public void getRecursiveFilesFromFolder() {
		// setup:
		Folder f1 = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(f1);
		Folder f2 = new Folder("folder1", "/f/folder1/", "/f/folder1/", storedf1.getFolderId(), "2017.01", 2);
		Folder storedf2 = new FolderDAO().storeFolder(f2);
		Folder f3 = new Folder("folder2", "/f/folder1/folder2/", "/f/folder1/folder2/", storedf2.getFolderId(), "2017.01", 2);
		Folder storedf3 = new FolderDAO().storeFolder(f3);

		String name1 = "file1";
		String name2 = "file2";
		String name3 = "file3";
		String pathLower1 = "/f/file1";
		String pathLower2 = "/f/folder1/file2";
		String pathLower3 = "/f/folder1/folder2/file3";
		String pathDisplay1 = "/f/file1";
		String pathDisplay2 = "/f/folder1/file2";
		String pathDisplay3 = "/f/folder1/folder2/file3";
		int enclosingFolderId1 = storedf1.getFolderId();
		int enclosingFolderId2 = storedf2.getFolderId();
		int enclosingFolderId3 = storedf3.getFolderId();
		int size = 10;
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 5;
		File file1 = new File(name1, pathLower1, pathDisplay1, enclosingFolderId1, size, serverCreatedAt,
				serverChangedAt, ownerId);
		File file2 = new File(name2, pathLower2, pathDisplay2, enclosingFolderId2, size, serverCreatedAt,
				serverChangedAt, ownerId);
		File file3 = new File(name3, pathLower3, pathDisplay3, enclosingFolderId3, size, serverCreatedAt,
				serverChangedAt, ownerId);
		File storedFile1 = new FileDAO().storeFile(file1);
		File storedFile2 = new FileDAO().storeFile(file2);
		File storedFile3 = new FileDAO().storeFile(file3);

		// when:
		List<File> files = new FolderDAO().getRecursiveFilesFromFolder(storedf1.getFolderId());

		// then:
		assertNotNull(files);
		assertThat(files).hasSize(3).extracting(File::getFileId)
			.contains(storedFile1.getFileId(), storedFile2.getFileId(), storedFile3.getFileId());
		assertThat(files).extracting(File::getName).contains("file1", "file2", "file3");
	}

	@Test
	public void getFolderById() {
		// setup:
		FolderDAO dao = new FolderDAO();
		Folder f1 = new Folder(1, "folder", "/files/", "/files/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(f1);
		// dao.storeFolderFolderContent(new
		// FolderFolderContent(f1.getParentFolderId(), f1.getFolderId()));

		// when:
		Folder folder = dao.getFolder(storedf1.getFolderId());

		// then:
		assertNotNull(folder);
		assertThat(folder.getFolderId()).isGreaterThan(0);
		assertThat(folder).extracting(Folder::getName).containsOnly("folder");
		assertThat(folder).extracting(Folder::getPathLower).containsOnly("/files/");
		assertThat(folder).extracting(Folder::getPathDisplay).containsOnly("/files/");
		assertThat(folder).extracting(Folder::getParentFolderId).containsOnly(0);
		assertThat(folder).extracting(Folder::getServerCreatedAt).containsOnly("2017.01");
		assertThat(folder).extracting(Folder::getOwnerId).containsOnly(1);

	}

	@Test
	public void deleteRecursiveFolder() {
		// setup:
		Folder folder1 = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);
		Folder storedf1 =new FolderDAO().storeFolder(folder1);

		Folder folder2 = new Folder(2, "f2", "/f/f2/", "/f/f2/", storedf1.getFolderId(), "2017.01", 1);
		Folder storedf2 = new FolderDAO().storeFolder(folder2);
		// new FolderDAO()
		// .storeFolderFolderContent(new
		// FolderFolderContent(folder2.getParentFolderId(),
		// folder2.getFolderId()));
		String name1 = "file1";
		String pathLower1 = "/f/f2/file1";
		String pathDisplay1 = "/f/f2/file1";
		int enclosingFolderId = storedf2.getFolderId();
		int size = 10;
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 5;
		// files
		File file1 = new File(name1, pathLower1, pathDisplay1, enclosingFolderId, size, serverCreatedAt,
				serverChangedAt, ownerId);

		new FileDAO().storeFile(file1);
		// new FileDAO().storeFolderFileContent(new
		// FolderFileContent(file1.getEnclosingFolderId(), file1.getFileId()));

		// when:
		new FolderDAO().deleteFolderWithContent(storedf2.getFolderId());
		List<File> filesResult = new FolderDAO().getRecursiveFilesFromFolder(1);

		// then:
		assertNotNull(filesResult);
		assertThat(filesResult).hasSize(0);

	}

	@Test
	public void createDirectoryTest() {
		// setup: Integer folderId, String newFolderName)

		Folder folder1 = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(folder1);
		// new FolderDAO()
		// .storeFolderFolderContent(new
		// FolderFolderContent(folder1.getParentFolderId(),
		// folder1.getFolderId()));


		String newFolderName = "newFolder";

		// when:
		Folder newFolder = new FolderDAO().createDirectoryIn(storedf1.getFolderId(), newFolderName);
		List<Folder> foldersResult = new FolderDAO().getRecursiveFoldersFromFolder(storedf1.getFolderId());

		// then:
		assertNotNull(foldersResult);
		assertNotNull(newFolder);
		assertThat(newFolder).extracting(Folder::getName).containsOnly("newFolder");
		assertThat(newFolder).extracting(Folder::getPathLower).containsOnly("/f/newfolder/");
		assertThat(newFolder).extracting(Folder::getPathDisplay).containsOnly("/f/newFolder/");
		assertThat(newFolder).extracting(Folder::getParentFolderId).containsOnly(storedf1.getFolderId());
		// assertThat(newFolder).extracting(Folder::getServerCreatedAt).containsOnly("2017.01");
		assertThat(newFolder).extracting(Folder::getOwnerId).containsOnly(1);
		assertThat(foldersResult).hasSize(1);

	}

	@Test
	public void moveFolderWithContent() {
		// setup:
		Folder f1 = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);// root
		Folder storedf1 = new FolderDAO().storeFolder(f1);
		Folder f2 = new Folder("folder2", "/f/folder2/", "/f/folder2/", storedf1.getFolderId(), "2017.01.18", 2);// move this
		Folder storedf2 = new FolderDAO().storeFolder(f2);
		Folder f3 = new Folder("folder3", "/f/folder2/folder3/", "/f/folder2/folder3/", storedf2.getFolderId(), "2017.01", 2);// with this
		Folder storedf3 = new FolderDAO().storeFolder(f3);
		Folder f4 = new Folder("Folder4", "/f/folder4/", "/f/Folder4/", storedf1.getFolderId(), "2017.01", 2);// to this folder
		Folder storedf4 = new FolderDAO().storeFolder(f4);

		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 7;
		File file1 = new File(4, "file1", "/f/folder2/file1", "/f/folder2/FILE1", storedf2.getFolderId(), 10, serverCreatedAt, serverChangedAt, ownerId);// move this
		File file2 = new File(5, "file2", "/f/folder2/folder3/file2", "/f/folder2/folder3/filE2", storedf3.getFolderId(), 12, serverCreatedAt, serverChangedAt, ownerId);// move this
		new FileDAO().storeFile(file1);
		new FileDAO().storeFile(file2);

		// when:
		Folder movedFolder = new FolderDAO().moveFolderWithContent(storedf2.getFolderId(), storedf4.getFolderId(), "newFolderName");
		List<Folder> resultFolderList = new FolderDAO().getRecursiveFoldersFromFolder(storedf4.getFolderId());
		List<File> resultFilesList = new FolderDAO().getRecursiveFilesFromFolder(storedf4.getFolderId());

		// then:
		assertNotNull(movedFolder);
		assertThat(movedFolder).extracting(Folder::getFolderId).containsOnly(storedf2.getFolderId());
		assertThat(movedFolder).extracting(Folder::getName).containsOnly("newFolderName");
		assertThat(movedFolder).extracting(Folder::getPathLower).containsOnly("/f/folder4/newfoldername/");
		assertThat(movedFolder).extracting(Folder::getPathDisplay).containsOnly("/f/Folder4/newFolderName/");
		assertThat(movedFolder).extracting(Folder::getParentFolderId).containsOnly(storedf4.getFolderId());
		assertThat(movedFolder).extracting(Folder::getServerCreatedAt).containsOnly("2017.01.18");
		assertThat(movedFolder).extracting(Folder::getOwnerId).containsOnly(2);

		assertNotNull(resultFolderList);
		assertThat(resultFolderList).hasSize(2);

		assertNotNull(resultFilesList);
		assertThat(resultFilesList).hasSize(2);

	}
	@Test
	public void renameRecursiveFoldersTest() {
		// setup:
		Folder f1 = new Folder("f", "/f/", "/f/", 0, "2017.01", 1);
		Folder storedf1 = new FolderDAO().storeFolder(f1);
		Folder f2 = new Folder("folder2", "/f/folder2/", "/f/folder2/", storedf1.getFolderId(), "2017.01.18", 2);// move this
		Folder storedf2 = new FolderDAO().storeFolder(f2);
		Folder f3 = new Folder("folder3", "/f/folder2/folder3/", "/f/folder2/folder3/", storedf2.getFolderId(), "2017.01", 2);// with this
		Folder storedf3 = new FolderDAO().storeFolder(f3);
		
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01";
		int ownerId = 7;
		File file1 = new File("file1", "/f/folder2/file1", "/f/folder2/FILE1", storedf2.getFolderId(), 10, serverCreatedAt, serverChangedAt, ownerId);// move this
		File storedfile4 = new FileDAO().storeFile(file1);
		
		// when:
		Folder renamedFolder = new FolderDAO().renameRecursiveFolder(storedf2.getFolderId(), "newFolderName", "/f/");
		Folder  folder3 = new FolderDAO().getFolder(storedf3.getFolderId());
		File file1result = new FileDAO().getFile(storedfile4.getFileId());
		
		// then:
		assertNotNull(renamedFolder);
		assertThat(renamedFolder.getFolderId()).isGreaterThan(0);
		assertThat(renamedFolder).extracting(Folder::getName).containsOnly("newFolderName");
		assertThat(renamedFolder).extracting(Folder::getPathLower).containsOnly("/f/newfoldername/");
		assertThat(renamedFolder).extracting(Folder::getPathDisplay).containsOnly("/f/newFolderName/");
		assertThat(renamedFolder).extracting(Folder::getParentFolderId).containsOnly(storedf1.getFolderId());
		assertThat(renamedFolder).extracting(Folder::getServerCreatedAt).containsOnly("2017.01.18");
		assertThat(renamedFolder).extracting(Folder::getOwnerId).containsOnly(2);

		assertNotNull(folder3);
		assertThat(folder3.getFolderId()).isGreaterThan(0);
		assertThat(folder3).extracting(Folder::getName).containsOnly("folder3");
		assertThat(folder3).extracting(Folder::getPathLower).containsOnly("/f/newfoldername/folder3/");
		assertThat(folder3).extracting(Folder::getPathDisplay).containsOnly("/f/newFolderName/folder3/");
		assertThat(folder3).extracting(Folder::getParentFolderId).containsOnly(storedf2.getFolderId());
		assertThat(folder3).extracting(Folder::getServerCreatedAt).containsOnly("2017.01");
		assertThat(folder3).extracting(Folder::getOwnerId).containsOnly(2);

		assertNotNull(file1result);
		assertThat(file1result.getFileId()).isGreaterThan(0);
		assertThat(file1result).extracting(File::getName).containsOnly("file1");
		assertThat(file1result).extracting(File::getPathLower).containsOnly("/f/newfoldername/file1");
		assertThat(file1result).extracting(File::getPathDisplay).containsOnly("/f/newFolderName/file1");
		assertThat(file1result).extracting(File::getEnclosingFolderId).containsOnly(storedf2.getFolderId());
		assertThat(file1result).extracting(File::getSize).containsOnly(10);
		assertThat(file1result).extracting(File::getServerCreatedAt).containsOnly(serverCreatedAt);
		assertThat(file1result).extracting(File::getServerChangedAt).containsOnly(serverChangedAt);
		assertThat(file1result).extracting(File::getOwnerId).containsOnly(ownerId);

	}
}
