import com.mongodb.*;
import entity.FileChunkInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.UnknownHostException;
import java.util.List;

import org.mongodb.morphia.*;
import org.mongodb.morphia.query.MorphiaIterator;

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
    private final Datastore _datastore;
    private static MongoDBAccessor ourInstance = new MongoDBAccessor();

    public static MongoDBAccessor getInstance() {
        return ourInstance;
    }

    private MongoDBAccessor() {
        _log = LogManager.getRootLogger();
        _morphia = new Morphia();
        _morphia.mapPackage("entity");
        _datastore = _morphia.createDatastore(new MongoClient("localhost"), "torrentmeta");
        _datastore.ensureIndexes();
        try
        {

            /*_mongoClient = new MongoClient( "localhost" );
            _db = _mongoClient.getDB("torrentmeta");
            _log.debug(TAG  + "Connected to database torrentmeta");*/

            /*try
            {
                _db.createCollection("fileCollection", new BasicDBObject("capped", false));
            }catch(MongoException me)
            {
                //means collection already exists
            }
            _fileCollection = _db.getCollection("fileCollection");*/



        }catch (Exception une)
        {
            _log.error(TAG + " constructor: "+une.getMessage() + " " + une.getStackTrace());
        }
    }

    @Override
    public FileChunkInfo getChunkInfoForFileName(String fileName) {
        List<FileChunkInfo> info = _datastore.createQuery(FileChunkInfo.class)
                .field("FileName").equalIgnoreCase(fileName).asList();
        if(info.size() == 1)
        {
            _log.debug(TAG + " getChunkInfoForFileName: entity found for file name "+ fileName +". Returning entity");
            return info.get(0);
        }
        else
        {
            _log.error(TAG + " getChunkInfoForFileName: for file search " + fileName + " number of entries in DB is not as expected");
            return null;
        }
    }

    @Override
    public boolean saveFileInfo(FileChunkInfo info) {
        _log.debug(TAG + " saveFileInfo: call to save file by name" + info.FileName);
        _datastore.save(info);
        _log.debug(TAG + " saveFileInfo: call to save file by name" + info.FileName +" succeded");
        return true;
    }
}
