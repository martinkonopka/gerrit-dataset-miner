package konopka.gerrit.clients;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.Url;
import konopka.gerrit.data.AccountDto;
import konopka.gerrit.data.IAccountsRepository;
import konopka.gerrit.data.cache.AccountsCache;

/**
 * Created by Martin on 27.6.2015.
 */
public class AccountsClient {

    private GerritApi api;
    private final IAccountsRepository repo;
    private final AccountsCache cache;

    public AccountsClient(GerritApi api, IAccountsRepository repo) {
        this.api = api;
        this.repo = repo;
        this.cache = new AccountsCache(repo);
    }

    public void prepare() {
        cache.restore();
    }

    public AccountDto get(String name, String email) {
        AccountDto account = cache.tryGetCached(email, name, null);
        if (account != null) {
            return account;
        }


        AccountInfo info = getAccountInfo(email);
        if (info == null) {
            info = getAccountInfo(Url.encode(name));
        }

        if (info != null && cache.isCached(info.email, info.name, info._accountId)) {
            return cache.tryGetCached(info.email, info.name, info._accountId);
        }

        account = info == null ? saveGitAccount(name, email) : saveGerritAccount(info);
        if (account != null) {
            cache.cache(account);
        }
        return account;
    }

    protected AccountInfo getAccountInfo(String id) {
        AccountInfo info = null;
        try {
            info = api.accounts().id(id).get();
        } catch (RestApiException e) {
            System.out.println("ERROR: Account not found: " + id);
        }
        return info;
    }

    public AccountDto get(AccountInfo info) {
        if (cache.isCached(info.email, info.name, info._accountId))
        {
            return cache.tryGetCached(info.email, info.name, info._accountId);
        }

        AccountDto account = saveGerritAccount(info);
        if (account != null) {
            cache.cache(account);
        }
        return account;
    }

    private AccountDto saveGerritAccount(AccountInfo info) {
        if (info != null) {
            AccountDto account = new AccountDto(info.name, info.email, info.username, info._accountId);
            account = repo.add(account);

            return account;
        }
        return null;
    }

    private AccountDto saveGitAccount(String name, String email) {
        if (email != null) {
            AccountDto account = new AccountDto(name, email);
            account = repo.add(account);

            return account;
        }
        return null;
    }
}
