//package konopka.gerrit.data.mssql;
//
//import konopka.gerrit.data.entities.EndpointDto;
//import konopka.gerrit.data.IEndpointsRepository;
//import konopka.gerrit.data.Repository;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by Martin on 2.7.2015.
// */
//public class EndpointsRepository extends Repository implements IEndpointsRepository {
//
//    private static final String CREATE_TABLE = "CREATE TABLE [Endpoints] " +
//            "(" +
//            "[Id] [int] IDENTITY(1,1) NOT NULL," +
//            "[Name] [nvarchar](max) NOT NULL," +
//            "[Url] [nvarchar](max) NOT NULL" +
//            ");";
//
//    private static final String INSERT_QUERY = "INSERT INTO [Endpoints] " +
//            "([Name], [Url]) VALUES(?, ?);";
//
//            private static final String SELECT_QUERY = "SELECT [Id], [Name], [Url] FROM [Endpoints]";
//    private Connection connection;
//
//    public EndpointsRepository(Connection connection) {
//        this.connection = connection;
//    }
//
//
//    @Override
//    public void init()
//    {
//        executeSqlStatement(connection, CREATE_TABLE);
//    }
//
//    public List<EndpointDto> getAll() {
//        List<EndpointDto> endpoints = new ArrayList<>();
//        Statement stmt = null;
//        try {
//            stmt = connection.createStatement();
//            ResultSet results = stmt.executeQuery(SELECT_QUERY);
//            while (results.next()) {
//                endpoints.add(new EndpointDto(results.getInt("Id"), results.getString("Name"), results.getString("Url")));
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            closeStatement(stmt);
//        }
//        return endpoints;
//    }
//
//    public EndpointDto add(String name, String url) {
//        PreparedStatement stmt = null;
//        EndpointDto endpoint = new EndpointDto(name, url);
//        try {
//            stmt = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
//            stmt.setString(1, name);
//            stmt.setString(2, url);
//
//            int affectedRows = stmt.executeUpdate();
//
//            if (affectedRows > 0) {
//                ResultSet generatedKeys = stmt.getGeneratedKeys();
//                if (generatedKeys.next()) {
//                    endpoint.id = generatedKeys.getInt(1);
//                }
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            closeStatement(stmt);
//        }
//
//        return endpoint;
//    }
//}
