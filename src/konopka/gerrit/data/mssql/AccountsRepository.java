package konopka.gerrit.data.mssql;

import konopka.gerrit.data.entities.AccountDto;
import konopka.gerrit.data.IAccountsRepository;
import konopka.gerrit.data.Repository;
import konopka.util.Logging;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin on 27.6.2015.
 */
public class AccountsRepository extends Repository implements IAccountsRepository {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AccountsRepository.class);

    private static final String CREATE_TABLE_QUERY = "CREATE TABLE [Accounts] " +
            "(" +
            "[Id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY," +
            "[Name] [nvarchar](max) NULL," +
            "[Email] [nvarchar](max) NULL," +
            "[Username] [nvarchar](max) NULL," +
            "[GerritId] int NULL" +
            ");";

    private static final String SELECT_ACCOUNTS_QUERY = "SELECT [Id], [Name], [Email], [Username], [GerritId] FROM [Accounts]";

    private Connection connection;

    public AccountsRepository(Connection connection) {
        this.connection = connection;
    }


    @Override
    public void init() {
        executeSqlStatement(connection, CREATE_TABLE_QUERY);
    }



    private static final String INSERT_ACCOUNT_QUERY = "INSERT INTO [Accounts] " +
            "([Name], [Email], [Username], [GerritId]) VALUES(?, ?, ?, ?)";

    @Override
    public AccountDto add(AccountDto account) {
        PreparedStatement stmt = null;
        try {

            stmt = connection.prepareStatement(INSERT_ACCOUNT_QUERY, Statement.RETURN_GENERATED_KEYS);
            if (account.name != null) {
                stmt.setString(1, account.name);
            }
            else {
                stmt.setNull(1, Types.NVARCHAR);
            }

            if (account.email != null) {
                stmt.setString(2, account.email);
            }
            else {
                stmt.setNull(2, Types.NVARCHAR);
            }

            if (account.username != null) {
                stmt.setString(3, account.username);
            }
            else {
                stmt.setNull(3, Types.NVARCHAR);
            }

            if (account.accountId != null) {
                stmt.setInt(4, account.accountId);
            }
            else {
                stmt.setNull(4, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    account.id = generatedKeys.getInt(1);
                }
            }


        } catch (SQLException e) {
            logger.error(Logging.prepare("add", account.email));
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }

        return account;
    }

    @Override
    public List<AccountDto> getAll() {
        Statement stmt = null;
        List<AccountDto> accounts = new ArrayList<>();
        try {

            stmt = connection.createStatement();

            ResultSet results = stmt.executeQuery(SELECT_ACCOUNTS_QUERY);

            while (results.next()) {
                AccountDto account = new AccountDto(
                        results.getInt("Id"),
                        results.getString("Name"),
                        results.getString("Email"),
                        results.getString("Username"),
                        results.getInt("GerritId"));

                accounts.add(account);
            }


        } catch (SQLException e) {
            logger.error(Logging.prepare("getAll"), e);
//            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }
        return accounts;
    }

}
