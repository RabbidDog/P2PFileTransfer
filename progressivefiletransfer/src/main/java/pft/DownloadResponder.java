package pft;

import org.javatuples.Pair;
import pft.file_operation.IFileFacade;
import pft.frames.DataRequest;
import pft.operations.SendDataResponsePacket;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

/**
 * Created by rabbiddog on 6/20/16.
 */
public class DownloadResponder {

    public static void respond(DataRequest request, SocketAddress destination, ConcurrentLinkedQueue<Pair<ByteBuffer, SocketAddress >> sendBuffer, ConcurrentLinkedQueue<DataRequest> datarequestBuffer, IFileFacade fileManager)
    {
        /*first request always has the information about the chunk size
        * This chunk size should be passed to the SendDataResponsePacket as length*/
        ExecutorService executor = Executors.newFixedThreadPool(1);
        CompletionService<Boolean> completionService =
                new ExecutorCompletionService<Boolean>(executor);
        ConcurrentHashMap<Long, byte[]> bufferedFileData = new ConcurrentHashMap<Long, byte[]>(65536);
        /*load part of chunk alreadychunk already*/
        fileManager.bufferedRead(request.offset(), request.length() *5, 512, bufferedFileData);

        completionService.submit(new SendDataResponsePacket(request.identifier(), fileManager, request.offset(), request.chunkSize(), destination, sendBuffer, bufferedFileData, datarequestBuffer), true);

        /*delete old data*/
    }
}
