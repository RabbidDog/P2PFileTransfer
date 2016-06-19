package database;

import entity.*;

import java.util.List;

/**
 * Created by rabbiddog on 6/18/16.
 */
public interface IDataBase {
    public FileChunkInfo getChunkInfoForFileName(String fileName);
    public boolean saveFileInfo(FileChunkInfo info);
    public boolean updateChunkInfo(Chunk chunk);
    public List<Chunk> getChunksByDownloadStatus(String fileName,boolean status);

}
