    package pft.file_operation;


    import java.io.FileNotFoundException;
    import java.io.IOException;
    import java.io.RandomAccessFile;
    import java.io.File;
    import java.io.FileNotFoundException;
    import java.io.IOException;
    import java.io.RandomAccessFile;
    import java.nio.file.Files;
    import java.nio.file.Path;
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
        public static boolean bufferedRead(Path filePath, long offset, long length, int chunkSize, ConcurrentHashMap<Long, byte[]> buffer)
        {
            if(Files.notExists(filePath))
                return false;
            else if(!Files.isReadable(filePath))
                return false;

            File file = new File(filePath.toString());
            long fileSize = file.length();
            if(offset > fileSize)
                return false;

            long end = (offset + length)>fileSize ? fileSize : (offset + length);

            try
            {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(offset);
                while (offset < end)
                {
                    if((end-offset) < chunkSize)
                        chunkSize = (int)(end-offset);
                    byte[] chunkbuffer = new byte[chunkSize];
                    raf.read(chunkbuffer, 0, chunkSize);
                    buffer.putIfAbsent(offset, chunkbuffer);
                    offset += chunkSize;
                }

                raf.close();
                return true;
            }
            catch (FileNotFoundException fex)
            {
                return false;
            }catch (IOException ioex)
            {
                return false;
            }
        }

}
