package konopka.gerrit.data;


import konopka.gerrit.data.entities.ApprovalTypeDto;
import konopka.gerrit.data.entities.BranchDto;
import konopka.gerrit.data.entities.ProjectDto;

import java.util.List;

public interface IProjectsRepository extends IRepository {
    ProjectDto add(ProjectDto project);
    BranchDto addBranch(ProjectDto project, String branch, String revision);

    ApprovalTypeDto addApprovalType(ApprovalTypeDto approvalTypeDto);

    List<ProjectDto> getAllProjects();
}
