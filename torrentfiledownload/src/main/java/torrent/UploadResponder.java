package torrent;

import org.javatuples.Pair;
import pft.frames.DataResponse;
import pft.frames.Frame;
import pft.frames.PartialUpoadRequest;
import pft.frames.UploadRequest;
import pft.operations.*;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rabbiddog on 6/20/16.
 */
/*handles upload requests. creates threads similar to Download handler for each request*/
public class UploadResponder {

    public static ExecutorService Respond(PartialUpoadRequest request, SocketAddress destination, ConcurrentLinkedQueue<Pair<ByteBuffer , SocketAddress >> sendBuffer, ConcurrentLinkedQueue<DataResponse> dataresponseBuffer)
    {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        ConcurrentHashMap<Long, Pair< ByteBuffer, SocketAddress >> pendingPackets = new ConcurrentHashMap<Long, Pair< ByteBuffer, SocketAddress >>();
        AtomicLong currentOffset = new AtomicLong(0);
        AtomicLong highestOffsetReceived = new AtomicLong(0);
        Future sender =  executorService.submit(new SendDataRequestPacket(request.identifier(), request.fileName(), request.offset(), request.length(), destination, pendingPackets, sendBuffer, currentOffset));
        Future resender = executorService.submit(new ResendDataRequestPacket(request.identifier(), request.fileName(), destination, currentOffset, request.length(), highestOffsetReceived, pendingPackets,  sendBuffer));
        Future processor = executorService.submit(new ProcessDataResponsePacket(request.identifier(), request.fileName(), currentOffset, request.length(), highestOffsetReceived, dataresponseBuffer, pendingPackets,destination, sendBuffer));

        return executorService;
    }

}
