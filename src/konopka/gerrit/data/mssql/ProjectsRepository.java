package konopka.gerrit.data.mssql;

import konopka.gerrit.data.*;
import konopka.gerrit.data.cache.ProjectsCache;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectsRepository extends Repository implements IProjectsRepository {

    private final static String CREATE_TABLE_QUERY = "CREATE TABLE [Projects] " +
            "(" +
            "[Id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY," +
            "[ProjectId] [nvarchar](max) NOT NULL," +
            "[Name] [nvarchar](max) NOT NULL," +
            "[ParentId] [int] NULL" +
            ");";

    private final static String CREATE_BRANCH_TABLE_QUERY = "CREATE TABLE [Branches] " +
            "(" +
            "[Id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY," +
            "[ProjectId] [int] NOT NULL," +
            "[Name] [nvarchar](max) NOT NULL," +
            "[Revision] [nvarchar](max) NOT NULL," +
            "CONSTRAINT [FK_Branches_Projects]  FOREIGN KEY ([ProjectId]) REFERENCES [Projects] ([Id]) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";

    private final static String CREATE_APPROVALTYPE_TABLE_QUERY = "CREATE TABLE [ProjectApprovalTypes] " +
            "(" +
            "[Id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
            "[ProjectId] [int] NOT NULL," +
            "[Name] [nvarchar](max) NOT NULL," +
            "[DefaultValue] [smallint] NOT NULL," +
            "CONSTRAINT [FK_ProjectApprovalTypes_Projects]  FOREIGN KEY ([ProjectId]) REFERENCES [Projects] ([Id]) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";

    private final static String CREATE_APPROVALVALUE_TABLE_QUERY = "CREATE TABLE [ProjectApprovalValues] " +
            "(" +
            "[TypeId] [int] NOT NULL, " +
            "[Value] [smallint] NOT NULL," +
            "[Description] [nvarchar](max) NOT NULL," +
            "PRIMARY KEY ( [TypeId], [Value] )," +
            "CONSTRAINT [FK_ProjectApprovalValues_ProjectApprovalTypes] FOREIGN KEY ([TypeId]) REFERENCES [ProjectApprovalTypes] ([Id]) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");";

    private final static String INSERT_APPROVALTYPE_QUERY = "INSERT INTO [ProjectApprovalTypes]" +
            "(ProjectId, Name, DefaultValue) Values(?, ?, ?);";

    private final static String INSERT_APPROVALVALUE_QUERY = "INSERT INTO [ProjectApprovalValues] " +
            "(TypeId, Value, Description) VALUES(?, ?, ?);";


    private final static String ADD_FOREIGNKEYS_QUERY = "ALTER TABLE [Projects] WITH CHECK ADD CONSTRAINT [FK_Projects_Project_ParentId_Id] FOREIGN KEY([ParentId]) " +
            "REFERENCES [Projects] ([Id]) " +
            "ON DELETE NO ACTION;";
    private final static String ALTER_TABLE_QUERY = "ALTER TABLE [Projects] CHECK CONSTRAINT [FK_Projects_Project_ParentId_Id];";

    private final static String INSERT_PROJECT_QUERY = "INSERT INTO [Projects] (" +
            "[ProjectId], " +
            "[Name], " +
            "[ParentId]" +
            ") VALUES(?, ?, ?);";


    private final static String SELECT_PROJECTS_QUERY = "SELECT [Id], [ProjectId], [Name], [ParentId] FROM [Projects]";
    private final static String SELECT_BRANCHES_QUERY = "SELECT [Id], [Name], [Revision] FROM [Branches] WHERE [ProjectId] = ?";
    private final static String SELECT_APPROVALTYPES_QUERY = "SELECT [Id], [Name], [DefaultValue] FROM [ProjectApprovalTypes] WHERE [ProjectId] = ?";
    private final static String SELECT_APPROVALVALUES_QUERY = "SELECT [Value], [Description] FROM [ProjectApprovalValues] WHERE [TypeId] = ?";

    private Connection connection;

    public ProjectsRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void init() {

        if (executeSqlStatement(connection, CREATE_TABLE_QUERY)) {
            executeSqlStatement(connection, ADD_FOREIGNKEYS_QUERY);
            executeSqlStatement(connection, ALTER_TABLE_QUERY);
        }
        executeSqlStatement(connection, CREATE_BRANCH_TABLE_QUERY);
        executeSqlStatement(connection, CREATE_APPROVALTYPE_TABLE_QUERY);
        executeSqlStatement(connection, CREATE_APPROVALVALUE_TABLE_QUERY);
    }



    public List<ProjectDto> getAllProjects() {
        List<ProjectDto> projects = new ArrayList<>();

        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            ResultSet results = stmt.executeQuery(SELECT_PROJECTS_QUERY);
            while (results.next()) {
                ProjectDto project = new ProjectDto(
                        results.getInt("Id"),
                        results.getString("ProjectId"),
                        results.getString("Name"));

                project.parentId = Optional.of(results.getInt("ParentId"));
                if (results.wasNull()) {
                    project.parentId = Optional.empty();
                }

                getBranches(project).forEach(project.branches::add);
                getApprovals(project).forEach(a -> project.approvals.put(a.name, a));
                projects.add(project);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return projects;
    }

    public List<BranchDto> getBranches(ProjectDto project) {
        PreparedStatement stmt = null;
        List<BranchDto> branches = new ArrayList<>();
        try {
            stmt = connection.prepareStatement(SELECT_BRANCHES_QUERY);
            stmt.setInt(1, project.id);
            ResultSet results = stmt.executeQuery();

            while (results.next()) {
                BranchDto branch = new BranchDto();
                branch.id = results.getInt("Id");
                branch.name = results.getString("Name");
                branch.revision = results.getString("Revision");
                branch.projectId = project.id;

                branches.add(branch);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return branches;
    }

    public List<ApprovalTypeDto> getApprovals(ProjectDto project) {
        PreparedStatement stmt = null;
        List<ApprovalTypeDto> approvals = new ArrayList<>();
        try {
            stmt = connection.prepareStatement(SELECT_APPROVALTYPES_QUERY);

            stmt.setInt(1, project.id);
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                int id = results.getInt("Id");
                List<ApprovalValueDto> values = getApprovalValues(id);

                ApprovalTypeDto approval = new ApprovalTypeDto(results.getInt("Id"), project, results.getString("Name"), results.getShort("DefaultValue"), values);

                approvals.add(approval);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }
        return approvals;
    }

    public List<ApprovalValueDto> getApprovalValues(int typeId) {
        PreparedStatement stmt = null;
        List<ApprovalValueDto> values = new ArrayList<>();
        try {
            stmt = connection.prepareStatement(SELECT_APPROVALVALUES_QUERY);

            stmt.setInt(1, typeId);
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                ApprovalValueDto value = new ApprovalValueDto(results.getShort("Value"), results.getString("Description"));
                values.add(value);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }
        return values;
    }


    @Override
    public ProjectDto add(ProjectDto project) {
        PreparedStatement stmt = null;
        try {

            stmt = connection.prepareStatement(INSERT_PROJECT_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, project.projectId);
            stmt.setString(2, project.name);

            if (project.parentId.isPresent()) {
                stmt.setInt(3, project.parentId.get());
            }
            else {
                stmt.setNull(3, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    project.id = generatedKeys.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }

        return project;
    }

    private static final String INSERT_BRANCH_QUERY = "INSERT INTO [Branches] ([ProjectId], [Name], [Revision]) VALUES(?,?,?)";

    public BranchDto addBranch(ProjectDto project, String ref, String revision) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_BRANCH_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, project.id);
            stmt.setString(2, ref);
            stmt.setString(3, revision);

            BranchDto branchdto = new BranchDto();
            branchdto.name = ref;
            branchdto.revision = revision;
            branchdto.projectId = project.id;

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    branchdto.id = generatedKeys.getInt(1);
                }
            }

            return branchdto;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }

        return null;
    }

    @Override
    public ApprovalTypeDto addApprovalType(ApprovalTypeDto approval) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_APPROVALTYPE_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, approval.project.id);
            stmt.setString(2, approval.name);
            stmt.setShort(3, approval.defaultValue);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    approval.id = generatedKeys.getInt(1);
                }
            }

            approval.values.forEach(v -> addApprovalValue(approval.id, v));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
        return approval;
    }

    private void addApprovalValue(int id, ApprovalValueDto value) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(INSERT_APPROVALVALUE_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, id);
            stmt.setShort(2, value.value);
            stmt.setString(3, value.description);

            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeStatement(stmt);
        }
    }



}
