package entity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rabbiddog on 6/18/16.
 */
public class FileChunkInfo {
    public final String FileName;
    public byte[] fileHash;
    public ConcurrentHashMap<Long, String[]> chunkInfo;

    public FileChunkInfo(String fileName, byte[] hash)
    {
        this.FileName = fileName;
        this.fileHash = hash;
    }

    public FileChunkInfo(String fileName)
    {
        this.FileName = fileName;
    }

    public void setChunkInfo(ConcurrentHashMap<Long, String[]> info)
    {
        this.chunkInfo = info;
    }

}
