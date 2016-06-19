    package pft.file_operation;

    import java.io.FileNotFoundException;
    import java.io.IOException;
    import java.io.RandomAccessFile;
    import java.util.concurrent.ConcurrentHashMap;

    /**
 * Created by anjum parvez ali  on 5/14/16.
 */


public interface IFileFacade {

    public byte[] getHash(String hashAlgo, int offset, int length);
    public OpenFileOperationStatus fileMatchDescription(byte[] hash, String hashAlgo);
    public OpenFileOperationStatus fileMatchDescription(byte[] hash, String hashAlgo, int offset, int length);
    public byte[] readFromPosition(int offset, int length);
    public long writeFromPosition(long offset, long length, byte[] data);
    public long getSize();
    public boolean fileExits();
    public String getFileName();
    public boolean deleteFile();
    public boolean bufferedRead(long offset, long length, int chunkSize, ConcurrentHashMap<Long, byte[]> buffer);
    public static long writeBytesToFile(String fileName, long offset, byte[] data)  {
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "rw");
            randomAccessFile.seek(offset);
            randomAccessFile.write(data);
            randomAccessFile.close();
            return data.length;

        } catch (IOException e) {
            e.printStackTrace();
            return 0;

        }

    }
}
