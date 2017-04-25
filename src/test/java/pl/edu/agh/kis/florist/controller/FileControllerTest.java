package pl.edu.agh.kis.florist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static pl.edu.agh.kis.florist.db.Tables.FILE_METADATA;
import static pl.edu.agh.kis.florist.db.Tables.FOLDER_METADATA;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import pl.edu.agh.kis.florist.dao.FolderDAO;
import pl.edu.agh.kis.florist.dao.FileDAO;
import pl.edu.agh.kis.florist.exceptions.PathDoesNotExistsException;
import pl.edu.agh.kis.florist.model.File;
import pl.edu.agh.kis.florist.model.Folder;
import spark.Request;
import spark.Response;

public class FileControllerTest {

	private final String DB_URL = "jdbc:sqlite:test.db";
	private DSLContext create;

	@Before
	public void setUp() {
		create = DSL.using(DB_URL);
		create.deleteFrom(FILE_METADATA).execute();
		create.deleteFrom(FOLDER_METADATA).execute();
	}

	@After
	public void tearDown() {
		create.close();
	}

	@Test
	public void getFileOrFolderMetadataTest() {
		// setup:		
		Request req = mock(Request.class);
		Response res = mock(Response.class, withSettings().stubOnly());
		FolderDAO mockedFolderDAO = mock(FolderDAO.class);
		FileController fileControler = new FileController(mockedFolderDAO, new FileDAO());

		// when:
		when(req.params("path")).thenReturn("/folder/");
		when(req.attribute("ownerid")).thenReturn(7);

		when(mockedFolderDAO.getFolder(any(String.class)))
				.thenReturn(new Folder(18, "folder", "/folder/", "/folder/", 0, "2017", 7));

		Folder folderTested = (Folder) fileControler.getFileOrFolderMetadata(req, res);

		// then:
		assertNotNull(folderTested);
		assertThat(folderTested).extracting(Folder::getFolderId).contains(18);
		assertThat(folderTested).extracting(Folder::getName).contains("folder");
		assertThat(folderTested).extracting(Folder::getPathLower).contains("/folder/");
		assertThat(folderTested).extracting(Folder::getPathDisplay).contains("/folder/");
		assertThat(folderTested).extracting(Folder::getParentFolderId).contains(0);
		assertThat(folderTested).extracting(Folder::getServerCreatedAt).contains("2017");
		assertThat(folderTested).extracting(Folder::getOwnerId).contains(7);

	}

}
