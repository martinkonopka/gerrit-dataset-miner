package konopka.gerrit.data;

import com.google.gerrit.extensions.client.Comment;

import java.sql.Timestamp;

public class CommentDto {
    public CommentDto(ChangeDto change, String id) {
        this.change = change;
        this.id = id;
    }

    public CommentDto(PatchSetDto patchSet, String id) {
        this.patchSet = patchSet;
        this.change = patchSet.change;
        this.id = id;
    }

    public CommentDto(PatchSetFileDto file, String id) {
        this(file.patchSet, id);
        this.patchSetFile = file;
    }

    public ChangeDto change;
    public PatchSetDto patchSet;
    public PatchSetFileDto patchSetFile;
    public String id;
    public String inReplyToCommentId;
    //public CommentDto inReplyTo;
    public Timestamp createdAt;
    public String message;
    public AccountDto author;
    public Integer line;
    public Comment.Range range;
}


