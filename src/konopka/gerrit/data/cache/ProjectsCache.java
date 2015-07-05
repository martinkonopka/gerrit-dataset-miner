package konopka.gerrit.data.cache;

import konopka.gerrit.data.IProjectsRepository;
import konopka.gerrit.data.entities.ProjectDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin on 25.6.2015.
 *
 *
 */

public class ProjectsCache {

    private Map<String, ProjectDto> map;

    public ProjectsCache() {
        this.map = new HashMap<>();
    }

    public void restore(List<ProjectDto> projects) {
        projects.forEach(this::cache);
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
