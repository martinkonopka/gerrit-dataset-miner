package konopka.gerrit.data.cache;

import konopka.gerrit.data.IProjectsRepository;
import konopka.gerrit.data.ProjectDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin on 25.6.2015.
 *
 *
 */

public class ProjectsCache {

    private Map<String, ProjectDto> map;

    private IProjectsRepository repo;
    public ProjectsCache(IProjectsRepository repo) {
        this.repo = repo;
        this.map = new HashMap<>();
    }

    public void restore() {
        repo.getAllProjects().forEach(this::cache);
    }

    public boolean isCached(String gerritId) {
        return map.containsKey(gerritId);
    }

    public ProjectDto tryGetCached(String gerritId) {
        if (isCached(gerritId)) {
            return map.get(gerritId);
        }
        return null;
    }

    public void cache(ProjectDto project) {
        if (isCached(project.projectId) == false) map.put(project.projectId, project);
    }
}
