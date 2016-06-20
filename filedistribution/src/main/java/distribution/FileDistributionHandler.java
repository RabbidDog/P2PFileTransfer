package distribution;

import java.io.*;
import java.io.File;

import entity.Chunk;
import entity.FileChunkInfo;
import entity.Peer;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by ankur on 14.06.2016.
 */
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pft.*;

import java.io.FileReader;

import pft.file_operation.PftFileManager;
import pft.frames.DataRequest;

public class FileDistributionHandler {
    private Logger _log;

    private String fileName;
    private LinkedList<String> nodeList;
    private PftFileManager fileManager;
    private boolean fileExists;
    private long fileSize;
    private byte[] fileSha;
    private int numberOfChunks;
    private int sizeOfChunks = 16*1024;
    private int leftOverChunk;
    private HashMap<Integer, Peer> peerMap;
    private FileChunkInfo _fileChunkInfo;

    public FileDistributionHandler(String fileName, LinkedList<String> nodeList) throws FileNotFoundException {
        _log = LogManager.getRootLogger();

        this.fileName = fileName;
        this.nodeList = nodeList;
        fileManager = new PftFileManager(FileDistributionApplication.mainFolder+fileName);
        if(fileManager.fileExits()){
            this.fileExists = true;
            fileSize = fileManager.getSize();
            fileSha = fileManager.getHash("SHA-1", 0, (int)this.fileSize);
            _fileChunkInfo = new FileChunkInfo(this.fileName, this.fileSha);
        }
        else {
            throw new FileNotFoundException("FileChunkInfo doesnot Exist");
        }
    }
    //set the number of chunks and sizeOfChunks
    public void setDistributionParameters(){
        this.numberOfChunks = (int)this.fileSize /sizeOfChunks;
        while(this.numberOfChunks > 2400){
            this.sizeOfChunks = this.sizeOfChunks + (16*1024);
            this.numberOfChunks = (int)this.fileSize /sizeOfChunks;
        }

        this.leftOverChunk = (int)this.fileSize % sizeOfChunks;
        if(this.leftOverChunk > 0) this.numberOfChunks++;
        //TODO: Implement Logger instead of System.out.print
        _log.info("fileSize: " + fileSize);
        _log.info("numberOfchunks: " + numberOfChunks);
        _log.info("sizerOfchunks: " + sizeOfChunks);
        _log.info("leftOver: " + leftOverChunk);
    }

    public void startDistribution() throws IOException {

        PacketService pckService = new PacketService(FileDistributionApplication.mainFolder);
        Thread th = new Thread(pckService);
        th.start();
        Random rand = new Random();
        UploadHandler uploadHandler = new UploadHandler(_fileChunkInfo, FileDistributionApplication.mainFolder+fileName, pckService._server._sendBuffer, rand, pckService._dataRequestQueueForIdentifier);
        uploadHandler.startUpload();
    }
    public void generateTorrentFile() throws IOException {
        int peer_count = 0;
        int numberOfPeers = nodeList.size();
        int offset = 0;
        byte[] chunk;
        String path = getTorrentDirectory();
        //TODO: Implement Logger instead of System.out.print
        _log.info("Torrent file directory"+path);

        this.peerMap = new HashMap<>();
        int i = 0;
        for (String hostname:nodeList)
        {
            String[] hs = hostname.split(":");
            Peer p = new Peer();
            p.address = hs[0];
            p.port = Integer.parseInt(hs[1]);
            peerMap.putIfAbsent(i, p);
            i++;
        }
        _fileChunkInfo.chunkInfo = new ArrayList<Chunk>();
        _fileChunkInfo.chunkCount = numberOfChunks;
        _fileChunkInfo.chunkSize = sizeOfChunks;
        _fileChunkInfo.size = fileSize;

        RandomAccessFile torrentFile = new RandomAccessFile(new File(path + fileName + ".torrent"), "rw");
        try{
            torrentFile.writeBytes(fileName + "\r\n");
                torrentFile.write(fileSha, 0, 20);
            torrentFile.writeBytes("\r\n");
            torrentFile.writeBytes(fileSize + "\r\n");
            torrentFile.writeBytes(Integer.toString(numberOfChunks) + "\r\n");
            torrentFile.writeBytes(Integer.toString(sizeOfChunks) + "\r\n");
            torrentFile.writeBytes(Integer.toString(leftOverChunk) + "\r\n");
            torrentFile.writeBytes(Integer.toString(numberOfPeers) + "\r\n");
            for(int j= 0;j<numberOfPeers;j++){
                torrentFile.writeBytes(Integer.toString(j)+" ");
                torrentFile.writeBytes(nodeList.get(j) + "\r\n");
            }
            for (i = 0; i < numberOfChunks; i++) {
                Chunk ch=new Chunk();
                ch.offset = offset;
                ch.length = sizeOfChunks;
                ch.isDownloaded = false;
                ch.peerList = new ArrayList<>();
                if(i == (numberOfChunks -1))
                {
                    ch.length = fileSize - offset;
                }

                torrentFile.writeBytes(Long.toString(offset)+" ");
                torrentFile.writeBytes(Integer.toString(peer_count)+" ");
                ch.peerList.add(peerMap.get(peer_count));

                peer_count = (peer_count + 1) % numberOfPeers;
                ch.peerList.add(peerMap.get(peer_count));
                _fileChunkInfo.chunkInfo.add(ch);
                torrentFile.writeBytes(Integer.toString(peer_count) + "\r\n");
                offset = offset + sizeOfChunks;
            }

        }finally {
            torrentFile.close();
        }

    }


    private String getTorrentDirectory() {
        //Get the path where the torrent file is to be created from the configTorrent.properties FileChunkInfo
        {
            File configFile = new File("config.properties");
            String path;
            try {
                FileReader reader = new FileReader(configFile);
                Properties props = new Properties();
                props.load(reader);
                path= (props.getProperty("torrentDirectory"));
                reader.close();
                return path;

            } catch (FileNotFoundException ex) {
                _log.error("Error in reading Configuration file while searching for path to Log file");
                return null;
            } catch (IOException ex) {
                _log.error("Error in reading Configuration file while searching for path to Log file");
                return null;
            }

        }
    }
}
