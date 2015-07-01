package konopka.gerrit.data;

import com.google.gerrit.extensions.common.ChangeInfo;

/**
 * Created by Martin on 25.6.2015.
 */
public interface IChangesRepository extends IRepository {
    boolean addChange(ChangeDto change);

    boolean containsChange(int id);
}
