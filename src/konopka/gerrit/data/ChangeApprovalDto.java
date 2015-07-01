package konopka.gerrit.data;

/**
 * Created by Martin on 30.6.2015.
 */
public class ChangeApprovalDto {
    public final ChangeDto change;
    public final CommentDto comment;
    //public AccountDto getAuthor() { return comment.author; }
    public final ApprovalValueDto value;
    public final ApprovalTypeDto type;

    public ChangeApprovalDto(ChangeDto change, CommentDto comment, ApprovalTypeDto type, ApprovalValueDto value) {
        this.change = change;
        this.comment = comment;
        this.type = type;
        this.value = value;
    }
}
