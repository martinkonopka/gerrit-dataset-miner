package konopka.gerrit.data;

/**
 * Created by Martin on 25.6.2015.
 */
public interface IDataRepository extends IRepository {
    IProjectsRepository projects();
    IChangesRepository changes();
//    IBranchesRepository branches();
    IAccountsRepository accounts();
}
