package konopka.gerrit.data.mssql;

import konopka.gerrit.data.IAccountsRepository;
import konopka.gerrit.data.IChangesRepository;
import konopka.gerrit.data.IDataRepository;
import konopka.gerrit.data.IProjectsRepository;

import java.sql.Connection;

/**
 * Created by Martin on 25.6.2015.
 */
public class DataRepository implements IDataRepository {

    private IProjectsRepository _projects;
    private IAccountsRepository _accounts;
    private ChangesRepository _changes;

    public DataRepository(Connection connection) {
        _projects = new ProjectsRepository(connection);
        _accounts = new AccountsRepository(connection);
        _changes = new ChangesRepository(connection);

    }

    @Override
    public void init() {
        _projects.init();
        _accounts.init();
        _changes.init();
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
}
