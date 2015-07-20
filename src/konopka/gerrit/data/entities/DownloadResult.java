package konopka.gerrit.data.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin on 19.7.2015.
 */
public enum DownloadResult {

    NO_ATTEMPT(0),
    DOWNLOADED(1),
    NOT_FOUND(2),
    ERROR(3),
    UNKNOWN(10);

    private final int value;

    DownloadResult(int v) {
        this.value = v;
    }

    private static final Map<Integer, DownloadResult> map = new HashMap<>();
    static {
        for (DownloadResult type : DownloadResult.values()) {
            map.put(type.value, type);
        }
    }

    public static DownloadResult fromInt(int i) {
        DownloadResult state = map.get(i);
        if (state == null)
            return DownloadResult.UNKNOWN;
        return state;
    }

    public int getValue() {
        return value;
    }
}
