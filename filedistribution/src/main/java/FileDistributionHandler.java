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

    public FileDistributionHandler(String fileName, LinkedList<String> nodeList){
        this.fileName = fileName;
        this.nodeList = nodeList;
        fileManager = new PftFileManager(fileName);
        if(fileManager.fileExits()){
            this.fileExists = true;
            fileSize = fileManager.getSize();
            fileSha = fileManager.getHash("SHA-1", 0, (int)this.fileSize);
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
    }
}
