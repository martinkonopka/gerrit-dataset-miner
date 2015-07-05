package konopka.gerrit.data.entities;

import java.util.*;

/**
 * Created by Martin on 26.6.2015.
 */
public class ProjectDto {
    public ProjectDto(String projectId, String name) {
        this.projectId = projectId;
        this.name = name;
        parentId = Optional.empty();
        branches = new ArrayList<>();
        approvals = new HashMap<>();
    }

    public ProjectDto(int id, String projectId, String name) {
        this(projectId, name);
        this.id = id;
    }

    public int id;
    public final String projectId;
    public final String name;
    public Optional<Integer> parentId;

    public final List<BranchDto> branches;
    public final Map<String, ApprovalTypeDto> approvals;

    public BranchDto getBranch(String key) {
        Optional<BranchDto> branch = branches.stream().filter(b -> b.name.equals(key) || b.revision.equals(key) || (b.name.endsWith(key) && b.name.startsWith("refs")) || b.name.contains(key)).findFirst();
        if (branch.isPresent()) {
            return branch.get();
        }
        return null;
    }
}
