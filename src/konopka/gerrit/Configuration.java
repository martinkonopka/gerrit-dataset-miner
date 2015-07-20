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

    private long downloadPause;
    private int queryStart;
    private int queryLimit;

    private String miningMode = "";
    private int queryStop;
    private int downloadAttemptsLimit;

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

    public final String getMiningMode() { return miningMode; }

    public final int getStop() { return queryStop; }

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

        String start = sm.lookupSymbol("Start");
        if (start.isEmpty() == false) {
            queryStart = Integer.parseInt(start);
        }

        String limit = sm.lookupSymbol("Limit");
        if (limit.isEmpty() == false) {
            queryLimit = Integer.parseInt(limit);
        }

        String mode = sm.lookupSymbol("MiningMode");
        if (mode.isEmpty() == false) {
            miningMode = mode.toLowerCase().trim();
        }

        String stop = sm.lookupSymbol("Stop");
        if (stop.isEmpty() == false) {
            queryStop = Integer.parseInt(stop);
        }

        String attemptsLimit = sm.lookupSymbol("DownloadAttemptsLimit");
        if (attemptsLimit.isEmpty() == false) {
            downloadAttemptsLimit = Integer.parseInt(attemptsLimit);
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
        queryStart = 0;
        queryLimit = 20;
        queryStop = 300000;
        miningMode = "simple";
        downloadAttemptsLimit = 3;
    }

    private void log() {
        logger.info(Logging.prepareWithPart("init", "DatabaseConnectionString: " + databaseConnectionString));
        logger.info(Logging.prepareWithPart("init", "GerritEndpoint: " + gerritEndpoint));
        logger.info(Logging.prepareWithPart("init", "ChangesQuery: " + changesQuery));
        logger.info(Logging.prepareWithPart("init", "DownloadPause: " + downloadPause));
        logger.info(Logging.prepareWithPart("init", "Start: " + queryStart));
        logger.info(Logging.prepareWithPart("init", "Limit: " + queryLimit));
        logger.info(Logging.prepareWithPart("init", "Stop: " + queryStop));
        logger.info(Logging.prepareWithPart("init", "MiningMode: " + miningMode));
        logger.info(Logging.prepareWithPart("init", "DownloadAttemptsLimit: " + downloadAttemptsLimit));

    }

    public int getStart() {
        return queryStart;
    }

    public int getLimit() {
        return queryLimit;
    }

    public final int getDownloadAttemptsLimit() {
        return downloadAttemptsLimit;
    }

}
