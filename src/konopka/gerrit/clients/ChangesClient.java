package konopka.gerrit.clients;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.*;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.Url;
import konopka.gerrit.data.*;
import konopka.gerrit.data.entities.*;
import konopka.gerrit.extensions.common.ChangeMessageInfoEx;
import konopka.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.events.Comment;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Martin on 27.6.2015.
 */
public class ChangesClient {
    private static final Logger logger = LoggerFactory.getLogger(ChangesClient.class);

    private final WaitCaller caller;
    private GerritApi api;
    private IChangesRepository repo;

    private ProjectsClient projects;
    private AccountsClient accounts;

    public ChangesClient(GerritApi api, WaitCaller caller, IChangesRepository repository, ProjectsClient projects, AccountsClient accounts) {
        this.api = api;
        this.repo = repository;
        this.caller = caller;

        this.projects = projects;
        this.accounts = accounts;
    }


    public List<ChangeInfo> getChangeDetails(int number) {
        List<ChangeInfo> details = null;
        String query = Integer.toString(number);
        try {
            logger.info(Logging.prepare("downloadChangeDetails", query));

            Changes.QueryRequest request = api
                    .changes()
                    .query(query)
                    .withOptions(ListChangesOption.DETAILED_LABELS,
                            ListChangesOption.ALL_REVISIONS,
                            ListChangesOption.ALL_COMMITS,
                            ListChangesOption.ALL_FILES,
                            ListChangesOption.MESSAGES,
                            ListChangesOption.CURRENT_ACTIONS,
                            ListChangesOption.DETAILED_ACCOUNTS,
                            ListChangesOption.DOWNLOAD_COMMANDS);

            details = caller.waitOrCall(request::get);
        } catch (Exception e) {
            logger.error(Logging.prepare("downloadChangeDetails", query), e);
        }

        if (details == null) {
            details = Collections.emptyList();
        }

        int count = details.size();
        if (count == 0) {
            logger.info(Logging.prepareWithPart("downloadChangeDetails", "no details", query));
        }
        else if (count > 1) {
            logger.info(Logging.prepareWithPart("downloadChangeDetails", "more than 1 detail", query));
        }

        return details;
    }

    public List<ChangeInfo> downloadChanges(String query, int start, int limit) {
        List<ChangeInfo> infos = null;
        try {
            logger.info(Logging.prepare("downloadChanges", query, Integer.toString(start), Integer.toString(limit)));

            Changes.QueryRequest request = api
                    .changes()
                    .query(query)
                    .withStart(start)
                    .withLimit(limit);

            infos = caller.waitOrCall(request::get);
        } catch (Exception e) {
            logger.error(Logging.prepare("downloadChanges", query, Integer.toString(start), Integer.toString(limit)), e);
        }
        if (infos == null || infos.size() == 0) {
            infos = Collections.emptyList();
            logger.info(Logging.prepareWithPart("downloadChanges", "no results", Integer.toString(start), Integer.toString(limit)));
        }
        else {
            List<String> results = infos.stream().map(i -> Integer.toString(i._number)).collect(Collectors.toList());

            if (results.size() > 0) {
                logger.info(Logging.prepareWithPart("downloadChanges", String.join(",", results), Integer.toString(start), Integer.toString(limit)));
            }
            else {
                logger.info(Logging.prepareWithPart("downloadChanges", "no results", Integer.toString(start), Integer.toString(limit)));
            }
        }

        return infos;
    }


