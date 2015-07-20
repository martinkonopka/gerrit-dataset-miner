package konopka.gerrit.data;

import konopka.gerrit.data.entities.ChangeDownloadDto;

/**
 * Created by Martin on 17.7.2015.
 */
public interface IDownloadsRepository extends IRepository {
    boolean addDownload(ChangeDownloadDto download);

    ChangeDownloadDto getDownload(int changeId);

    void saveDownload(ChangeDownloadDto download);
}
