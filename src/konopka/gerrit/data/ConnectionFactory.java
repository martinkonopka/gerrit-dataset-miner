package konopka.gerrit.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Martin on 4.7.2015.
 */
public class ConnectionFactory {
    public static Connection getMSSQLConnection(String connectionString) throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        String connectionUrl = "jdbc:" + connectionString;

        return DriverManager.getConnection(connectionUrl);
    }
}

