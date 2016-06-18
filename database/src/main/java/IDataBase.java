import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rabbiddog on 6/18/16.
 */
public interface IDataBase {
    public ConcurrentHashMap<Long, String[]> getChunkInfoForFileName(String fileName);
    public boolean saveFileInfo();
}
