package pft.frames.marshallers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import pft.frames.PartialUpoadRequest;
import pft.frames.UploadRequest;

import java.nio.charset.Charset;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static pft.frames.marshallers.Utils.filenameToString;

/**
 * Created by ankur on 19.06.2016.
 */
public class PartialUploadRequestMarshaller implements Marshaller<PartialUpoadRequest> {
    private static final PooledByteBufAllocator ALLOCATOR =
            PooledByteBufAllocator.DEFAULT;

    private static final int FILENAME_LENGTH = 256;
    private static final int SIZE_LENGTH = 4;
    private static final int SHA1_LENGTH = 20;
    private static final int OFFSET_LENGTH = 4;
    private static final int LENGTH_LENGTH = 4;
    private static final int LENGTH = FILENAME_LENGTH + SHA1_LENGTH + SIZE_LENGTH + OFFSET_LENGTH + LENGTH_LENGTH;


    @Override
    public PartialUpoadRequest decode(int identifier, byte[] data) {
        checkArgument(identifier != 0);
        checkNotNull(data);
        checkArgument(data.length == LENGTH);

        ByteBuf buffer = null;
        try {
            buffer = ALLOCATOR.buffer(LENGTH);
            buffer.writeBytes(data);
            byte[] filenameBytes = new byte[FILENAME_LENGTH];
            buffer.readBytes(filenameBytes);
            String filename = filenameToString(filenameBytes);

            checkState(buffer.readerIndex() == FILENAME_LENGTH);

            long size = buffer.readUnsignedInt();
            checkState(buffer.readerIndex() == FILENAME_LENGTH + SIZE_LENGTH);
            byte[] sha1 = new byte[SHA1_LENGTH];
            buffer.readBytes(sha1);
            checkState(buffer.readerIndex() == FILENAME_LENGTH + SIZE_LENGTH + SHA1_LENGTH);

            long offset = buffer.readUnsignedInt();
            checkState(buffer.readerIndex() == FILENAME_LENGTH + SIZE_LENGTH + SHA1_LENGTH + OFFSET_LENGTH);
            long length = buffer.readUnsignedInt();
            checkState(buffer.readerIndex() == LENGTH);

            return new PartialUpoadRequest(identifier,filename, size, sha1, offset, length);
        } finally {
            if (buffer != null) {
                buffer.release();
            }
        }

    }

    @Override
    public byte[] encode(PartialUpoadRequest frame) {
        checkNotNull(frame);

        ByteBuf buffer = null;
        try {
            buffer = ALLOCATOR.buffer(LENGTH);
            byte[] filename = frame.fileName().getBytes(Charset.forName("ASCII"));
            buffer.writeBytes(filename);
            // Zero-terminate string
            buffer.writeZero(FILENAME_LENGTH - filename.length);
            checkState(buffer.writerIndex() == FILENAME_LENGTH);
            buffer.writeInt((int) frame.size());
            checkState(buffer.writerIndex() == FILENAME_LENGTH + SIZE_LENGTH);
            buffer.writeBytes(frame.sha1());
            checkState(buffer.writerIndex() == FILENAME_LENGTH + SIZE_LENGTH + SHA1_LENGTH);
            buffer.writeInt((int) frame.offset());
            checkState(buffer.writerIndex() == FILENAME_LENGTH + SIZE_LENGTH + SHA1_LENGTH + OFFSET_LENGTH);
            buffer.writeInt((int) frame.length());
            checkState(buffer.writerIndex() == LENGTH);
            byte[] data = new byte[LENGTH];
            buffer.readBytes(data);

            return data;
        } finally {
            if (buffer != null) {
                buffer.release();
            }


        }
    }
}
