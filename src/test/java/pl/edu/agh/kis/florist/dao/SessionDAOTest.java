package pl.edu.agh.kis.florist.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static pl.edu.agh.kis.florist.db.Tables.USERS;
import static pl.edu.agh.kis.florist.db.Tables.SESSION_DATA;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.edu.agh.kis.florist.model.Folder;
import pl.edu.agh.kis.florist.model.Session;
import pl.edu.agh.kis.florist.model.User;

public class SessionDAOTest {
	
	private final String DB_URL = "jdbc:sqlite:test.db";
	private DSLContext create;
	@Before
	public void setUp() {
		create = DSL.using(DB_URL);
		// clean up all tables
		create.deleteFrom(USERS).execute();
		create.deleteFrom(SESSION_DATA).execute();
	}

	@After
	public void tearDown() {
		create.close();
	}

	@Test
	public void storeSession() {
		// setup:
		User u = new UserDAO().createNewUser("name", "pass");

		// when:
		Session s = new SessionDAO().createNewSession("name", "pass");

		// then:
		assertNotNull(s);
		assertThat(s.getUserId()).isEqualTo(u.getId());		
	}
	
	@Test
	public void getSessionTest() {
		// setup:
		User u = new UserDAO().createNewUser("name", "pass");
		Session rec = new SessionDAO().createNewSession("name", "pass");

		// when:
		Session s = new SessionDAO().getSession(rec.getSessionId());

		// then:
		assertNotNull(s);
		assertThat(s.getUserId()).isEqualTo(u.getId());		
	}
	
	@Test
	public void updateSessionTest() {
		// setup:
		User u = new UserDAO().createNewUser("name", "pass");
		Session rec = new SessionDAO().createNewSession("name", "pass");

		// when:
		Session s = new SessionDAO().updateSession(rec.getSessionId());
		// then:
		assertNotNull(s);
		assertThat(s.getUserId()).isEqualTo(u.getId());
		System.out.println(s.getLastAccessed()+"  "+rec.getLastAccessed());
		assertThat(s.getLastAccessed()).isNotEqualTo(rec.getLastAccessed());
	}


}