    public ChangeDto saveChange(ChangeInfo info) throws RestApiException, InterruptedException {
        logger.info(Logging.prepare("saveChange", Integer.toString(info._number)));

        if (repo.containsChange(info._number)) {
            logger.info(Logging.prepareWithPart("saveChange", "skipping ", Integer.toString(info._number)));
            return null;
        }
        ProjectDto project = projects.getProject(Url.encode(info.project));
        BranchDto branch = projects.getBranch(project, info.branch);
        AccountDto owner = accounts.get(info.owner);
        ChangeDto change = new ChangeDto(info._number, project, branch, owner);

        change.changeId = info.id;
        change.topic = info.topic;
        change.subject = info.subject;
        change.createdAt = info.created;
        change.updatedAt = info.updated;
        change.state = info.status;

        change.baseChangeId = info.baseChange;
        change.isMergeable = info.mergeable;


        List<ChangeMessageInfoEx> messages = new ArrayList<>();
        if (info.messages != null) {
            int order = 0;
            for (ChangeMessageInfo message : info.messages) {
                ChangeMessageInfoEx messageEx = new ChangeMessageInfoEx(order, message);
                messages.add(messageEx);
                order += 1; // TODO use order number
            }
        }

        addChangeMessages(change, messages);


        for (Map.Entry<String, RevisionInfo> entry : info.revisions.entrySet()) {
            RevisionInfo revision = entry.getValue();
            PatchSetDto patch = new PatchSetDto(change, revision._number);
            patch.gitCommitId = entry.getKey();
            if (revision.commit == null) {
                logger.info(Logging.prepareWithPart("saveChange", "Missing commit", Integer.toString(change.id)));
                return null;
            }

            patch.subject = revision.commit.subject;
            patch.message = revision.commit.message;
            patch.createdAt = revision.created;
            if (patch.createdAt == null) {
                patch.createdAt = revision.commit.author.date;
            }
            patch.author = accounts.get(revision.commit.author.name, revision.commit.author.email);
            patch.committer = accounts.get(revision.commit.committer.name, revision.commit.committer.email);
            if (revision.files != null && revision.files.values().size() > 1) {
                patch.addedLines = revision.files.values().stream().filter(f -> f.linesInserted != null).mapToInt(f -> f.linesInserted).sum();
                patch.deletedLines = revision.files.values().stream().filter(f -> f.linesDeleted != null).mapToInt(f -> f.linesDeleted).sum();
            }
            patch.ref = revision.ref;

            if (patch.ref == null && revision.fetch != null) {
                Optional<FetchInfo> fetch = revision.fetch.values().stream().filter(e -> e.ref != null).findFirst();
                if (fetch.isPresent()){
                    patch.ref = fetch.get().ref;
                }
            }

            if (revision.commit.parents != null) {
                revision.commit.parents.stream().map(c -> c.commit).forEach(patch.parents::add);
            }


            PatchSetFileDto commitMessageFile = mapPatchSetFileDto(patch, "/COMMIT_MSG", new FileInfo());
            patch.files.add(commitMessageFile);

            if (revision.files != null) {
                revision.files.entrySet().stream().map(e -> mapPatchSetFileDto(patch, e.getKey(), e.getValue()))
                        .forEach(patch.files::add);
            }


            addPatchSetMessages(change, messages, patch);


            try {
                RevisionApi revisionApi = api.changes().id(info.id).revision(entry.getKey());

                Map<String, List<CommentInfo>> revisionComments = null;
                revisionComments = caller.waitOrCall(revisionApi::comments);

                addPatchSetComments(change, patch, revisionComments);


            } catch (com.urswolfer.gerrit.client.rest.http.HttpStatusException exception) {
                logger.info(Logging.prepareWithPart("saveChange", "comments: no comments", Integer.toString(info._number)), exception);

            } catch (Exception e) {
                logger.error(Logging.prepare("saveChange", Integer.toString(info._number)), e);
                e.printStackTrace();
            }

            change.patchSets.add(patch);

        }

        Optional<PatchSetDto> current =  change.patchSets.stream().filter(p -> p.gitCommitId.equals(info.currentRevision)).findFirst();
        if (current.isPresent()) {
            change.currentPatchSet = current.get();
        }



        if (info.labels != null && info.labels.size() > 0) {

            projects.addApprovals(project, info.labels);

            info.labels.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue().all != null && e.getValue().all.size() > 0)
                    .forEach(e -> e.getValue().all.stream().filter(l -> l.date != null).map(l -> {
                        List<CommentDto> comments = change.comments.stream().filter(m -> m.createdAt.equals(l.date) && m.author.accountId.equals(l._accountId)).collect(Collectors.toList());
                        Optional<CommentDto> comment = comments.stream().filter(c -> c.patchSet != null).findFirst();
                        if (comment.isPresent() == false) {
                            comment = comments.stream().findAny();
                        }
                        if (comment.isPresent()) {

                            //noinspection PointlessBooleanExpression
                            if (project.approvals.containsKey(e.getKey()) == false) {
                                projects.addApproval(project, e.getKey(), e.getValue());
                            }
                            ApprovalTypeDto type = project.approvals.get(e.getKey());

                            if (type != null) {
                                Optional<ApprovalValueDto> value = type.values.stream().filter(v -> Integer.compare(v.value, l.value) == 0).findFirst();
                                if (value.isPresent()) {
                                    return new ChangeApprovalDto(change, comment.get(), type, value.get());
                                }
                            }
                            else {
                                logger.error(Logging.prepareWithPart("saveChange","missing approval", e.getKey()));
                            }
                        }
                        return null;
                    }).filter(a -> a != null).forEach(change.approvals::add));
        }
        else {
            logger.info(Logging.prepareWithPart("saveChange", "no labels", Integer.toString(info._number)));
        }



