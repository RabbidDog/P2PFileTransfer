import com.mongodb.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rabbiddog on 6/18/16.
 */
public class MongoDBAccessor implements IDataBase{
    private MongoClient _mongoClient;
    private Logger _log;
    private final String TAG = "MongoDBAccessor";
    private DB _db;
    private DBCollection _fileCollection;
    private DBCollection _peerInfoCollection;
    private static MongoDBAccessor ourInstance = new MongoDBAccessor();

    public static MongoDBAccessor getInstance() {
        return ourInstance;
    }

    private MongoDBAccessor() {
        _log = LogManager.getRootLogger();
        try
        {
            _mongoClient = new MongoClient( "localhost" );
            _db = _mongoClient.getDB("torrentmeta");
            _log.debug(TAG  + "Connected to database torrentmeta");

            try
            {
                _db.createCollection("fileCollection", new BasicDBObject("capped", false));
            }catch(MongoException me)
            {
                //means collection already exists
            }
            _fileCollection = _db.getCollection("fileCollection");



        }catch (UnknownHostException une)
        {
            _log.error(TAG + " constructor: "+une.getMessage() + " " + une.getStackTrace());
        }
    }

    @Override
    public ConcurrentHashMap<Long, String[]> getChunkInfoForFileName(String fileName) {
        return new ConcurrentHashMap<Long, String[]>();
    }

    @Override
    public boolean saveFileInfo() {
        return false;
    }
}
