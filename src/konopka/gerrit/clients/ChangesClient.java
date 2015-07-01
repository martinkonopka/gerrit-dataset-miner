package konopka.gerrit.clients;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.*;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.Url;
import konopka.gerrit.data.*;
import konopka.gerrit.extensions.common.ChangeMessageInfoEx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Martin on 27.6.2015.
 */
public class ChangesClient {

    private final IAccountsRepository accountsRepo;
    private GerritApi api;
    private IChangesRepository repo;

    private ProjectsClient projects;
    private AccountsClient accounts;
    public ChangesClient(GerritApi api, IChangesRepository repository, ProjectsClient projects, AccountsClient accounts, IAccountsRepository accountsRepo) {
        this.api = api;
        this.repo = repository;

        this.projects = projects;
        this.accounts = accounts;
        this.accountsRepo = accountsRepo;
    }



    public void get(ProjectDto project, int totalLimit, int sleep) throws InterruptedException {
        int start = 0;
        int limit = 100;
        try {
            while (start + limit <= totalLimit) {
                List<ChangeInfo> infos = api.changes().query("p:" + project.projectId)
                        .withStart(start)
                        .withLimit(limit)
                        .withOptions(ListChangesOption.DETAILED_LABELS,
                                ListChangesOption.ALL_REVISIONS,
                              //  ListChangesOption.DRAFT_COMMENTS,
                                ListChangesOption.ALL_COMMITS,
                                ListChangesOption.ALL_FILES,
                                ListChangesOption.MESSAGES,
                                ListChangesOption.CURRENT_ACTIONS,
                                ListChangesOption.DETAILED_ACCOUNTS
                                //        ListChangesOption.REVIEWED
                        ).get();

                if (infos == null || infos.size() <= 0) {
                    System.out.println("empty results  " + start + " " + limit);
                    return;
                }
                for (ChangeInfo info : infos) {
                    saveChanges(info);

                    Thread.sleep(sleep);
                }

                start += limit;
            }
        } catch (RestApiException e) {
            e.printStackTrace();
        }
    }



    public void get(int start, int limit, int totalLimit, int sleep) throws InterruptedException {
        try {
            while (start + limit <= totalLimit) {
                List<ChangeInfo> infos = api.changes().query("status:reviewed")
                        .withStart(start)
                        .withLimit(limit)
                        .withOptions(ListChangesOption.DETAILED_LABELS,
                                ListChangesOption.ALL_REVISIONS,
                                //  ListChangesOption.DRAFT_COMMENTS,
                                ListChangesOption.ALL_COMMITS,
                                ListChangesOption.ALL_FILES,
                                ListChangesOption.MESSAGES,
                                ListChangesOption.CURRENT_ACTIONS,
                                ListChangesOption.DETAILED_ACCOUNTS
                                //        ListChangesOption.REVIEWED
                        ).get();

                if (infos == null || infos.size() <= 0) {
                    System.out.println("empty results  " + start + " " + limit);

                    return;

                }

                for (ChangeInfo info : infos) {

                        saveChanges(info);


                    Thread.sleep(sleep);
                }

                start += limit;
            }
        } catch (RestApiException e) {
            e.printStackTrace();
        }
    }

    protected void saveChanges(ChangeInfo info) throws RestApiException, InterruptedException {
        System.out.println("Change: " + info._number);

        if (repo.containsChange(info._number)) {
            System.out.println("Skipping change: " + info._number);
            return;
        }
            List<CommentDto> allMessages = new ArrayList<>();


            ProjectDto project = projects.getProject(Url.encode(info.project));
            BranchDto branch = project.getBranch(info.branch);
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


            messages.stream().filter(m -> m != null && m._revisionNumber == null).map(m ->
                    {
                        CommentDto comment = new CommentDto(change, m.id);
                        comment.createdAt = m.date;
                        comment.message = m.message;

                        if (m.author != null) {
                            comment.author = accounts.get(m.author);
                        }
                        return comment;
                    }
            ).forEach(change.comments::add);


        for (Map.Entry<String, RevisionInfo> entry : info.revisions.entrySet()) {
                RevisionInfo revision = entry.getValue();
                PatchSetDto patch = new PatchSetDto(change, revision._number);
                patch.gitCommitId = entry.getKey();
            if (revision.commit == null) {
                System.out.println("Missing commit, skipping.");
                return;
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



                RevisionApi revisionApi = api.changes().id(info.id).revision(entry.getKey());

                PatchSetFileDto[] files = revision.files.entrySet().stream().map(e ->
                                mapPatchSetFileDto(revisionApi, patch, e.getKey(), e.getValue())
                ).toArray(PatchSetFileDto[]::new);
                PatchSetFileDto commitMessageFile = mapPatchSetFileDto(revisionApi, patch, "/COMMIT_MSG", new FileInfo());

                if (commitMessageFile != null) {
                    patch.files.add(commitMessageFile);
                }

                for (PatchSetFileDto file : files) {
                    patch.files.add(file);
                }


               messages.stream().filter(m -> m != null && m._revisionNumber != null && m._revisionNumber.equals(patch.number)).map(m ->
                {
                    CommentDto comment = new CommentDto(patch, m.id);
                    comment.createdAt = m.date;
                    comment.message = m.message;

                    if (m.author != null) {
                        comment.author = accounts.get(m.author);
                    }
                    return comment;
                }
                ).forEach(change.comments::add);

                try {
                    Map<String, List<CommentInfo>> revisionComments = revisionApi.comments();

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
                            System.out.println("ERROR: File not found for comment: " + fc.getKey());
                        }
                    });


                }catch (com.urswolfer.gerrit.client.rest.http.HttpStatusException exception) {
                    System.out.println("no comments");
                    exception.printStackTrace();
                }
                change.patchSets.add(patch);
            }

            Optional<PatchSetDto> current =  change.patchSets.stream().filter(p -> p.gitCommitId.equals(info.currentRevision)).findFirst();
            if (current.isPresent()) {
                change.currentPatchSet = current.get();
            }

            if (info.labels != null && info.labels.size() > 0) {
                info.labels.entrySet().stream()
                        .filter(e -> e.getValue() != null && e.getValue().all != null && e.getValue().all.size() > 0)
                        .forEach(e -> e.getValue().all.stream().filter(l -> l.date != null).map(l -> {
                            Optional<CommentDto> comment = allMessages.stream().filter(m -> m.createdAt.equals(l.date) && m.author.accountId.equals(l._accountId)).findFirst();
                            if (comment.isPresent()) {
                                ApprovalTypeDto type = project.approvals.get(e.getKey());
                                Optional<ApprovalValueDto> value = type.values.stream().filter(v -> Integer.compare(v.value, l.value) == 0).findFirst();
                                if (value.isPresent()) {
                                    ChangeApprovalDto approval = new ChangeApprovalDto(change, comment.get(), type, value.get());
                                    return approval;
                                }
                            }
                            return null;
                        }).filter(a -> a != null).forEach(change.approvals::add));
            }
            else {
                System.out.println("patch without labels: " + info._number);
            }



            change.comments.sort((o1, o2) -> o1.createdAt.compareTo(o2.createdAt));

            repo.addChange(change);


    }

    protected static PatchSetFileDto mapPatchSetFileDto(RevisionApi revisionApi, PatchSetDto patch, String path, FileInfo info) {

        PatchSetFileDto file = new PatchSetFileDto(patch);
        file.path = path;
        if (file != null && info != null) {
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