        change.comments.sort((o1, o2) -> o1.createdAt.compareTo(o2.createdAt));

        repo.addChange(change);

        return change;
    }

    private void addPatchSetComments(ChangeDto change, PatchSetDto patch, Map<String, List<CommentInfo>> revisionComments) {
        if (revisionComments.size() > 0) {
            revisionComments.entrySet().forEach(fc -> {
                Optional<PatchSetFileDto> opt = patch.files.stream().filter(f -> f.path.equals(fc.getKey())).findFirst();
                if (opt.isPresent()) {
                    PatchSetFileDto file = opt.get();
                    for (CommentInfo ci : fc.getValue()) {
                        CommentDto comment = new CommentDto(file, ci.id);
                        comment.inReplyToCommentId = ci.inReplyTo;
                        comment.createdAt = ci.updated;
                        comment.message = ci.message;
                        comment.author = accounts.get(ci.author);
                        comment.line = ci.line;
                        comment.range = ci.range;
                        change.comments.add(comment);
                    }
                } else {
                    logger.info(Logging.prepareWithPart("saveChange", "comments: file not found", Integer.toString(change.id)));
                }
            });
        }
    }

    private void addPatchSetMessages(ChangeDto change, List<ChangeMessageInfoEx> messages, PatchSetDto patch) {
        if (messages.size() > 0) {
            messages.stream()
                    .filter(m -> m != null && m._revisionNumber != null && m._revisionNumber.equals(patch.number)).forEach(m ->
            {
                CommentDto comment = new CommentDto(patch, m.id);
                comment.createdAt = m.date;
                comment.message = m.message;

                if (m.author != null) {
                    comment.author = accounts.get(m.author);
                }
                change.comments.add(comment);
            });
        }
    }

    private void addChangeMessages(ChangeDto change, List<ChangeMessageInfoEx> messages) {
        if (messages.size() > 0) {
            messages.stream().filter(m -> m != null && m._revisionNumber == null).forEach(m -> {
                CommentDto comment = new CommentDto(change, m.id);
                comment.createdAt = m.date;
                comment.message = m.message;

                if (m.author != null) {
                    comment.author = accounts.get(m.author);
                }
                change.comments.add(comment);
            });
        }
    }

    protected static PatchSetFileDto mapPatchSetFileDto(PatchSetDto patch, String path, FileInfo info) {
        PatchSetFileDto file = new PatchSetFileDto(patch);
        file.path = path;
        if (info != null) {
            file.addedLines = info.linesInserted != null ? info.linesInserted : 0;
            file.deletedLines = info.linesDeleted != null ? info.linesDeleted : 0;
            file.isBinary = info.binary != null ? info.binary : false;
            file.oldPath = info.oldPath;
        }
        return file;
    }

//    protected static PatchSetFileDto mapPatchSetFileDto(RevisionApi revisionApi, PatchSetDto patch, String path) {
//        PatchSetFileDto file = new PatchSetFileDto(patch);
//        file.path = path;
//       // try {
//         //   DiffInfo diff = revisionApi.file(file.path).diff();
//            file.changeType = ChangeType.MODIFIED;//diff.changeType;
//            file.isBinary = false;//diff.binary != null ? diff.binary : false;
//            return file;
////        } catch (RestApiException e1) {
////            e1.printStackTrace();
////        }
//        //return null;
//    }

}
