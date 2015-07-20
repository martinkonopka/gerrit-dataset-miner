package konopka.gerrit.data.entities;

import java.sql.*;
import java.time.Instant;

public class ChangeDownloadDto {
    private int changeId;
    private DownloadResult result;
    private Timestamp lastAttempt;
    private int attempts;

    public ChangeDownloadDto(int changeId) {
        this.changeId = changeId;
        this.result = DownloadResult.NO_ATTEMPT;
        this.attempts = 0;
        this.lastAttempt = Timestamp.from(Instant.now());
        this.dirty();
    }

    public ChangeDownloadDto(int changeId, DownloadResult result, Timestamp lastAttempt, int attempts)
    {
        this.changeId = changeId;
        this.result = result;
        this.lastAttempt = lastAttempt;
        this.attempts = attempts;
    }

    private boolean isdirty = false;
    public final boolean isDirty() {
        return isdirty;
    }

    private void dirty() {
        isdirty = true;
    }

    public int getChangeId() {
        return changeId;
    }


    public DownloadResult getResult() {
        return result;
    }

    public void setResult(DownloadResult result) {
        dirty();

        this.result = result;
    }

    public Timestamp getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(Timestamp lastAttempt) {
        dirty();

        this.lastAttempt = lastAttempt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        dirty();

        this.attempts = attempts;
    }
}