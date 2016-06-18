import com.mongodb.*;
import entity.FileChunkInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.UnknownHostException;

import org.mongodb.morphia.*;

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
    private final Morphia _morphia;
    private static MongoDBAccessor ourInstance = new MongoDBAccessor();

    public static MongoDBAccessor getInstance() {
        return ourInstance;
    }

    private MongoDBAccessor() {
        _log = LogManager.getRootLogger();
        _morphia = new Morphia();
        try
        {
            _morphia.mapPackage("entity");
            final Datastore datastore = _morphia.createDatastore(new MongoClient("localhost"), "torrentmeta");
            datastore.ensureIndexes();
            /*_mongoClient = new MongoClient( "localhost" );
            _db = _mongoClient.getDB("torrentmeta");
            _log.debug(TAG  + "Connected to database torrentmeta");*/

            try
            {
                _db.createCollection("fileCollection", new BasicDBObject("capped", false));
            }catch(MongoException me)
            {
                //means collection already exists
            }
            _fileCollection = _db.getCollection("fileCollection");



        }catch (Exception une)
        {
            _log.error(TAG + " constructor: "+une.getMessage() + " " + une.getStackTrace());
        }
    }

    @Override
    public FileChunkInfo getChunkInfoForFileName(String fileName) {

    }

    @Override
    public boolean saveFileInfo(FileChunkInfo info) {
        
        return false;
    }
}
