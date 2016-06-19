package database;

import com.mongodb.*;
import entity.Chunk;
import entity.FileChunkInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import org.mongodb.morphia.*;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by rabbiddog on 6/18/16.
 */
public class MongoDBAccessor implements IDataBase{
    private MongoClient _mongoClient;
    private Logger _log;
    private final String TAG = "database.MongoDBAccessor";
    private DB _db;
    private DBCollection _fileCollection;
    private DBCollection _peerInfoCollection;
    private final Morphia _morphia;
    private final Datastore _datastore;
    private static MongoDBAccessor ourInstance;

    public static MongoDBAccessor getInstance() {
        return ourInstance;
    }
    public static MongoDBAccessor getInstance(String peerName){
        if(null == ourInstance)
        {
            ourInstance = new MongoDBAccessor(peerName);
        }
        return ourInstance;
    }

    private MongoDBAccessor(String peerName) {
        _log = LogManager.getRootLogger();
        _morphia = new Morphia();
        _morphia.mapPackage("entity");
        _datastore = _morphia.createDatastore(new MongoClient("localhost"), "torrentmeta"+peerName);
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

    @Override
    public boolean updateChunkInfo(Chunk chunk) {
        Query<Chunk> ch = _datastore.createQuery(Chunk.class)
                .filter("Id ==", chunk.Id );
        UpdateOperations<Chunk> up = _datastore.createUpdateOperations(Chunk.class)
                .set("isDownloaded", chunk.isDownloaded).set("peerList", chunk.peerList);

        UpdateResults results = _datastore.update(ch, up);
        return true;
    }

    @Override
    public List<Chunk> getChunksByDownloadStatus(String fileName, boolean status) {
        throw new NotImplementedException();
    }
}
