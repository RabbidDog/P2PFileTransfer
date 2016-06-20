package pft.frames.marshallers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import pft.frames.DataRequest;

import java.nio.charset.Charset;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static pft.frames.marshallers.Utils.filenameToString;


public class DataRequestMarshaller implements Marshaller<DataRequest> {

  private static final PooledByteBufAllocator ALLOCATOR =
      PooledByteBufAllocator.DEFAULT;

  private static final int OFFSET_LENGTH = 4;
  private static final int LENGTH_LENGTH = 4;
  private static final int FILENAME_LENGTH = 256;
  private static final int SHA_LENGTH = 20;

  private static final int LENGTH = OFFSET_LENGTH + LENGTH_LENGTH;
  private static final int LENGTH_WITH_FILE_SHA = OFFSET_LENGTH + LENGTH_LENGTH + FILENAME_LENGTH + SHA_LENGTH;


  @Override public DataRequest decode(int identifier, byte[] data) {
    checkArgument(identifier != 0);
    checkNotNull(data);
    //checkArgument(data.length == LENGTH);

    ByteBuf buffer = null;
    if(data.length == LENGTH){
      try {
        buffer = ALLOCATOR.buffer(LENGTH);
        buffer.writeBytes(data);

        long offset = buffer.readUnsignedInt();
        long length = buffer.readUnsignedInt();

        return new DataRequest(identifier, offset, length);
      } finally {
        if (buffer != null) {
          buffer.release();
        }
      }
    }
    else if(data.length == LENGTH_WITH_FILE_SHA){
      try {
        buffer = ALLOCATOR.buffer(LENGTH_WITH_FILE_SHA);
        buffer.writeBytes(data);
        byte[] filenameBytes = new byte[FILENAME_LENGTH];
        buffer.readBytes(filenameBytes);
        String filename = filenameToString(filenameBytes);
        byte[] sha1 = new byte[SHA_LENGTH];
        buffer.readBytes(sha1);
        long offset = buffer.readUnsignedInt();
        long length = buffer.readUnsignedInt();

        return new DataRequest(identifier, filename, sha1, offset, length);
      } finally {
        if (buffer != null) {
          buffer.release();
        }
      }
    }
    else{
      return null;
    }

  }

  @Override public byte[] encode(DataRequest frame) {
    checkNotNull(frame);

    ByteBuf buffer = null;
    if(frame.fileName() == null){
      try {
        buffer = ALLOCATOR.buffer(LENGTH);
        buffer.writeInt((int) frame.offset());
        buffer.writeInt((int) frame.length());
        byte[] data = new byte[LENGTH];
        buffer.readBytes(data);
        return data;
      } finally {
        if (buffer != null) {
          buffer.release();
        }
      }
    }
    else{
      try {
        buffer = ALLOCATOR.buffer(LENGTH_WITH_FILE_SHA);
        byte[] filename = frame.fileName().getBytes(Charset.forName("ASCII"));
        buffer.writeBytes(filename);
        buffer.writeZero(FILENAME_LENGTH - filename.length);
        checkState(buffer.writerIndex() == FILENAME_LENGTH);
        buffer.writeBytes(frame.sha());
        checkState(buffer.writerIndex() == FILENAME_LENGTH + SHA_LENGTH);
        buffer.writeInt((int) frame.offset());
        buffer.writeInt((int) frame.length());
        byte[] data = new byte[LENGTH_WITH_FILE_SHA];
        buffer.readBytes(data);
        return data;
      } finally {
        if (buffer != null) {
          buffer.release();
        }
      }
    }


  }

}
