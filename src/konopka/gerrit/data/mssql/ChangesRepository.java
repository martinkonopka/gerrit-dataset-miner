package konopka.gerrit.data.mssql;

import konopka.gerrit.data.*;

import java.sql.*;

/**
 * Created by Martin on 25.6.2015.
 */

public class ChangesRepository extends Repository implements IChangesRepository {
    private Connection connection;

    private static final String CREATE_TABLE_CHANGES = "CREATE TABLE [Changes] (" +
            "[Id] [int] NOT NULL PRIMARY KEY," +
            "[ProjectId] [int] NOT NULL FOREIGN KEY REFERENCES [Projects]([Id])," + // FK
            "[BranchId] [int] NULL FOREIGN KEY REFERENCES [Branches]([Id])," + // FK
            "[ChangeId] [nvarchar](max) NOT NULL," +
            "[OwnerId] [int] NOT NULL FOREIGN KEY REFERENCES [Accounts]([Id])," + // FK
            "[NumberOfPatchSets] [int] NOT NULL," +
            "[Topic] [nvarchar](max) NULL," +
            "[Subject] [nvarchar](max) NULL, " +
            "[CreatedAt] [datetime] NOT NULL," +
            "[UpdatedAt] [datetime] NOT NULL," +
            "[State] [nvarchar](max) NOT NULL," +
            "[IsMergeable] [bit] NOT NULL," +
            "[BaseChangeId] [int] NULL," +
            "[CurrentPatchSetId] [nvarchar](max) NULL" +

            ");";

    private static final String INSERT_CHANGE_QUERY = "INSERT INTO [Changes] " +
            "(Id, ProjectId, BranchId, ChangeId, OwnerId, NumberOfPatchSets," +
            "Topic, Subject, CreatedAt, UpdatedAt, State, IsMergeable, BaseChangeId, " +
            "CurrentPatchSetId)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    private static final String SELECT_CHANGE_ID_QUERY = "SELECT [Id] FROM [Changes] WHERE [Id] = ?";

    private static final String CREATE_TABLE_PATCHSETS = "CREATE TABLE [PatchSets] (" +
            "[ChangeId] [int] NOT NULL," +
            "[PatchSetId] [nvarchar](max) NOT NULL," + // Compound key - identity
            "[Number] [int] NOT NULL," +
            "[GitCommitId] [nvarchar](max) NOT NULL," + // key in set
            "[Subject] [nvarchar](max) NOT NULL," +
            "[Message] [nvarchar](max) NOT NULL," +
            //"[IsDraft] [bool] NOT NULL, " +
            "[CreatedAt] [datetime] NOT NULL," +
            "[NumberOfFiles] [int] NOT NULL," +
            "[AuthorId] [int] NULL FOREIGN KEY REFERENCES [Accounts]([Id])," +
            "[CommitterId] [int] NULL FOREIGN KEY REFERENCES [Accounts]([Id])," +
            "[AddedLines] [int] NOT NULL, " +
            "[DeletedLines] [int] NOT NULL," +
            "[Ref] [nvarchar](max) NULL," +
            "CONSTRAINT [FK_PatchSets_Changes] FOREIGN KEY ([ChangeId]) REFERENCES [Changes]([Id]) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";

