package konopka.gerrit.data.mssql;

import konopka.gerrit.data.*;

import java.sql.Connection;

/**
 * Created by Martin on 25.6.2015.
 */
public class DataRepository implements IDataRepository {

    private IProjectsRepository _projects;
    private IAccountsRepository _accounts;
    private IChangesRepository _changes;
    private IDownloadsRepository _downloads;

    public DataRepository(Connection connection) {
        _projects = new ProjectsRepository(connection);
        _accounts = new AccountsRepository(connection);
        _changes = new ChangesRepository(connection);
        _downloads = new DownloadsRepository(connection);
    }

    @Override
    public void init() {
        _projects.init();
        _accounts.init();
        _changes.init();
        _downloads.init();
    }

    @Override
    public IProjectsRepository projects() {
        return _projects;
    }

    @Override
    public IAccountsRepository accounts() { return _accounts; }

    @Override
    public IChangesRepository changes() {
        return _changes;
    }

    @Override
    public IDownloadsRepository downloads() { return _downloads; }
}
