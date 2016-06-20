package torrent;

import org.javatuples.Pair;
import pft.file_operation.IFileFacade;
import pft.frames.DataRequest;
import pft.frames.DataResponse;
import pft.operations.*;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rabbiddog on 6/20/16.
 */
public class DownloadResponder {

    public static ExecutorService respond(DataRequest request, SocketAddress destination, ConcurrentLinkedQueue<Pair<ByteBuffer, SocketAddress >> sendBuffer, ConcurrentLinkedQueue<DataRequest> datarequestBuffer, IFileFacade fileManager)
    {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ConcurrentHashMap<Long, byte[]> bufferedFileData = new ConcurrentHashMap<Long, byte[]>(65536);
        /*load part of chunk alreadychunk already*/
        fileManager.bufferedRead(request.offset(), request.length(), 512, bufferedFileData);
        executor.submit(new SendDataResponsePacket(request.identifier(), fileManager, request.offset(), request.length(), destination, sendBuffer, bufferedFileData, datarequestBuffer));

        return executor;
    }
}