    private static final String INSERT_PATCHSET_QUERY = "INSERT INTO [PatchSets] " +
            "(ChangeId, PatchSetId, Number, GitCommitId, Subject, Message," +
            "CreatedAt, NumberOfFiles, AuthorId, CommitterId, AddedLines, DeletedLines, Ref) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    private static final String CREATE_TABLE_PATCHSETPARENTS = "CREATE TABLE [PatchSetParents] (" +
            "[ChildGitCommitId] [nvarchar](max) NOT NULL," +
            "[ParentGitCommitId] [nvarchar](max) NOT NULL" +
            ");";

    private static final String INSERT_PATCHSETPARENT_QUERY = "INSERT INTO [PatchSetParents] " +
            "(ChildGitCommitId, ParentGitCommitId) VALUES(?, ?);";

    private static final String CREATE_TABLE_PATCHSETAPPROVALS = "CREATE TABLE [ChangeApprovals] " +
            "(" +
            "[ValueTypeId] [int] NOT NULL," +
            "[ValueId] [smallint] NOT NULL," +
            "[ChangeId] [int] NOT NULL, " +
            "[PatchSetId] [nvarchar](max) NULL," +
            "[CommentId] [nvarchar](max) NULL," +
            "CONSTRAINT FK_ProjectApprovalValues FOREIGN KEY (ValueTypeId, ValueId) REFERENCES [ProjectApprovalValues] ([TypeId], [Value])," +
            "CONSTRAINT FK_ChangeApprovals_Changes FOREIGN KEY ([ChangeId]) REFERENCES [Changes]([ID]) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";

    private static final String INSERT_INTO_PATCHSETAPPROVALS = "INSERT INTO [ChangeApprovals] " +
            "(ValueTypeId, ValueId, ChangeId, PatchSetId, CommentId) VALUES(?, ?, ?, ?, ?);";

    private static final String CREATE_TABLE_PATCHSETFILES = "CREATE TABLE [PatchSetFiles] (" +
            "[ChangeId] [int] NOT NULL," +
            "[PatchSetId] [nvarchar](max) NOT NULL," +
            "[PatchSetFileId] [nvarchar](max) NOT NULL," +
            "[Path] [nvarchar](max) NOT NULL," +
            "[AddedLines] [int] NOT NULL, " +
            "[DeletedLines] [int] NOT NULL," +
         //   "[ChangeType] [nvarchar](max) NOT NULL" +
            "[IsBinary] [bit] NOT NULL," +
//            "[OldPath] [nvarchar](max) NULL" +
            "CONSTRAINT [FK_PatchSetFiles_Changes] FOREIGN KEY ([ChangeId]) REFERENCES [Changes]([Id]) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";

    private static final String INSERT_PATCHSETFILE_QUERY = "INSERT INTO [PatchSetFiles] " +
            "(ChangeId, PatchSetId, PatchSetFileId, Path, AddedLines," +
            "DeletedLines, IsBinary) " + // ChangeType , OldPath) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?);";

    private static final String CREATE_TABLE_COMMENTS = "CREATE TABLE [Comments] (" +
            "[ChangeId] [int] NOT NULL," +
            "[PatchSetId] [nvarchar](max) NULL," +
            "[PatchSetFileId] [nvarchar](max) NULL," +
            "[CommentId] [nvarchar](max) NOT NULL," +
            "[IsRevisionMessage] [bit] NOT NULL,"+
            "[CreatedAt] [datetime] NOT NULL," +
            "[Message] [nvarchar](max) NOT NULL," +
            "[ReplyToCommentId] [nvarchar](max) NULL," +
            "[AuthorId] [int] NULL  FOREIGN KEY REFERENCES [Accounts]([Id])," +
            "[Line] [int] NULL," +
            "[IsRange] [bit] NULL," +
            "[RangeStartLine] [int] NULL," +
            "[RangeStartCharacter] [int] NULL," +
            "[RangeEndLine] [int] NULL," +
            "[RangeEndCharacter] [int] NULL," +
            "CONSTRAINT [FK_Comments_Changes] FOREIGN KEY ([ChangeId]) REFERENCES [Changes]([Id]) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";


    private static final String INSERT_PATCHSET_MESSAGE_QUERY = "INSERT INTO [Comments] " +
            "(ChangeId, PatchSetId, CommentId," +
            "CreatedAt, Message, AuthorId, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, ?, 1)";

    private static final String INSERT_CHANGE_MESSAGE_QUERY = "INSERT INTO [Comments] " +
            "(ChangeId, CommentId," +
            "CreatedAt, Message, AuthorId, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, 1)";


    private static final String INSERT_LINE_COMMENT_QUERY = "INSERT INTO [Comments] " +
            "(ChangeId, PatchSetId, PatchSetFileId, CommentId, " +
            "CreatedAt, Message, ReplyToCommentId, AuthorId, " +
            "Line, IsRange, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0);";

    private static final String INSERT_RANGE_COMMENT_QUERY = "INSERT INTO [Comments] " +
            "(ChangeId, PatchSetId, PatchSetFileId, CommentId, " +
            "CreatedAt, Message, ReplyToCommentId, AuthorId, " +
            "RangeStartLine, RangeStartCharacter, RangeEndLine," +
            "RangeEndCharacter, IsRange, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0);";



    public ChangesRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void init() {
        executeSqlStatement(connection, CREATE_TABLE_CHANGES);
        executeSqlStatement(connection, CREATE_TABLE_PATCHSETS);
        executeSqlStatement(connection, CREATE_TABLE_PATCHSETPARENTS);
        executeSqlStatement(connection, CREATE_TABLE_PATCHSETAPPROVALS);
        executeSqlStatement(connection, CREATE_TABLE_PATCHSETFILES);
        executeSqlStatement(connection, CREATE_TABLE_COMMENTS);
    }

    private String buildPatchSetId(PatchSetDto patchSet) {
        return patchSet.change.id + "-" + patchSet.number;
    }
    private String buildPatchSetFileId(PatchSetFileDto file) {
        return buildPatchSetId(file.patchSet) + "-" + file.path;
    }

    @Override
    public boolean addChange(ChangeDto change) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_CHANGE_QUERY);
            stmt.setInt(1, change.id);
            stmt.setInt(2, change.project.id);
            if (change.branch != null) {
                stmt.setInt(3, change.branch.id);
            }
            else {
                stmt.setNull(3, Types.INTEGER); System.out.println("Null branch: " + change.id);
            }
            stmt.setString(4, change.changeId);
            stmt.setInt(5, change.owner.id);
            stmt.setInt(6, change.getNumberOfPatchSets());
            stmt.setString(7, change.topic);
            stmt.setString(8, change.subject);
            stmt.setTimestamp(9, change.createdAt);
            stmt.setTimestamp(10, change.updatedAt);
            stmt.setString(11, change.state.name().toUpperCase());
            stmt.setBoolean(12, change.isMergeable != null ? change.isMergeable : false);
            stmt.setString(13, change.baseChangeId);
            //stmt.setInt(13, 0);
            if (change.currentPatchSet != null) {
                stmt.setString(14, buildPatchSetId(change.currentPatchSet));
            }
            else {
                stmt.setString(14, null);
            }

