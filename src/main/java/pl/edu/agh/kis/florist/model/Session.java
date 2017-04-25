package pl.edu.agh.kis.florist.model;

import java.sql.Timestamp;

import pl.edu.agh.kis.florist.db.tables.pojos.SessionData;

public class Session extends SessionData{

	public Session(String sessionId, Integer userId, Timestamp lastAccessed) {
		super(sessionId, userId, lastAccessed);
	}

}
