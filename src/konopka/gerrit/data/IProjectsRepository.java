package konopka.gerrit.data;


import java.util.List;

public interface IProjectsRepository extends IRepository {

    ProjectDto add(ProjectDto project);
    BranchDto addBranch(ProjectDto project, String branch, String revision);

    ApprovalTypeDto addApprovalType(ApprovalTypeDto approvalTypeDto);

    List<ProjectDto> getAllProjects();
    //  int getProjectId(String name);

}
