import java.io.*;
import java.io.File;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Created by ankur on 14.06.2016.
 */
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pft.*;

import java.io.FileReader;

import pft.file_operation.PftFileManager;

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

    public FileDistributionHandler(String fileName, LinkedList<String> nodeList) throws FileNotFoundException {
        _log = LogManager.getRootLogger();

        this.fileName = fileName;
        this.nodeList = nodeList;
        fileManager = new PftFileManager(fileName);
        if(fileManager.fileExits()){
            this.fileExists = true;
            fileSize = fileManager.getSize();
            fileSha = fileManager.getHash("SHA-1", 0, (int)this.fileSize);
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
        //TODO: Implement Logger instead of System.out.print
        _log.info("fileSize: " + fileSize);
        _log.info("numberOfchunks: " + numberOfChunks);
        _log.info("sizerOfchunks: " + sizeOfChunks);
        _log.info("leftOver: " + leftOverChunk);
    }

    public void startDistribution() throws IOException {
        int peer_count = 0;
        int numberOfPeers = nodeList.size();
        int offset = 0;
        byte[] chunk;


        try{

            for (int i = 0; i < numberOfChunks; i++) {
                chunk = fileManager.readFromPosition(offset, sizeOfChunks);
                // A function call to pft to be implemented
                //TODO : Modified Pft
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)

                peer_count = (peer_count + 1) % numberOfPeers;
                //upload the chunk to the next peer for high availability
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)

                offset = offset + sizeOfChunks;
            }
            if (leftOverChunk !=0) {
                //upload the leftoverChunk to two peers
                chunk = fileManager.readFromPosition(offset, leftOverChunk);
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)

                peer_count = (peer_count + 1) % numberOfPeers;
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)
            }
        }finally {
        }

    }
    public void generateTorrentFile() throws IOException {
        int peer_count = 0;
        int numberOfPeers = nodeList.size();
        int offset = 0;
        byte[] chunk;
        String path = getTorrentDirectory();
        //TODO: Implement Logger instead of System.out.print
        _log.info("Torrent file directory"+path);

        System.out.println();

        RandomAccessFile torrentFile = new RandomAccessFile(path + "\\" + fileName + ".torrent", "rw");
        try{
            torrentFile.writeBytes(fileName + "\r\n");
            torrentFile.writeBytes(fileSha + "\r\n");
            torrentFile.writeBytes(fileSize + "\r\n");
            torrentFile.writeBytes(Integer.toString(numberOfChunks) + "\r\n");
            torrentFile.writeBytes(Integer.toString(sizeOfChunks) + "\r\n");
            torrentFile.writeBytes(Integer.toString(leftOverChunk) + "\r\n");
            torrentFile.writeBytes(Integer.toString(numberOfPeers) + "\r\n");
            for(int j= 0;j<numberOfPeers;j++){
                torrentFile.writeBytes(Integer.toString(j)+" ");
                torrentFile.writeBytes(nodeList.get(j) + "\r\n");
            }
            for (int i = 0; i < numberOfChunks; i++) {
                chunk = fileManager.readFromPosition(offset, sizeOfChunks);
                // A function call to pft to be implemented
                //TODO : Modified Pft
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)
                //write the entry in the torrent file
                torrentFile.writeBytes(Long.toString(offset)+" ");
                torrentFile.writeBytes(Integer.toString(peer_count)+" ");
                peer_count = (peer_count + 1) % numberOfPeers;
                //upload the chunk to the next peer for high availability
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)
                torrentFile.writeBytes(Integer.toString(peer_count) + "\r\n");
                offset = offset + sizeOfChunks;
            }
            if (leftOverChunk !=0) {
                //upload the leftoverChunk to two peers
                chunk = fileManager.readFromPosition(offset, leftOverChunk);
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)
                torrentFile.writeBytes(Long.toString(leftOverChunk)+" ");
                torrentFile.writeBytes(Integer.toString(peer_count)+" ");
                peer_count = (peer_count + 1) % numberOfPeers;
                // pft_upload(nodeList.get(peer_count), fileParameters, Offset, length....)
                torrentFile.writeBytes(Integer.toString(peer_count) + "\r\n");
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
