package konopka.gerrit.data;

import konopka.gerrit.data.entities.ChangeDto;

/**
 * Created by Martin on 25.6.2015.
 */
public interface IChangesRepository extends IRepository {
    boolean addChange(ChangeDto change);

    boolean containsChange(int id);
}
