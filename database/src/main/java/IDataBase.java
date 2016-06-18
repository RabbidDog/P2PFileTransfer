import entity.*;

/**
 * Created by rabbiddog on 6/18/16.
 */
public interface IDataBase {
    public FileChunkInfo getChunkInfoForFileName(String fileName);
    public boolean saveFileInfo(FileChunkInfo info);
}
