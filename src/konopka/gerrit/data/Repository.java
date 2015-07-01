package konopka.gerrit.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Martin on 29.6.2015.
 */
public abstract class Repository {


    protected boolean executeSqlStatement(Connection connection, String sql) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();

            stmt.executeUpdate(sql);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }
        return false;
    }

    protected void closeStatement(Statement stmt) {
        try {
            if (stmt != null && stmt.isClosed() == false)
                stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
