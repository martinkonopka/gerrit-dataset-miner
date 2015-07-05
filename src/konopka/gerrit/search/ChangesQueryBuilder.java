package konopka.gerrit.search;

import com.google.gerrit.extensions.api.changes.Changes;

import javax.management.Query;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Martin on 3.7.2015.
 */
public class ChangesQueryBuilder {

    private final Changes.QueryRequest request;
    public ChangesQueryBuilder(Changes.QueryRequest request) {
        this.request = request;
        this.states = new ArrayList<>();
    }

    private final List<QueryChangeStatus> states;

//
//
//
//    public ChangesQueryBuilder or() {
//
//        return this;
//    }

    public ChangesQueryBuilder withStatus(QueryChangeStatus status) {
        if (states.contains(status) == false) {
            states.add(status);
        }
        return this;
    }

    public ChangesQueryBuilder withStatuses(QueryChangeStatus... statuses) {
        for (QueryChangeStatus status : statuses) {
            withStatus(status);
        }
        return this;
    }

//    public ChangesQueryBuilder is() {
//
//
//        return this;
//    }

//    public ChangesQueryBuilder and() {
//
//        return this;
//    }
//

//    public ChangesQueryBuilder withProject(ProjectDto project) {}

    public Changes.QueryRequest build() {
        if (states.size() > 0) {
            String query = states.stream().map(s -> "status:" + s.toString().toLowerCase()).reduce("", (a, b) -> a + " OR " + b);
            request.withQuery(query);
        }
        return request;
    }
}
