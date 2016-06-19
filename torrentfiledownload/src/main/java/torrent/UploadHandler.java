package torrent;

import entity.Chunk;
import entity.FileChunkInfo;
import entity.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.file_operation.IFileFacade;
import pft.file_operation.PftFileManager;
import pft.frames.DataRequest;
import pft.frames.PartialUpoadRequest;
import pft.operations.SendDataResponsePacket;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by rabbiddog on 6/19/16.
 */
/*handles uploading files for each file*/
public class UploadHandler {

    private FileChunkInfo _fileChunkInfo;
    private ConcurrentHashMap<Long, byte[]> _bufferedFileData;
    private ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    private IFileFacade _fileManager;
    private Random _rand;
    private final Logger _log;
    private final String TAG = "UploadHandler : ";
    ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataRequest>> _dataRequestQueueForIdentifier;

    public UploadHandler(FileChunkInfo fileChunkInfo, String filePath, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer, Random rand, ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataRequest>> dataRequestQueueForIdentifier)
    {
        this._fileChunkInfo = fileChunkInfo;
        this._bufferedFileData =  new ConcurrentHashMap<Long, byte[]>(65536);
        this._sendBuffer = sendBuffer;
        this._fileManager = new PftFileManager(filePath);
        _log = LogManager.getRootLogger();
        _dataRequestQueueForIdentifier = dataRequestQueueForIdentifier;
    }

    public void startUpload()
    {
        /*load part of file in bufferedfile data*/
        _fileManager.bufferedRead(0, 65536, (int)_fileChunkInfo.chunkSize, _bufferedFileData);
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5,
                0L, TimeUnit.MILLISECONDS, queue);
        //new requests when already queued 5, will be clocked instead of rejecting and throwing exception.
        // Execute 5 at a time
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                // this will block if the queue is full as opposed to throwing
                try
                {
                    executor.getQueue().put(r);
                }catch (InterruptedException iex)
                {
                    _log.error(TAG + "in rejected executionhandler. " + iex.getStackTrace());
                }
            }
        });
        /*pick each chunk and its peer*/
        for (Chunk ch:_fileChunkInfo.chunkInfo)
        {
            Iterator it = ch.peerList.iterator();
            while (it.hasNext())
            {
                Peer p = (Peer)it.next();
                int identifier = _rand.nextInt();
                PartialUpoadRequest req = new PartialUpoadRequest(identifier, _fileChunkInfo.FileName, _fileChunkInfo.size, _fileChunkInfo.fileHash, ch.offset, _fileChunkInfo.chunkSize);
                /*create buffer for this request*/
                ConcurrentLinkedQueue<DataRequest> dataReqBuffer = new ConcurrentLinkedQueue<DataRequest>();
                _dataRequestQueueForIdentifier.putIfAbsent(identifier, dataReqBuffer);
                /*start a thread to process the datarequests*/
                //Runnable worker = new SendDataResponsePacket(identifier, _fileManager, ch.offset, ch., SocketAddress destination, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer, ConcurrentHashMap<Long, byte[]> bufferedFileData)

            }
        }
    }


}
