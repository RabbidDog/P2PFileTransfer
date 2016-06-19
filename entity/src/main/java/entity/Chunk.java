package entity;

import java.util.List;

/**
 * Created by rabbiddog on 6/18/16.
 */
import org.mongodb.morphia.annotations.*;

@Entity()
public class Chunk {
    @Id
    public long Id;
    public long offset;
    public long length;
    @Reference
    public List<Peer> peerList;
    public boolean isDownloaded;
}
