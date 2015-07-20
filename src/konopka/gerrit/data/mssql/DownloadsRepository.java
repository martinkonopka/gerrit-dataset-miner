package konopka.gerrit.data.mssql;

import konopka.gerrit.data.IDownloadsRepository;
import konopka.gerrit.data.Repository;
import konopka.gerrit.data.entities.ChangeDownloadDto;
import konopka.gerrit.data.entities.DownloadResult;
import konopka.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;

/**
 * Created by Martin on 17.7.2015.
 */
public class DownloadsRepository extends Repository implements IDownloadsRepository {


    private static final Logger logger = LoggerFactory.getLogger(DownloadsRepository.class);

    private final Connection connection;


    private static final String CREATE_TABLE_DOWNLOADS = "CREATE TABLE [Downloads] (" +
            "[ChangeId] [int] NOT NULL PRIMARY KEY," +
            "[Result] [int] NOT NULL," +
            "[LastAttemptAt] [datetime] NOT NULL," +
            "[Attempts] [int] NOT NULL" +
            ");";

    private static final String INSERT_DOWNLOAD_QUERY = "INSERT INTO [Downloads] " +
            "(ChangeId, Result, LastAttemptAt, Attempts) " +
            "VALUES(?, ?, ?, ?);";

    private static final String UPDATE_DOWNLOAD_QUERY = "UPDATE [Downloads] " +
            "SET " +
            "[Result] = ? " +
            ",[LastAttemptAt] = ? " +
            ",[Attempts] = ? " +
            " WHERE [ChangeId] = ?;";

    private static final String SELECT_DOWNLOADS_QUERY = "SELECT [ChangeId], [Result], [LastAttemptAt], [Attempts] " +
            "FROM [Downloads] WHERE [ChangeId] = ?;";



    public DownloadsRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void init() {
        executeSqlStatement(connection, CREATE_TABLE_DOWNLOADS);
    }

    @Override
    public boolean addDownload(ChangeDownloadDto download) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_DOWNLOAD_QUERY);
            stmt.setInt(1, download.getChangeId());
            stmt.setInt(2, download.getResult().getValue());
            stmt.setTimestamp(3, download.getLastAttempt());
            stmt.setInt(4, download.getAttempts());
            stmt.execute();

            return true;
        } catch (SQLException ex) {
            logger.error(Logging.prepare("addDownload", Integer.toString(download.getChangeId())));
        } finally {
            closeStatement(stmt);
        }
        return false;
    }



    @Override
    public ChangeDownloadDto getDownload(int changeId) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(SELECT_DOWNLOADS_QUERY);
            stmt.setInt(1, changeId);

            ResultSet results = stmt.executeQuery();

            if (results.next()) {
                ChangeDownloadDto download = new ChangeDownloadDto(
                        results.getInt("ChangeId"),
                        DownloadResult.fromInt(results.getInt("Result")),
                        results.getTimestamp("LastAttemptAt"),
                        results.getInt("Attempts"));
                return download;
            }
        } catch (SQLException ex) {

        } finally {
            closeStatement(stmt);
        }

        return new ChangeDownloadDto(changeId);
    }

    private boolean checkDownload(int changeId) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(SELECT_DOWNLOADS_QUERY);
            stmt.setInt(1, changeId);

            ResultSet results = stmt.executeQuery();

            return results.next();

        } catch (SQLException ex) {

        } finally {
            closeStatement(stmt);
        }

        return false;
    }


    public void saveDownload(ChangeDownloadDto download) {
        if (download.isDirty()) {
            boolean exists = checkDownload(download.getChangeId());


            if (exists) {
                updateDownload(download);
            } else {
                addDownload(download);
            }
        }
    }

    private void updateDownload(ChangeDownloadDto download) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(UPDATE_DOWNLOAD_QUERY);
            stmt.setInt(1, download.getResult().getValue());
            stmt.setTimestamp(2, download.getLastAttempt());
            stmt.setInt(3, download.getAttempts());
            stmt.setInt(4, download.getChangeId());

            stmt.execute();
        } catch (SQLException ex) {
            logger.error(Logging.prepare("updateDownload", Integer.toString(download.getChangeId())), ex);
        } finally {
            closeStatement(stmt);
        }
    }
}
