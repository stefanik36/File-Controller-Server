package pl.edu.agh.kis.florist.dao;

import static pl.edu.agh.kis.florist.db.Tables.USERS;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.mindrot.jbcrypt.BCrypt;

import pl.edu.agh.kis.florist.db.tables.records.UsersRecord;
import pl.edu.agh.kis.florist.exceptions.RecordDoesNotExistsException;
import pl.edu.agh.kis.florist.exceptions.AlreadyExistsException;
import pl.edu.agh.kis.florist.model.User;

public class UserDAO {
	private final String DB_URL = "jdbc:sqlite:test.db";

	public static String createNewHashedPassword(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt());
	}

	public static boolean checkPassword(String candidatePassword, String storedHashedPassword) {
		return BCrypt.checkpw(candidatePassword, storedHashedPassword);
	}

	public User createNewUser(String userName, String userPass) {
		try (DSLContext create = DSL.using(DB_URL)) {
			UsersRecord record = create.selectFrom(USERS).where(USERS.USER_NAME.equal(userName)).fetchOne();
			if (record == null) {
				User result = create
						.insertInto(USERS, USERS.ID, USERS.USER_NAME, USERS.DISPLAY_NAME, USERS.HASHED_PASSWORD)
						.values(null, userName, userName, createNewHashedPassword(userPass)).returning(USERS.ID)
						.fetchOne().into(User.class);
				return getUser(result.getId());
			} else {
				throw new AlreadyExistsException("User with this name already exists.");
			}
		}
	}

	public User getUser(Integer userId) {
		try (DSLContext create = DSL.using(DB_URL)) {
			UsersRecord record = create.selectFrom(USERS).where(USERS.ID.equal(userId)).fetchOne();
			if (record == null) {
				throw new RecordDoesNotExistsException("There is no user with this id.");
			}
			return record.into(User.class);

		}
	}

	public User getUser(String name, String pass) {
		try (DSLContext create = DSL.using(DB_URL)) {
			UsersRecord record = create.selectFrom(USERS).where(USERS.USER_NAME.equal(name)).fetchOne();
			if (record == null) {
				throw new RecordDoesNotExistsException("There is no user with this name.");
			}
			User u = record.into(User.class);
			if (!checkPassword(pass, u.getHashedPassword())) {
				throw new RecordDoesNotExistsException("Wrong password.");
			}
			return u;
		}
	}
}