package pft.frames;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by ankur on 19.06.2016.
 */
public class PartialUpoadRequest extends Frame {
    String fileName;
    long size;
    byte[] sha1;
    long offset;
    long length;


    public PartialUpoadRequest(int identifier, String filename, long size, byte[] sha1, long offset, long length) {
        super(identifier);
        this.fileName = checkNotNull(filename);
        checkArgument(filename.length() < 256);
        this.size = size;
        checkArgument(size > 0);
        this.sha1 = sha1;
        this.offset = offset;
        checkArgument(offset > 0);
        this.length = length;
        checkArgument(length > 0);
    }
    public String fileName() {
        return fileName;
    }

    public long size() {
        return size;
    }

    public byte[] sha1() {
        return sha1;
    }

    public long offset(){
        return offset;
    }
    public long length(){
        return length;
    }
    @Override
    public byte type() {
        return 10;
    }
}
