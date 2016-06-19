import entity.Chunk;
import entity.FileChunkInfo;
import entity.Peer;
import org.w3c.dom.Entity;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ankur on 18.06.2016.
 */
public class TorrentParser {
    private String torrentFilePath;

    public TorrentParser(String torrentFilePath){
        this.torrentFilePath = torrentFilePath;
    }
    private  String getHost() {
        //Get the path where the torrent file is to be created from the configTorrent.properties FileChunkInfo
        {
            File configFile = new File("config.properties");
            String host;
            try {
                FileReader reader = new FileReader(configFile);
                Properties props = new Properties();
                props.load(reader);
                host= (props.getProperty("hostname"));
                reader.close();
                return host;

            } catch (FileNotFoundException ex) {
                return null;
            } catch (IOException ex) {
                return null;
            }

        }
    }
    public FileChunkInfo fileParser() throws IOException {
        String hostName = getHost();
        RandomAccessFile torrentFile = new RandomAccessFile(this.torrentFilePath, "r");
        byte[] sha = new byte[20];
        ConcurrentHashMap<Long, String[]> chunkPeerMap = new ConcurrentHashMap<Long, String[]>();
        List<Chunk> chunkInfo = new LinkedList<Chunk>();
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
            Chunk chunk = new Chunk();
            List<Peer> peer = new LinkedList<Peer>();
            Peer peer1 = new Peer();
            Peer peer2 = new Peer();
            String [] address1;
            String [] address2;
            int portPeer1;
            int portPeer2;
            String peer1Address;
            String peer2Address;
            String delimForIpPort = ":";
            String offsetPeer = torrentFile.readLine();
            String delims = " ";
            String[] tokens = offsetPeer.split(delims);
            long offset = Long.parseLong(tokens[0]);
            peersForChunks[0] = peers[Integer.parseInt(tokens[1])];
            peersForChunks[1] = peers[Integer.parseInt(tokens[2])];
            if(peersForChunks[0].equals(hostName) || peersForChunks[1].equals(hostName)){
                chunk.isDownloaded = true;
            }
            else {
                chunk.isDownloaded = false;
            }
            address1 = peersForChunks[0].split(delimForIpPort);
            address2 = peersForChunks[1].split(delimForIpPort);
            peer1Address = address1[0];
            peer2Address = address2[0];
            portPeer1 = Integer.parseInt(address1[1]);
            portPeer2 = Integer.parseInt(address2[1]);
            peer1.address = peer1Address;
            peer2.address = peer2Address;
            peer1.port = portPeer1;
            peer2.port = portPeer2;
            peer.add(peer1);
            peer.add(peer2);
            chunk.peerList = peer;
            chunk.offset = offset;
            if(j== (numberOfCHunks - 1)){
                chunk.length = size - offset;
            }
            else {
                chunk.length = sizeOfCHunks;
            }
            chunkInfo.add(chunk);

        }
        FileChunkInfo fileChunkInfo = new FileChunkInfo(fileName, sha);
        fileChunkInfo.size = size;
        fileChunkInfo.chunkCount = numberOfCHunks;
        fileChunkInfo.chunkSize = sizeOfCHunks;
        fileChunkInfo.chunkInfo = chunkInfo;
        return fileChunkInfo;
    }



}
