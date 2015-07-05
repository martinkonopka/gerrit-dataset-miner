//package konopka.gerrit.data.entities;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by Martin on 2.7.2015.
// */
//public class FetchChangeOptionDto {
//    public int id;
//    public final ProjectDto project;
//    public final String type;
//    public final String url;
//
//    public FetchChangeOptionDto(ProjectDto project, String type, String url) {
//        this.project = project;
//        this.url = url;
//        this.type = type;
//
//        this.commandTemplates = new ArrayList<>();
//    }
//
//    public FetchChangeOptionDto(int id, ProjectDto project, String type, String url) {
//        this(project, type, url);
//        this.id = id;
//    }
//
//    public final List<FetchCommandTemplateDto> commandTemplates;
//}
//
