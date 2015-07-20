package konopka.gerrit.clients;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.projects.BranchInfo;
import com.google.gerrit.extensions.api.projects.ProjectApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FetchInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.sun.javaws.exceptions.InvalidArgumentException;
import konopka.gerrit.data.*;
import konopka.gerrit.data.cache.ProjectsCache;
import konopka.gerrit.data.entities.*;
import konopka.util.Logging;
import org.apache.log4j.spi.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Martin on 26.6.2015.
 */
public class ProjectsClient {
   private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ProjectsClient.class);


    private GerritApi api;
    private IProjectsRepository repo;
    private ProjectsCache cache;
    private Boolean downloadParents;
    private WaitCaller caller;

    public ProjectsClient(GerritApi api, WaitCaller caller, IProjectsRepository repository, Boolean downloadParents) {
        this.api = api;
        this.repo = repository;
        this.caller = caller;

        this.cache = new ProjectsCache();
        this.downloadParents = downloadParents;
    }

    public void prepare() {
        cache.restore(repo.getAllProjects());
    }


    public ProjectDto getProject(String id) {
        if (cache.isCached(id)) {
            return cache.tryGetCached(id);
        }

        ProjectInfo info = null;

        try {
            info = caller.waitOrCall(() -> api.projects().name(id).get());
        } catch (Exception e) {

            logger.error(Logging.prepare("getProject", id), e);
        }

        if (info != null) {
            ProjectDto project = new ProjectDto(info.id, info.name);

            if (info.parent != null && downloadParents) {
                ProjectDto parent = getProject(info.parent);
                project.parentId = Optional.of(parent.id);
            }

            project = repo.add(project);

           // getProjectBranches(project);
           // getApprovals(project);

            cache.cache(project);

            return project;
        }

        return null;
    }


//
//    private void getApprovals(ProjectDto project) {
//        int start = 0;
//        int limit = 1;
//
//        boolean getCommands = true;
//
//        List<ApprovalTypeDto> approvals = new ArrayList<>();
//        while (approvals.size() <= 0) {
//            List<ChangeInfo> changes = null;
//
//            try {
//                Changes.QueryRequest request = api.changes().query()
//                        .withStart(start)
//                        .withLimit(limit)
//                        .withOptions(ListChangesOption.DETAILED_LABELS);
//                if (getCommands) {
//                    request.withOption(ListChangesOption.DOWNLOAD_COMMANDS);
//                }
//
//                changes = caller.waitOrCall(() -> request.get());
//            } catch (Exception e) {
//                e.printStackTrace();
//                getCommands = false;
//            }
//
//            if (changes != null && changes.size() > 0) {
//                ChangeInfo change = changes.get(0);
//
//                if (change.labels != null && change.labels.size() > 0) {
//                    approvals.addAll(createApprovalTypes(project, change.labels));
//                }
//
//            }
//
//            start += limit;
//        }
//
//        approvals.forEach(repo::addApprovalType);
//
//        approvals.forEach(a -> project.approvals.put(a.name, a));
//    }

    private List<ApprovalTypeDto> createApprovalTypes(ProjectDto project, Map<String, LabelInfo> labels) {
        return labels.entrySet().stream()
                .filter(e -> project.approvals.containsKey(e.getKey()) == false)
                .map(e -> createApprovalType(project, e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private ApprovalTypeDto createApprovalType(ProjectDto project, String type, LabelInfo label) {
        short defaultValue = 0;
        if (label != null && label.defaultValue != null) {
            defaultValue = label.defaultValue;
        }

        return new ApprovalTypeDto(
                project,
                type,
                defaultValue,
                label.values.entrySet().stream()
                        .map(ev -> new ApprovalValueDto(ev.getKey(), ev.getValue())).toArray(ApprovalValueDto[]::new));
    }

    public void addApprovals(ProjectDto project, Map<String, LabelInfo> labels) {
        if (labels.size() > 0) {
            labels.entrySet().forEach(e -> addApproval(project, e.getKey(), e.getValue()));
        }
    }

    public void addApproval(ProjectDto project, String key, LabelInfo label) {
        if (project.approvals.containsKey(key) == false) {
            ApprovalTypeDto approval = createApprovalType(project, key, label);
            repo.addApprovalType(approval);
            project.approvals.put(approval.name, approval);
        }
    }

    private void getProjectBranches(ProjectDto project) {
        List<BranchInfo> branches = null;
        try {
            ProjectApi.ListBranchesRequest request = api.projects().name(project.projectId).branches();
            branches = caller.waitOrCall(() -> request.get());
        } catch (Exception e) {
            logger.error(Logging.prepare("getProjectBranches", project.projectId), e);

        }

        if (branches != null) {
            List<BranchDto> projectBranches = branches.stream()
                    .filter(b -> project.hasBranch(b.ref) == false)
                    .map(b -> repo.addBranch(project, b.ref, b.revision))
                    .filter(b -> b != null)
                    .collect(Collectors.toList());

            projectBranches.forEach(project.branches::add);
        }
    }

    public BranchDto getBranch(ProjectDto project, String name) {
        if (project.hasBranch(name) == false) {
            repo.loadProjectBranches(project);
        }

        if (project.hasBranch(name) == false) {
            getProjectBranches(project);
        }
        return project.getBranch(name);

    }
}
