package torrent;

import entity.Chunk;
import entity.FileChunkInfo;
import entity.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pft.Framer;
import pft.file_operation.IFileFacade;
import pft.file_operation.PftFileManager;
import pft.frames.DataRequest;
import pft.frames.DataResponse;
import pft.frames.DownloadRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.javatuples.*;
import pft.frames.Frame;
import database.*;
import pft.operations.ProcessDataResponsePacket;
import pft.operations.ResendDataRequestPacket;
import pft.operations.SendDataRequestPacket;

/**
 * Created by ankur on 18.06.2016.
 */
public class DownloadHandler implements RunnableFuture {
    String fileName;
    byte[] sha;
    private final int THREAD_POOL_SIZE = 5;
    private ExecutorService downloadChunkThread = null;
    private ConcurrentLinkedQueue<Frame> receivingBuffer = new ConcurrentLinkedQueue<>();
    private volatile Iterator it;
    private IDataBase _db;
    private FileChunkInfo _fFileChunkInfo;
    private Logger _log;
    private Random _rand;
    private ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataResponse>> _dataResponseQueueForIdentifier;
    private final String TAG = "DownloadHandler";


    public DownloadHandler(String fileName, byte[] sha, Random rand, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer, ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataResponse>> dataResponseQueueForIdentifier)
    {
        this.fileName = fileName;
        this.sha = sha;
        this.downloadChunkThread = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this._log = LogManager.getRootLogger();
        this._db = MongoDBAccessor.getInstance();
        this._rand = rand;
        this._sendBuffer = sendBuffer;
        this._dataResponseQueueForIdentifier = dataResponseQueueForIdentifier;
    }
    /*call db each time this method is called so that updated information is received*/
    private List<Chunk> getChunkPeerHashMap()
    {
        FileChunkInfo fileChunkInfo = _db.getChunkInfoForFileName(fileName);
        return fileChunkInfo.chunkInfo;
    }

   /* private ConcurrentHashMap<String, Integer> getPeerHostPortMap(){
        ConcurrentHashMap<String, Integer> peerHostPortMap = null;
        return  peerHostPortMap;
    }*/

    /*public void sendDownloadRequest() throws UnknownHostException {
        ConcurrentLinkedQueue<Pair<ByteBuffer, SocketAddress>> downloadRequestQueue = new ConcurrentLinkedQueue<>();
        ByteBuffer buffer = generateDownloadRequest();
        ConcurrentHashMap<String, Integer> peerHostPortMap = getPeerHostPortMap();
        it = peerHostPortMap.entrySet().iterator();
        while(it.hasNext()){
            ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry)it.next();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), (int)pair.getValue());
            downloadRequestQueue.add(Pair.with(buffer,inetSocketAddress));
        }

    }*/
    /*public void startDownload(){
        ConcurrentHashMap<Long, String[]> chunkPeersHashMap = getChunkPeerHashMap();
        it = chunkPeersHashMap.entrySet().iterator();
        if(it.hasNext()){
            while(it.hasNext()){
                ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry)it.next();
                downloadChunkThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        downloadHandler(pair, Thread.currentThread());
                    }
                });
            }
        }


    }*/
    private void downloadHandler(Map.Entry pair, Thread thread) {
        long offset = (long) pair.getKey();
        String[] peers = (String[]) pair.getValue();
        //Generate a dataRequest and send it to the peer
    }

    private ByteBuffer generateDownloadRequest(){
        byte[] payload;
        Framer framer = new Framer();
        DownloadRequest downloadRequest = new DownloadRequest(fileName,sha);
        payload = framer.frame(downloadRequest);
        ByteBuffer buffer = ByteBuffer.allocate(payload.length);
        buffer.put(payload);
        return buffer;
    }


    @Override
    public void run() {
        /*load file data from the db*/
        _fFileChunkInfo = _db.getChunkInfoForFileName(fileName);
        if(null == _fFileChunkInfo)
        {
            _log.error(TAG + " run: filechunkinfo for filename " + fileName +" returned null");
            return;
        }
        IFileFacade fileManager = new PftFileManager(TorrentApplication._mainFolder+fileName);
        /*choose a chunk to download*/
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
            ExecutorService executorService = new ThreadPoolExecutor(15, 15,
                0L, TimeUnit.MILLISECONDS, queue); //new requests when already queued 100, will be rejected. Execute 10 at a time
        boolean stillChunkDownLoadPossible =  true;
        do{
            /*choose a chunk that is pending
            * change its status to downloaded
            * start download
            * if download completes, save status to DB
            * else change status back to not downloaded
            * send update to Tracker
            * */
            Iterator it = _fFileChunkInfo.chunkInfo.iterator();
            Chunk nextChunkToDownload = null;
            while (it.hasNext())
            {
                nextChunkToDownload = null;
                Chunk ch = (Chunk) it.next();
                if(!ch.isDownloaded)
                {
                    nextChunkToDownload = ch;
                    break;
                }
            }
            if(null != nextChunkToDownload)
            {
                /*change status*/
                nextChunkToDownload.isDownloaded = true;
                /*start download*/
                /*unique identifier*/
                int identifier = _rand.nextInt();
                /*choose a peer*/
                SocketAddress source;
                Iterator itPeer = nextChunkToDownload.peerList.iterator();
                if(itPeer.hasNext())
                {
                    Peer p = (Peer)itPeer.next();
                    source = new InetSocketAddress(p.address, p.port);
                }
                else
                {
                    _log.debug(TAG + " No peers found for chunk "+ nextChunkToDownload.offset);
                    continue;
                }

                ConcurrentHashMap<Long, Pair< ByteBuffer, SocketAddress >> pendingPackets = new ConcurrentHashMap<Long, Pair< ByteBuffer, SocketAddress >>();
                ConcurrentLinkedQueue<DataResponse> dataRespBuff = new ConcurrentLinkedQueue<DataResponse>();
                _dataResponseQueueForIdentifier.putIfAbsent(identifier, dataRespBuff);
                AtomicLong currentOffset = new AtomicLong(0);
                AtomicLong highestOffsetReceived = new AtomicLong(0);
                Future sender =  executorService.submit(new SendDataRequestPacket(identifier, _fFileChunkInfo.FileName, _fFileChunkInfo.fileHash, nextChunkToDownload.offset, nextChunkToDownload.length, source, pendingPackets, _sendBuffer, currentOffset));
                Future resender = executorService.submit(new ResendDataRequestPacket(identifier, _fFileChunkInfo.FileName, source, currentOffset, nextChunkToDownload.length, highestOffsetReceived, pendingPackets,  _sendBuffer));
                Future processor = executorService.submit(new ProcessDataResponsePacket(identifier, _fFileChunkInfo.FileName, currentOffset, nextChunkToDownload.length, highestOffsetReceived, dataRespBuff, pendingPackets,source, _sendBuffer, fileManager));

                //executorService.submit();
            }
            else {
                _log.debug(TAG + " run: stopping to add further chunk download threads as there are currently no free chunks");
                stillChunkDownLoadPossible = false;
            }
        }while(stillChunkDownLoadPossible);


    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Object get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
