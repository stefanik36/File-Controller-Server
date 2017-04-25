package pl.edu.agh.kis.florist.dao;

import static pl.edu.agh.kis.florist.db.Tables.SESSION_DATA;

import java.sql.Timestamp;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import pl.edu.agh.kis.florist.db.tables.records.SessionDataRecord;
import pl.edu.agh.kis.florist.exceptions.RecordDoesNotExistsException;
import pl.edu.agh.kis.florist.exceptions.SessionExpiredException;
import pl.edu.agh.kis.florist.model.Session;
import pl.edu.agh.kis.florist.model.User;

public class SessionDAO {

	private final String DB_URL = "jdbc:sqlite:test.db";
	private final UserDAO userDAO = new UserDAO();

	public Session createNewSession(String name, String pass) {
		try (DSLContext create = DSL.using(DB_URL)) {
			User u = userDAO.getUser(name, pass);
			deleteUserSessions(u.getId());

			String sessionId = UUID.randomUUID().toString();
			Timestamp lastAccessed = new Timestamp(System.currentTimeMillis());
			int result = create
					.insertInto(SESSION_DATA, SESSION_DATA.USER_ID, SESSION_DATA.SESSION_ID, SESSION_DATA.LAST_ACCESSED)
					.values(u.getId(), sessionId, lastAccessed).execute();
			if (result == 0) {
				throw new RecordDoesNotExistsException("Cannot create session.");
			}
			SessionDataRecord record = create.selectFrom(SESSION_DATA)
					.where(SESSION_DATA.SESSION_ID.equal(sessionId), SESSION_DATA.LAST_ACCESSED.equal(lastAccessed))
					.fetchOne();
			if (record == null) {
				throw new RecordDoesNotExistsException("Session is not created.");
			}
			return record.into(Session.class);

		}
	}

	private void deleteUserSessions(Integer uid) {
		try (DSLContext create = DSL.using(DB_URL)) {

			create.delete(SESSION_DATA).where(SESSION_DATA.USER_ID.equal(uid)).execute();

		}
	}

	public Session updateSession(String sessionId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			try {
				Session session = getSession(sessionId);
				Timestamp time = new Timestamp(System.currentTimeMillis());
				if ((session.getLastAccessed().getTime() + 1000 * 60) > time.getTime()) {
					int result = create.update(SESSION_DATA).set(SESSION_DATA.LAST_ACCESSED, time)
							.where(SESSION_DATA.SESSION_ID.equal(sessionId)).execute();

					if (result == 0) {
						throw new RecordDoesNotExistsException("This file does not exists.");
					}
					return getSession(sessionId);
				} else {
					throw new SessionExpiredException("Session expired.");
				}
			} catch (RecordDoesNotExistsException e) {
				throw new RecordDoesNotExistsException(e.getMessage());
			}

		}
	}

	public Session getSession(String sessionId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			System.out.println("------------------------------------------------------------ses id: " + sessionId);
			SessionDataRecord record = create.selectFrom(SESSION_DATA).where(SESSION_DATA.SESSION_ID.equal(sessionId))
					.fetchOne();
			if (record == null) {
				throw new RecordDoesNotExistsException("This session does not exists.");
			}
			Session session = record.into(Session.class);
			return session;
		}
	}

}
