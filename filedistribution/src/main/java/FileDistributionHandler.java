import java.util.LinkedList;

/**
 * Created by ankur on 14.06.2016.
 */
public class FileDistributionHandler {
    private String fileName;
    private LinkedList<String> nodeList;
    pftFileManager fileManager;
    public FileDistributionHandler(String fileName, LinkedList<String> nodeList){
        this.fileName = fileName;
        this.nodeList = nodeList;
        fileManager = new pftFileManager(fileName);
    }
}
