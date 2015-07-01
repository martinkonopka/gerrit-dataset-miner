package konopka.gerrit.data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PatchSetDto {
    public PatchSetDto(ChangeDto change, int number) {
        this.change = change;
        this.number = number;
        parents = new ArrayList<>();
        files = new ArrayList<>();
    }

    public int number;
    public ChangeDto change;

    public String gitCommitId;
    public String subject;
    public String message;
    public Timestamp createdAt;

    public List<PatchSetFileDto> files;
    public int getNumberOfFiles() { return files.size(); }
    public AccountDto author;
    public AccountDto committer;
    public int addedLines;
    public int deletedLines;

    public String ref;

    public List<String> parents;

}


