package pft.operations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.Framer;
import pft.file_operation.IFileFacade;
import pft.frames.DataResponse;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by rabbiddog on 6/19/16.
 */
public class SendDataResponsePacket implements Runnable {
    private int identifier;
    private String fileName;
    private long startOffset;
    private long length;
    private SocketAddress destination;
    private ConcurrentHashMap<Long, Pair<ByteBuffer ,SocketAddress>> pendingPackets;
    private ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    private final int defaultPacketSize = 512;
    private Framer _framer;
    private final Logger _log;
    private String TAG;
    private IFileFacade fileManager;
    private ConcurrentHashMap<Long, byte[]> bufferedFileData;

    public SendDataResponsePacket(int identifier, IFileFacade fileManager, long startOffset, long length, SocketAddress destination, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer, ConcurrentHashMap<Long, byte[]> bufferedFileData)
    {
        this.identifier = identifier;
        this.fileManager = fileManager;
        this.startOffset = startOffset;
        this.length = length;
        this.destination = destination;
        this.bufferedFileData = bufferedFileData;
        _framer = new Framer();
        _sendBuffer = sendBuffer;
        _log = LogManager.getRootLogger();
        TAG = "SendDataResponsePacket: FileName: "+fileName+ " offset: " + startOffset + " identifier: "+identifier;
    }
    @Override
    public void run() {
        _log.debug(TAG + "Offset received: " + startOffset);
        _log.debug(TAG + "Length  received: " + length);
        long window = length / defaultPacketSize;
        int readBytes = defaultPacketSize;
        long offset = startOffset;
        byte [] dataFromFile;
        if(window == 0) {
            window = 1;
            readBytes = (int)length;
        }
                /*read data from file at one go*/
        if(!bufferedFileData.containsKey(startOffset))
        {
            _log.debug(TAG + "Offset not in buffer. Read from file");
            fileManager.bufferedRead(startOffset, length*5, defaultPacketSize, bufferedFileData);
        }

        for (int i = 0; i< window; i++) {
            dataFromFile = bufferedFileData.remove(offset);
            if(null == dataFromFile)
            {
                _log.debug(TAG + "Offset not in buffer. Read from file");
                fileManager.bufferedRead(offset, length * 5, defaultPacketSize, bufferedFileData);
            }
            DataResponse dataResponse = new DataResponse(identifier,offset, readBytes, dataFromFile);
            ByteBuffer packetBuffer = ByteBuffer.wrap(_framer.frame(dataResponse));
            _sendBuffer.add(Pair.with(packetBuffer, destination));

            _log.debug(TAG + " Packet sent for offset: " + offset);
            offset += defaultPacketSize;
        }
    }
}
