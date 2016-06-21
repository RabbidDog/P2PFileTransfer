package pft.operations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.Framer;
import pft.file_operation.IFileFacade;
import pft.frames.DataRequest;
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
    private final int identifier;
    private String fileName;
    private long startOffset;
    private long totalLength;
    private SocketAddress destination;
    private ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    private final int defaultPacketSize = 512;
    private Framer _framer;
    private final Logger _log;
    private String TAG;
    private IFileFacade fileManager;
    private ConcurrentHashMap<Long, byte[]> bufferedFileData;
    private ConcurrentLinkedQueue<DataRequest> _dataRequests;

    public SendDataResponsePacket(int identifier, IFileFacade fileManager, long startOffset, long totalLength, SocketAddress destination, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer, ConcurrentHashMap<Long, byte[]> bufferedFileData, ConcurrentLinkedQueue<DataRequest> dataRequests)
    {
        this.identifier = identifier;
        this.fileManager = fileManager;
        this.startOffset = startOffset;
        this.totalLength = totalLength;
        this.destination = destination;
        this.bufferedFileData = bufferedFileData;
        this._dataRequests = dataRequests;
        _framer = new Framer();
        _sendBuffer = sendBuffer;
        _log = LogManager.getRootLogger();
        TAG = "SendDataResponsePacket: FileName: "+this.fileManager.getFileName()+ " startoffset: " + this.startOffset + " identifier: "+ this.identifier;
    }
    @Override
    public void run() {
        long lastTimePacketsReceived = System.currentTimeMillis();
        _log.debug(TAG  + "SendDataResponsePacket process started");
        for(;;)
        {
            _log.debug(TAG + "Destination of packet will be  "+ destination.toString());
            if((System.currentTimeMillis() - lastTimePacketsReceived) > 10000) //10sec
            {
                _log.debug(TAG + "No request received in 10 sec. Closing...");
                break;
            }
            try{
                DataRequest req = _dataRequests.poll();
                if(null == req)
                {
                    Thread.sleep(10);
                    continue;
                }

                long reqOfset = req.offset();
                /*if(reqOfset> (startOffset + totalLength))
                {
                    _log.debug(TAG + " request offset greater than last position inside chuck.Indication to end");
                    break;
                }*/
                long reqLength = req.length();
                _log.debug(TAG + "Offset received: " + reqOfset);
                _log.debug(TAG + "Length  received: " + reqLength);
                long window = reqLength / defaultPacketSize;
                int readBytes = defaultPacketSize;
                long offset = reqOfset;
                byte [] dataFromFile;
                if(window == 0) {
                    window = 1;
                    readBytes = (int)reqLength;
                }
                /*read data from file at one go*/
                if(!bufferedFileData.containsKey(reqOfset))
                {
                    _log.debug(TAG + "Offset not in buffer. Read from file");
                    fileManager.bufferedRead(startOffset, reqLength*5, defaultPacketSize, bufferedFileData);
                }

                for (int i = 0; i< window; i++) {
                    dataFromFile = bufferedFileData.remove(offset);
                    if(null == dataFromFile)
                    {
                        _log.debug(TAG + "Offset not in buffer. Read from file");
                        fileManager.bufferedRead(offset, reqLength * 5, defaultPacketSize, bufferedFileData);
                        dataFromFile = bufferedFileData.remove(offset);
                    }
                    if(null == dataFromFile)
                    {
                        _log.error(TAG + " dataFileFile still null after read");
                    }
                    DataResponse dataResponse = new DataResponse(identifier,offset, dataFromFile.length, dataFromFile);
                    _log.debug("Response packet getiing ready with length: " + dataFromFile.length);
                    ByteBuffer packetBuffer = ByteBuffer.wrap(_framer.frame(dataResponse));
                    _sendBuffer.add(Pair.with(packetBuffer, destination));

                    _log.debug(TAG + " Packet sent for offset: " + offset);
                    offset += defaultPacketSize;
                }
            }catch (InterruptedException iex)
            {
                _log.debug(TAG + " "+iex.getStackTrace());
                break;
            }

        }
        _log.debug(TAG + " exited loop. Will not listen on this port");
    }
}
