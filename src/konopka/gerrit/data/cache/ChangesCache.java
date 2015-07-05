package konopka.gerrit.data.cache;

import konopka.gerrit.data.IChangesRepository;

import java.sql.Timestamp;

/**
 * Created by Martin on 29.6.2015.
 */
public class ChangesCache {


    private Timestamp lastChangeAt;

    public Timestamp getLastChangeAt() { return lastChangeAt; }
    public void setLastChangeAt(Timestamp time) { lastChangeAt = time; }

    public ChangesCache() {

    }

    public void restore() {

    }
}
