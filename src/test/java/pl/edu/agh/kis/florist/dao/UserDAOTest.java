package pl.edu.agh.kis.florist.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static pl.edu.agh.kis.florist.db.Tables.FILE_METADATA;
import static pl.edu.agh.kis.florist.db.Tables.FOLDER_METADATA;
import static pl.edu.agh.kis.florist.db.Tables.USERS;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.edu.agh.kis.florist.model.File;
import pl.edu.agh.kis.florist.model.User;

public class UserDAOTest {

	private final String DB_URL = "jdbc:sqlite:test.db";
	private DSLContext create;
	@Before
	public void setUp() {
		create = DSL.using(DB_URL);
		// clean up all tables
		create.deleteFrom(USERS).execute();
	}

	@After
	public void tearDown() {
		create.close();
	}

	@Test
	public void storeSingleFile() {
		// setup:
		
		// when:
		User us = new UserDAO().createNewUser("name", "pass");
		new UserDAO();
		boolean passBool = UserDAO.checkPassword("pass", us.getHashedPassword());
		
		// then:
		assertNotNull(us);
		assertThat(us.getId()).isGreaterThan(0);
		assertThat(us).extracting(User::getUserName).containsOnly("name");
		assertThat(us).extracting(User::getDisplayName).containsOnly("name");
		assertThat(passBool).isTrue();
	}


}
