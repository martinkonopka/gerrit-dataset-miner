package konopka.gerrit;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import konopka.gerrit.clients.AccountsClient;
import konopka.gerrit.clients.ChangesClient;
import konopka.gerrit.clients.ProjectsClient;
import konopka.gerrit.clients.WaitCaller;
import konopka.gerrit.data.ConnectionFactory;
import konopka.gerrit.data.IDownloadsRepository;
import konopka.gerrit.data.entities.ChangeDownloadDto;
import konopka.gerrit.data.entities.ChangeDto;
import konopka.gerrit.data.entities.DownloadResult;
import konopka.gerrit.data.mssql.DataRepository;
import konopka.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;

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


    public void mine() throws IllegalStateException {
        String mode = configuration.getMiningMode();

        if (mode.equals("simple")) {
            mineSimple(configuration.getStart(), configuration.getStop());
        }
        else if (mode.equals("query")) {
            mineQuery(configuration.getChangesQuery(), configuration.getStart(), configuration.getLimit());
        }
        else {
            throw new IllegalStateException();
        }
    }


    private boolean canMineChange(ChangeDownloadDto download, int attempt, int maxAttempts) {
        if (attempt < maxAttempts) {

            DownloadResult result = download.getResult();
            switch (result) {
                case DOWNLOADED:
                    logger.info(Logging.prepareWithPart("canMineChange", "skip downloaded", Integer.toString(download.getChangeId())));
                    return false;

                case NOT_FOUND:
                    if (attempt > 1) {
                        logger.info(Logging.prepareWithPart("canMineChange", "skip not found", Integer.toString(download.getChangeId())));

                        return false;
                    }
                case ERROR:
                    boolean skip = download.getLastAttempt().toLocalDateTime().isBefore(LocalDateTime.now().minusDays(3));
                    if (skip == false) {
                        logger.info(Logging.prepareWithPart("canMineChange", "skip missing", Integer.toString(download.getChangeId())));
                    }

                    return skip;


                case UNKNOWN:
                case NO_ATTEMPT:
                default:
                    return true;
            }
        }
        return false;
    }

    private DownloadResult mineChange(int id) {
        DownloadResult result = DownloadResult.NOT_FOUND;

        List<ChangeInfo> details = changes.getChangeDetails(id);

        for (ChangeInfo detail : details) {
            try {
                ChangeDto change = changes.saveChange(detail);

                if (change != null) {
                    result = DownloadResult.DOWNLOADED;
                    break;
                }

            } catch (RestApiException e) {
                e.printStackTrace();
                result = DownloadResult.ERROR;
            } catch (InterruptedException e) {
                e.printStackTrace();
                result = DownloadResult.NO_ATTEMPT;
            }
        }

       return result;
    }

    private void checkChange(ChangeDownloadDto download) {
        DownloadResult result = download.getResult();
        if (result.equals(DownloadResult.DOWNLOADED)  && repo.changes().containsChange(download.getChangeId()) == false) {
            download.setResult(DownloadResult.NO_ATTEMPT);
        }
        else if (result.equals(DownloadResult.NO_ATTEMPT) && repo.changes().containsChange(download.getChangeId())) {
            download.setResult(DownloadResult.DOWNLOADED);
        }
    }

    private boolean tryMineChange(int id, int maxAttempts) {
        ChangeDownloadDto download = repo.downloads().getDownload(id);

        checkChange(download);

        int attempt = 1;
        while (canMineChange(download, attempt, maxAttempts))
        {
            download.setAttempts(download.getAttempts() + 1);
            download.setLastAttempt(Timestamp.from(Instant.now()));

            DownloadResult result = mineChange(download.getChangeId());
            download.setResult(result);

            attempt += 1;
        }

        repo.downloads().saveDownload(download);

        return download.getResult().equals(DownloadResult.DOWNLOADED);
    }

    private void mineQuery(String query, int start, int limit)  {
        logger.info(Logging.prepare("mineQuery", query, Integer.toString(start), Integer.toString(limit)));
        int maxAttempts = configuration.getDownloadAttemptsLimit();
        int count;
        try {
            do {
                List<ChangeInfo> infos = changes.downloadChanges(query, start, limit);
                count = infos.size();

                if (count > 0) {
                    int savedChanges = infos.stream()
                            .map(i -> tryMineChange(i._number, maxAttempts))
                            .map(d -> d ? 1 : 0)
                            .reduce(0, Integer::sum);

                    logger.info(Logging.prepareWithPart("mineQuery", String.format("changes %d/%d (%f)", savedChanges, count, (float) savedChanges / count)), start, limit);

                    start += limit;
                }

            } while (count > 0);

        } catch (Exception e) {
            logger.error(Logging.prepare("get"), e);
        }
    }


    public void mineSimple(int start, int stop) {
        int id = start;
        int count = 0;
        int savedChanges = 0;
        int maxAttempts =  configuration.getDownloadAttemptsLimit();

        try {

            for ( ; id < stop; id += 1) {
                count += 1;
                boolean saved = tryMineChange(id, maxAttempts);
                savedChanges += saved ? 1 : 0;

                if (count % 100 == 0) {
                    logger.info(Logging.prepareWithPart("mineSimple", String.format("changes %d - %d: %d/%d (%f)", id - count, id, savedChanges, count, (float) savedChanges / count)), start, stop);
                    count = 0;
                    savedChanges = 0;
                }
            }
        } catch (Exception e) {
            logger.error(Logging.prepare("mineSimple"), e);
        }

        logger.info(Logging.prepareWithPart("mineSimple", "last: " + id));
    }
}
