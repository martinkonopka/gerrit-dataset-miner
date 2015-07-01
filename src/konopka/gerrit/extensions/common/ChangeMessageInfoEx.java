package konopka.gerrit.extensions.common;

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeMessageInfo;

import java.sql.Timestamp;

/**
 * Created by Martin on 1.7.2015.
 */
public class ChangeMessageInfoEx {
    public ChangeMessageInfoEx(int order, ChangeMessageInfo info) {
        this.order = order;
        this.id = info.id;
        this.author = info.author;
        this.date = info.date;
        this.message = info.message;
        this._revisionNumber = info._revisionNumber;
    }
    public String id;
    public AccountInfo author;
    public Timestamp date;
    public String message;
    public Integer _revisionNumber;
    public int order;
}