            stmt.execute();

            change.patchSets.forEach(this::addPatchSet);

            change.approvals.forEach(this::addChangeApproval);

            change.comments.forEach(this::addComment);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }

        return false;
    }

    public boolean containsChange(int id) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(SELECT_CHANGE_ID_QUERY);
            stmt.setInt(1, id);
            ResultSet results = stmt.executeQuery();
            return results.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }
        return false;
    }

    private boolean addPatchSet(PatchSetDto patch) {

        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_PATCHSET_QUERY);
            stmt.setInt(1, patch.change.id);
            stmt.setString(2, buildPatchSetId(patch));
            stmt.setInt(3, patch.number);
            stmt.setString(4, patch.gitCommitId);
            stmt.setString(5, patch.subject);
            stmt.setString(6, patch.message);
            stmt.setTimestamp(7, patch.createdAt);
            stmt.setInt(8, patch.getNumberOfFiles());
            stmt.setInt(9, patch.author.id);
            stmt.setInt(10, patch.committer.id);
            stmt.setInt(11, patch.addedLines);
            stmt.setInt(12, patch.deletedLines);
            stmt.setString(13, patch.ref);

            stmt.execute();
            patch.parents.forEach(p -> addPatchSetParent(patch.gitCommitId, p));
            patch.files.forEach(this::addPatchSetFile);


            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;
    }

    private boolean addPatchSetParent(String child, String parent) {
        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(INSERT_PATCHSETPARENT_QUERY);
            stmt.setString(1, child);
            stmt.setString(2, parent);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;

    }

    private boolean addComment(CommentDto comment) {
        if (comment.patchSetFile != null) {
            if (comment.range != null) {
                return addPatchSetFileRangeComment(comment);
            }
            else {
                return addPatchSetFileLineComment(comment);
            }
        }
        else if (comment.patchSet != null) {
            return addPatchSetMessage(comment);
        }
        else {
            return addChangeMessage(comment);
        }
    }

    private boolean addChangeApproval(ChangeApprovalDto approval) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_INTO_PATCHSETAPPROVALS);
            stmt.setInt(1, approval.type.id);
            stmt.setShort(2, approval.value.value);
            stmt.setInt(3, approval.change.id);
            stmt.setString(4, buildPatchSetId(approval.comment.patchSet));
            stmt.setString(5, approval.comment.id);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;
    }

    private boolean addChangeMessage(CommentDto comment) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_CHANGE_MESSAGE_QUERY);
            stmt.setInt(1, comment.change.id);
            stmt.setString(3, comment.id);
            stmt.setTimestamp(4, comment.createdAt);
            stmt.setString(5, comment.message);
            if (comment.author != null) {
                stmt.setInt(6, comment.author.id);
            }
            else {
                stmt.setNull(6, Types.INTEGER);
            }

            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;
    }

    private boolean addPatchSetMessage(CommentDto comment) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_PATCHSET_MESSAGE_QUERY);
            stmt.setInt(1, comment.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(comment.patchSet));
            stmt.setString(3, comment.id);
            stmt.setTimestamp(4, comment.createdAt);
            stmt.setString(5, comment.message);
            if (comment.author != null) {
                stmt.setInt(6, comment.author.id);
            }
            else {
                stmt.setNull(6, Types.INTEGER);
            }

            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;
    }

    private boolean addPatchSetFileLineComment(CommentDto comment) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_LINE_COMMENT_QUERY);
            stmt.setInt(1, comment.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(comment.patchSet));
            stmt.setString(3, buildPatchSetFileId(comment.patchSetFile));
            stmt.setString(4, comment.id);
            stmt.setTimestamp(5, comment.createdAt);

            stmt.setString(6, comment.message);
            stmt.setString(7, comment.inReplyToCommentId);

            if (comment.author != null) {
                stmt.setInt(8, comment.author.id);
            }
            else {
                stmt.setNull(8, Types.INTEGER);
            }
            if (comment.line != null) {
                stmt.setInt(9, comment.line);
            }
            else {
                stmt.setNull(9, Types.INTEGER);
            }

            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;
    }

    private boolean addPatchSetFileRangeComment(CommentDto comment) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_RANGE_COMMENT_QUERY);
            stmt.setInt(1, comment.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(comment.patchSet));
            stmt.setString(3, buildPatchSetFileId(comment.patchSetFile));
            stmt.setString(4, comment.id);
            stmt.setTimestamp(5, comment.createdAt);

            stmt.setString(6, comment.message);
            stmt.setString(7, comment.inReplyToCommentId);

            if (comment.author != null) {
                stmt.setInt(8, comment.author.id);
            }
            else {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setInt(9, comment.range.startLine);
            stmt.setInt(10, comment.range.startCharacter);
            stmt.setInt(11, comment.range.endLine);
            stmt.setInt(12, comment.range.endCharacter);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;
    }

    private boolean addPatchSetFile(PatchSetFileDto file) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_PATCHSETFILE_QUERY);
            stmt.setInt(1, file.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(file.patchSet));
            stmt.setString(3, buildPatchSetFileId(file));
            stmt.setString(4, file.path);
            stmt.setInt(5, file.addedLines);
            stmt.setInt(6, file.deletedLines);
            stmt.setBoolean(7, file.isBinary);//changeType.name().toUpperCase());
//            stmt.setBoolean(8, file.isBinary);
//            stmt.setString(9, file.oldPath);

            stmt.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return false;

    }
}
