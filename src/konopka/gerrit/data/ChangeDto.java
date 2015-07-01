package konopka.gerrit.data;

import com.google.gerrit.extensions.client.ChangeStatus;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin on 26.6.2015.
 */
public class ChangeDto
{
    public ChangeDto(int id, ProjectDto project, BranchDto branch, AccountDto owner)
    {
        this.id = id;
        this.project = project;
        this.branch = branch;
        this.owner = owner;
        this.approvals =  new ArrayList<>();
        this.comments = new ArrayList<>();
        this.patchSets = new ArrayList<>();

    }

    public int id; // number
    public String changeId; // naturalId
    public ProjectDto project;
    public BranchDto branch;
    public AccountDto owner;

    public String topic;
    public String subject;

    public Timestamp createdAt;
    public Timestamp updatedAt;

    public ChangeStatus state;

    public Boolean isMergeable;

    public String baseChangeId;

    public int getNumberOfPatchSets() { return patchSets.size(); }


  //  public List<ApprovalDto> approvals;
  //  public List<CommentDto> comments;

    public PatchSetDto currentPatchSet; // foreign
    public List<PatchSetDto> patchSets;

    public List<ChangeApprovalDto> approvals;
    public List<CommentDto> comments;

    //public List<ProblemDto> problems;
}


