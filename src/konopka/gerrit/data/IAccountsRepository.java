package konopka.gerrit.data;

import konopka.gerrit.data.entities.AccountDto;

import java.util.List;

/**
 * Created by Martin on 27.6.2015.
 */
public interface IAccountsRepository extends IRepository {
    AccountDto add(AccountDto account);

    List<AccountDto> getAll();
}
