package konopka.gerrit;

import com.google.gerrit.extensions.api.GerritApi;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import konopka.gerrit.clients.AccountsClient;
import konopka.gerrit.clients.ChangesClient;
import konopka.gerrit.clients.ProjectsClient;
import konopka.gerrit.data.ProjectDto;
import konopka.gerrit.data.mssql.DataRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Martin on 29.6.2015.
 */
public class Main {

    private static Connection connect(String connectionString) throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        String connectionUrl = "jdbc:" + connectionString;

        return DriverManager.getConnection(connectionUrl);

    }

    public static void main(String[] args) {


        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
	// or: authData = new GerritAuthData.Basic("https://example.com/gerrit", "user", "password"");
        GerritAuthData.Basic authData = new GerritAuthData.Basic("https://git.eclipse.org/r/");
    	//    GerritAuthData.Basic authData = new GerritAuthData.Basic("https://android-review.googlesource.com/");

        GerritApi gerritApi = gerritRestApiFactory.create(authData);
        String connectionString = "sqlserver://localhost;databaseName=eclipse-gerrit-dataset;integratedSecurity=true;";

        try {

            DataRepository repo = new DataRepository(connect(connectionString));

            repo.init();

            ProjectsClient projects = new ProjectsClient(gerritApi, repo.projects(), false);
            AccountsClient accounts = new AccountsClient(gerritApi, repo.accounts());
            ChangesClient changes = new ChangesClient(gerritApi, repo.changes(), projects, accounts, repo.accounts());

            projects.prepare();
            accounts.prepare();

            changes.get(0, 20, 100000, 1000);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

}
