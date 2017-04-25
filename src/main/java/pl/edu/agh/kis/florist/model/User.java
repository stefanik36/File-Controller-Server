package pl.edu.agh.kis.florist.model;
import pl.edu.agh.kis.florist.db.tables.pojos.Users;
public class User extends Users{

	public User(Integer id, String userName, String displayName, String hashedPassword) {
		super(id, userName, displayName, hashedPassword);
	}

}
