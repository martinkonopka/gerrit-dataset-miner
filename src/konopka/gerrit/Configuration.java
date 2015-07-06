package konopka.gerrit;

import konopka.gerrit.search.ChangesQueryBuilder;
import konopka.util.Logging;
import konopka.util.SymbolMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by Martin on 3.7.2015.
 */
public class Configuration {
    private String databaseConnectionString = "";
    private String gerritEndpoint = "";
    private String changesQuery = "";

    private long downloadPause = 0;

    public final String getDatabaseConnectionString() {
        return databaseConnectionString;
    }

    public final String getGerritEndpoint() {
        return gerritEndpoint;
    }

    public final String getChangesQuery() {
        return changesQuery;
    }

    public final long getDownloadPause() { return downloadPause; }

    private static final Logger logger;

    static {
        logger = LoggerFactory.getLogger(Configuration.class);
    }


    public Configuration(String path) {
        setDefaults();
        SymbolMap sm  = new SymbolMap(new File(path));
        String connection = sm.lookupSymbol("DatabaseConnectionString");
        if (connection.isEmpty() == false) {
            databaseConnectionString = connection;
        }
        String endpoint = sm.lookupSymbol("GerritEndpoint");
        if (endpoint.isEmpty() == false) {
            gerritEndpoint = endpoint;
        }

        String query = sm.lookupSymbol("ChangesQuery");
        if (query.isEmpty() == false) {
            changesQuery = query;
        }

        String pause = sm.lookupSymbol("DownloadPause");
        if (pause.isEmpty() == false) {
            downloadPause = Long.parseLong(pause);
        }

        log();
    }

    public Configuration() {
        setDefaults();
        log();
    }

    private void setDefaults() {
        databaseConnectionString = "sqlserver://localhost;databaseName=android-gerrit-dataset;integratedSecurity=true;";
        gerritEndpoint = "https://android-review.googlesource.com/";
        changesQuery = "status:reviewed+OR+status:merged+OR+status:open+OR+status:abandoned";
        downloadPause = 5000;
    }

    private void log() {
        logger.info(Logging.prepareWithPart("init", "DatabaseConnectionString: " + databaseConnectionString));
        logger.info(Logging.prepareWithPart("init", "GerritEndpoint: " + gerritEndpoint));
        logger.info(Logging.prepareWithPart("init", "ChangesQuery: " + changesQuery));
        logger.info(Logging.prepareWithPart("init", "DownloadPause: " + downloadPause));
    }
}
