import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

/**
 * Created by ankur on 14.06.2016.
 */
import pft.*;
import pft.file_operation.PftFileManager;

public class FileDistributionHandler {
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
        this.fileName = fileName;
        this.nodeList = nodeList;
        fileManager = new PftFileManager(fileName);
        if(fileManager.fileExits()){
            this.fileExists = true;
            fileSize = fileManager.getSize();
            fileSha = fileManager.getHash("SHA-1", 0, (int)this.fileSize);
        }
        else {
            throw new FileNotFoundException("File doesnot Exist");
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
        System.out.println("numberOfchunks: " + numberOfChunks);
        System.out.println("sizerOfchunks: " + sizeOfChunks);
        System.out.println("leftOver: " + leftOverChunk);

    }

    public void startDistribution() throws IOException {
        int peer_count = 0;
        int numberOfPeers = nodeList.size();
        int offset = 0;
        byte[] chunk;
        String path = getTorrentDirectory();
        //TODO: Implement Logger instead of System.out.print

        System.out.println("Torrent file directory"+path);

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
        //Get the path where the torrent file is to be created from the configTorrent.properties File
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile("configTorrent.properties", "r");
            String path = raf.readLine();
            return path;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        finally{
            if(raf != null){
                try {
                    raf.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
    }
}
