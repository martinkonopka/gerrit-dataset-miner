package konopka.gerrit;

import com.google.gerrit.extensions.api.GerritApi;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import konopka.gerrit.clients.AccountsClient;
import konopka.gerrit.clients.ChangesClient;
import konopka.gerrit.clients.ProjectsClient;
import konopka.gerrit.clients.WaitCaller;
import konopka.gerrit.data.ConnectionFactory;
import konopka.gerrit.data.mssql.DataRepository;
import konopka.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
* Created by Martin on 4.7.2015.
*/
public class GerritMiner {
    private static final Logger logger;

    static {
        logger = LoggerFactory.getLogger(GerritMiner.class);
    }

    private final Configuration configuration;
    private final ProjectsClient projects;
    private final AccountsClient accounts;
    private final ChangesClient changes;
    private final DataRepository repo;

    public GerritMiner(Configuration config) throws SQLException, ClassNotFoundException {
        configuration = config;

        WaitCaller caller = new WaitCaller(configuration.getDownloadPause());

        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        // or: authData = new GerritAuthData.Basic("https://example.com/gerrit", "user", "password"");
        GerritAuthData.Basic authData = new GerritAuthData.Basic(configuration.getGerritEndpoint());
        // GerritAuthData.Basic authData = new GerritAuthData.Basic("https://git.eclipse.org/r/");

        GerritApi gerritApi = gerritRestApiFactory.create(authData);

        repo = new DataRepository(ConnectionFactory.getMSSQLConnection(configuration.getDatabaseConnectionString()));

        projects = new ProjectsClient(gerritApi, caller, repo.projects(), false);
        accounts = new AccountsClient(gerritApi, caller, repo.accounts());
        changes = new ChangesClient(gerritApi, caller, repo.changes(), projects, accounts);
    }

    public void init() {
        repo.init();

        projects.prepare();
        accounts.prepare();
    }

    public void mine() {
        try {
            changes.get(configuration.getChangesQuery(), 0, 20);
        } catch (InterruptedException e) {
            logger.info(Logging.prepare("mine", "interrupted"));
            e.printStackTrace();
        }
    }
}
