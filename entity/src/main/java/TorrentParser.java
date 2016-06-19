import entity.FileChunkInfo;
import org.w3c.dom.Entity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ankur on 18.06.2016.
 */
public class TorrentParser {
    private String torrentFilePath;

    public TorrentParser(String torrentFilePath){
        this.torrentFilePath = torrentFilePath;
    }
    public FileChunkInfo fileParser() throws IOException {
        RandomAccessFile torrentFile = new RandomAccessFile(this.torrentFilePath, "r");
        byte[] sha = new byte[20];
        ConcurrentHashMap<Long, String[]> chunkPeerMap = new ConcurrentHashMap<Long, String[]>();

        String tempPeer;
        String fileName = torrentFile.readLine();
        torrentFile.read(sha);
        torrentFile.readLine();
        int size = Integer.parseInt(torrentFile.readLine());
        int numberOfCHunks = Integer.parseInt(torrentFile.readLine());
        int sizeOfCHunks = Integer.parseInt(torrentFile.readLine());
        int leftOver = Integer.parseInt(torrentFile.readLine());
        int numberOfPeers = Integer.parseInt(torrentFile.readLine());
        String[] peers = new String[numberOfPeers];
        for(int i = 0; i<numberOfPeers; i++){
            tempPeer = torrentFile.readLine();
            String delims = " ";
            String[] tokens = tempPeer.split(delims);
            peers[i] = tokens[1];

        }
        for(int j=0; j<numberOfCHunks;j++){
            String[] peersForChunks = new String[2];
            String offsetPeer = torrentFile.readLine();
            String delims = " ";
            String[] tokens = offsetPeer.split(delims);
            long offset = Long.parseLong(tokens[0]);
            peersForChunks[0] = peers[Integer.parseInt(tokens[1])];
            peersForChunks[1] = peers[Integer.parseInt(tokens[2])];
            chunkPeerMap.put(offset, peersForChunks);
        }
        FileChunkInfo fileChunkInfo = new FileChunkInfo(fileName, sha);
        fileChunkInfo.size = size;
        fileChunkInfo.chunkCount = numberOfCHunks;
        fileChunkInfo.chunkSize = sizeOfCHunks;
        fileChunkInfo.setChunkInfo(chunkPeerMap);

        return fileChunkInfo;
    }



}
