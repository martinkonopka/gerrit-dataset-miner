package konopka.gerrit.clients;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.BranchInfo;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import konopka.gerrit.data.*;
import konopka.gerrit.data.cache.ProjectsCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Martin on 26.6.2015.
 */
public class ProjectsClient {
    private GerritApi api;
    private IProjectsRepository repo;
    private ProjectsCache cache;
    private Boolean downloadParents;
    public ProjectsClient(GerritApi api, IProjectsRepository repository, Boolean downloadParents) {
        this.api = api;
        this.repo = repository;

        this.cache = new ProjectsCache(repository);
        this.downloadParents = downloadParents;
    }

    public void prepare() {
        cache.restore();;
    }


    public ProjectDto getProject(String id) {
        if (cache.isCached(id)) {
            return cache.tryGetCached(id);
        }

        ProjectInfo info = null;

        try {
            info = api.projects().name(id).get();
        } catch (RestApiException e) {
            e.printStackTrace();
        }

        if (info != null) {
            ProjectDto project = new ProjectDto(info.id, info.name);

            if (info.parent != null && downloadParents) {
                ProjectDto parent = getProject(info.parent);
                project.parentId = Optional.of(parent.id);
            }

            project = repo.add(project);

            getProjectBranches(project);
            getApprovals(project);

            cache.cache(project);

            return project;
        }

        return null;
    }





    private void getApprovals(ProjectDto project) {
        int start=  0;
        int limit = 1;

        List<ApprovalTypeDto> approvals = new ArrayList<>();
        while (approvals.size() <= 0) {
            List<ChangeInfo> changes;
            try {
                changes = api.changes().query()
                        .withStart(start)
                        .withLimit(limit)
                        .withOption(ListChangesOption.DETAILED_LABELS)
                        .get();

                if (changes.size() > 0) {
                    ChangeInfo change = changes.get(0);

                    if (change.labels.size() > 0) {
                        change.labels.entrySet().stream()
                                .map(e -> {
                                    short defaultValue = 0;
                                    if (e.getValue().defaultValue != null) {
                                        defaultValue = e.getValue().defaultValue;
                                    }

                                    return new ApprovalTypeDto(project,
                                            e.getKey(),
                                            defaultValue,
                                            e.getValue().values.entrySet().stream()
                                                    .map(ev -> new ApprovalValueDto(ev.getKey(), ev.getValue())).toArray(ApprovalValueDto[]::new));
                                })
                                .map(repo::addApprovalType)
                                .forEach(approvals::add);
                    }
                }
            } catch (RestApiException e) {
                e.printStackTrace();
            }

            start += limit;
        }

        approvals.forEach(a -> project.approvals.put(a.name, a));
    }

    private void getProjectBranches(ProjectDto project) {
        List<BranchInfo> branches = null;
        try {
            branches = api.projects().name(project.projectId).branches().get();
        } catch (RestApiException e) {
            e.printStackTrace();
        }

        if (branches != null) {
            branches.stream().map(b -> repo.addBranch(project, b.ref, b.revision))
                    .filter(b -> b != null)
                    .forEach(project.branches::add);
        }
    }
}
