package entity;

import java.util.List;

/**
 * Created by rabbiddog on 6/18/16.
 */
import org.mongodb.morphia.annotations.*;

@Entity()
public class FileChunkInfo {
    @Id
    public long id;
    public final String FileName;
    public byte[] fileHash;
    @Reference
    public List<Chunk> chunkInfo;
    public long size;
    public long chunkSize;
    public long chunkCount;

    public FileChunkInfo(String fileName, byte[] hash)
    {
        this.FileName = fileName;
        this.fileHash = hash;
    }

    public FileChunkInfo(String fileName)
    {
        this.FileName = fileName;
    }

    public void setChunkInfo(List<Chunk> info)
    {
        this.chunkInfo = info;
    }

}
