package torrent;

import pft.Framer;
import pft.frames.DataRequest;
import pft.frames.DownloadRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.javatuples.*;
import pft.frames.Frame;

/**
 * Created by ankur on 18.06.2016.
 */
public class DownloadHandler {
    String fileName;
    byte[] sha;
    private final int THREAD_POOL_SIZE = 5;
    private ExecutorService downloadChunkThread = null;
    private ConcurrentLinkedQueue<Frame> receivingBuffer = new ConcurrentLinkedQueue<>();
    private volatile Iterator it;
    public DownloadHandler(String fileName, byte[] sha){
        this.fileName = fileName;
        this.sha = sha;
        this.downloadChunkThread = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    }
    private ConcurrentHashMap getChunkPeerHashMap(){
        //chunkPeersHashMap = some Method to get the hashMap from the database
        ConcurrentHashMap<Long, String[]> chunkPeersHashMap = null; // = puth logic here
        return chunkPeersHashMap;
    }

    private ConcurrentHashMap<String, Integer> getPeerHostPortMap(){
        ConcurrentHashMap<String, Integer> peerHostPortMap = null;
        return  peerHostPortMap;
    }

    public void sendDownloadRequest() throws UnknownHostException {
        ConcurrentLinkedQueue<Pair<ByteBuffer, SocketAddress>> downloadRequestQueue = new ConcurrentLinkedQueue<>();
        ByteBuffer buffer = generateDownloadRequest();
        ConcurrentHashMap<String, Integer> peerHostPortMap = getPeerHostPortMap();
        it = peerHostPortMap.entrySet().iterator();
        while(it.hasNext()){
            ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry)it.next();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), (int)pair.getValue());
            downloadRequestQueue.add(Pair.with(buffer,inetSocketAddress));
        }

    }
    public void startDownload(){
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


    }
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


}
