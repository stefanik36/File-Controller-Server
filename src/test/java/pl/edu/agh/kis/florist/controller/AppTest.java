package pl.edu.agh.kis.florist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static pl.edu.agh.kis.florist.db.Tables.USERS;
import static pl.edu.agh.kis.florist.db.Tables.SESSION_DATA;
import static pl.edu.agh.kis.florist.db.Tables.FILE_METADATA;
import static pl.edu.agh.kis.florist.db.Tables.FOLDER_METADATA;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.edu.agh.kis.florist.dao.FileDAO;
import pl.edu.agh.kis.florist.dao.FolderDAO;
import pl.edu.agh.kis.florist.model.File;
import pl.edu.agh.kis.florist.model.Folder;

public class AppTest {

	private final String DB_URL = "jdbc:sqlite:test.db";
	private DSLContext create;

	@Before
	public void setUp() {
		create = DSL.using(DB_URL);
		// clean up all tables
		create.deleteFrom(FILE_METADATA).execute();
		create.deleteFrom(FOLDER_METADATA).execute();
		create.deleteFrom(SESSION_DATA).execute();
		create.deleteFrom(USERS).execute();
	}

	@After
	public void tearDown() {
		create.close();
	}

	@Test
	public void createFewFoldersAndFiles() {
		// setup:
		String serverCreatedAt = "2017-01-09T23:11:27";
		String serverChangedAt = "2017-01-09T23:22:27";
		Folder f1 = new Folder(1, "root", "/root/", "/root/", 0, "2017.01", 1);
		Folder f2 = new Folder(2, "folder1", "/root/folder1/", "/root/folder1/", 1, "2017.01", 2);
		Folder f3 = new Folder(3, "folder2", "/root/folder1/folder2/", "/root/folder1/folder2/", 2, "2017.01", 2);
		create.newRecord(FOLDER_METADATA, f1).store();
		create.newRecord(FOLDER_METADATA, f2).store();
		create.newRecord(FOLDER_METADATA, f3).store();

		File file1 = new File(1, "file1", "/root/file1", "/root/file1", 1, 7, serverCreatedAt, serverChangedAt, 11);
		File file2 = new File(2, "file2", "/root/folder1/file2", "/root/folder1/file2", 2, 8, serverCreatedAt,
				serverChangedAt, 12);
		File file3 = new File(3, "file3", "/root/folder1/file3", "/root/folder1/file3", 2, 9, serverCreatedAt,
				serverChangedAt, 13);
		new FileDAO().storeFile(file1);
		new FileDAO().storeFile(file2);
		new FileDAO().storeFile(file3);
		
		//when:
		List<Folder> resultFolderList = new FolderDAO().getRecursiveFoldersFromFolder(1);
		List<File> resultFilesList = new FolderDAO().getRecursiveFilesFromFolder(1);

		// then:

		assertNotNull(resultFolderList);
		assertThat(resultFolderList).hasSize(2);

		assertNotNull(resultFilesList);
		assertThat(resultFilesList).hasSize(3);

	}

}
