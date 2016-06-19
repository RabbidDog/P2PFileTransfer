package entity;

/**
 * Created by rabbiddog on 6/18/16.
 */
import org.mongodb.morphia.annotations.*;

@Entity()
public class Peer {
    @Id
    public long Id;
    public String address;
    public int port;
}
