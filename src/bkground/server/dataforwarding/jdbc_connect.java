package bkground.server.dataforwarding;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import bkground.server.dataforwarding.SocketInfo.User;

import com.mysql.jdbc.Statement;

public class jdbc_connect {
	private Connection connection;
	private Statement stmt;

	/* Main constructor for class */
	public jdbc_connect(String dbAddresss, String dbName, String userName,
			String passWord) {
		stmt = null;
		dbAddresss = "jdbc:mysql://" + dbAddresss + ":3306/" + dbName;
		try {
			/* Register JDBC driver with the machine */
			DriverManager.registerDriver(new com.mysql.jdbc.Driver());
		} catch (SQLException e) {
			System.out.println("Driver registration failed");
			e.printStackTrace();
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("JDBC Driver not found!");
			e.printStackTrace();
			return;
		}

		try {
			/* Connect to the database */
			this.connection = DriverManager.getConnection(dbAddresss, userName,
					passWord);

		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
			return;
		}
		if (connection != null) {
			/* Check if connection is successful or not */
			System.out.println("Connection to database successful");
		} else {
			System.out.println("Failed to make connection!");
		}
	}

	/* Remember to call it at the end of the program */
	void close_connection() {
		try {
			if (connection != null)
				connection.close();
			System.out.println("Connection closed successfully");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/* Inserts record in terminalconnections database */
	private void insert_record_terminalconnections_db(int userID, int terminalID) {
		try {
			stmt = (Statement) connection.createStatement();

			String sql = "INSERT INTO terminalconnections_db (userID, terminalID) "
					+ "VALUES (" + userID + "," + terminalID + ")";
			stmt.executeUpdate(sql);
			System.out.println("Inserted records into the table...");

		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Inserts record in subscriptions database */
	void insert_record_subscriptions_db(int subs_id, String name) {
		try {
			stmt = (Statement) connection.createStatement();

			String sql = "INSERT INTO subscriptions_db " + "VALUES (" + subs_id
					+ "," + "'" + name + "')";
			stmt.executeUpdate(sql);
			System.out.println("Inserted records into the table...");

		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Inserts record in subscribers database */
	void insert_record_subscribers_db(int subscriptionId, int userID) {
		try {
			stmt = (Statement) connection.createStatement();

			String sql = "INSERT IGNORE INTO subscribers_db " + "VALUES ("
					+ subscriptionId + "," + userID + ")";
			stmt.executeUpdate(sql);
			System.out.println("Inserted records into the table...");

		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Inserts record in authentication database */
	void insert_record_authentication_db(int authID, int userID,
			String fieldKey, String fieldPass) {
		try {
			stmt = (Statement) connection.createStatement();

			String sql = "INSERT INTO authentication_db " + "VALUES (" + authID
					+ "," + userID + "," + "'" + fieldKey + "'" + "," + "'"
					+ fieldPass + "'" + ")";
			stmt.executeUpdate(sql);
			System.out.println("Inserted records into the table...");

		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Inserts record in users database */
	void insert_record_users_db(int userID, String firstName, String lastName,
			String gravatar) {
		try {
			stmt = (Statement) connection.createStatement();

			String sql = "INSERT INTO users_db " + "VALUES (" + userID + ","
					+ "'" + firstName + "'" + "," + "'" + lastName + "'" + ","
					+ "'" + gravatar + "'" + ")";
			stmt.executeUpdate(sql);
			System.out.println("Inserted records into the table...");

		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Gets record from subscribers database */
	void get_record_subscribers_db(int subscriptionID) {
		try {
			stmt = (Statement) connection.createStatement();
			String sql = "SELECT subscriptionID, userID FROM subscribers_db WHERE subscriptionID="
					+ subscriptionID;
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int id = rs.getInt("subscriptionID");
				int userID = rs.getInt("userID");
				System.out.print("SUBS_ID: " + id);
				System.out.println(", USER_ID: " + userID);
			}
			rs.close();
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Returns a list with the userID entries mapped to a subscriptitonID */
	public void get_record_list_subscribers_db(List<Integer> Users, int subscriptionID) {
		try {
			stmt = (Statement) connection.createStatement();
			String sql = "SELECT subscriptionID, userID FROM subscribers_db WHERE subscriptionID="
					+ subscriptionID;
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int id = rs.getInt("subscriptionID");
				int userID = rs.getInt("userID");
				Users.add(userID);
				System.out.print("SUBS_ID: " + id);
				System.out.println(", USER_ID: " + userID);

			}
			rs.close();
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Gets record from subscriptions database */
	void get_record_subscriptions_db(int subscriptionID) {
		try {
			stmt = (Statement) connection.createStatement();
			String sql = "SELECT subscriptionID, name FROM subscriptions_db";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int id = rs.getInt(subscriptionID);
				String name = rs.getString("name");

				// Display values
				System.out.print("SUBS_ID: " + id);
				System.out.print(", USER_NAME: " + name);
			}
			rs.close();
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Gets users record for given username and password */
	void authorize_user_db(User user, String username, String password) {
		try {
			stmt = (Statement) connection.createStatement();
			String sql = "SELECT firstname, lastname, users_db.userid FROM users_db, "
					+ "authentication_db WHERE authentication_db.fieldKey='"
					+ username
					+ "' and authentication_db.fieldPass='"
					+ password
					+ "' and authentication_db.userid=users_db.userid";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				user.firstname = rs.getString("firstname");
				user.lastname = rs.getString("lastname");
				user.username = username;
				user.id = rs.getInt("userid");

				insert_record_terminalconnections_db(user.id, ServerInfo.id);

				// Display values
				System.out.println("Authorized : " + user.firstname + " "
						+ user.lastname);
			}
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		}
	}

	/* Truncates data of the table passed to it */
	void truncate_table(String table_name) {
		try {
			stmt = (Statement) connection.createStatement();
			stmt.executeUpdate("TRUNCATE " + table_name);

		} catch (SQLException se) {
			// Handle errors for JDBC
			System.out.println("Could not truncate test_table "
					+ se.getMessage());
			se.printStackTrace();
		}
	}

	/*/get terminalID for a given userID from terminalconnections database */
	public int get_terminalID_for_userID(int userID) {
		try {
			stmt = (Statement) connection.createStatement();
			int terminalID = -1;
			String sql = "SELECT userID, terminalID FROM terminalconnections_db WHERE userID=" +
					userID + ")";
			ResultSet rs = stmt.executeQuery(sql);
			int id = -1;
			while(rs.next()) {
				terminalID = rs.getInt("terminalID");
				id = rs.getInt("userID");
				System.out.println("Getting terminalID for " + userID + " id: " + id
						+ "terminalID " + terminalID);
			}
			return terminalID;
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
			return -1;
		}
	}

}
