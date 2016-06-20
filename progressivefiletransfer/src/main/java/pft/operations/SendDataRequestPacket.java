package pft.operations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.Framer;
import pft.frames.DataRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rabbiddog on 6/19/16.
 */
public class SendDataRequestPacket implements Callable<Long> {

    private int identifier;
    private String fileName;
    private long startOffset;
    private long length;
    private SocketAddress destination;
    private AtomicLong currentOffset;
    private ConcurrentHashMap<Long, Pair<ByteBuffer ,SocketAddress>> pendingPackets;
    private ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    private final long defaultPacketSize = 512;
    private Framer _framer;
    private final Logger _log;
    private String TAG;
    private byte[] _hashCode;

    public SendDataRequestPacket(int identifier, String fileName, byte[] hashCode, long startOffset, long length, SocketAddress destination, ConcurrentHashMap<Long, Pair<ByteBuffer ,SocketAddress>> pendingPackets, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer, AtomicLong currentOffset)
    {
        this.identifier = identifier;
        this.fileName = fileName;
        this.startOffset = startOffset;
        this.length = length;
        this.destination = destination;
        this.pendingPackets = pendingPackets;
        this.currentOffset = currentOffset;
        _framer = new Framer();
        _sendBuffer = sendBuffer;
        _log = LogManager.getRootLogger();
        _hashCode = hashCode;
        TAG = "SendDataRequestPacket : FileName: "+fileName+ " offset: " + startOffset + " identifier: "+identifier;
    }
    @Override
    public Long call() {

        _log .debug(TAG + " Send data request Thread started");
                    /*check if partial file exist*/
        DatagramPacket packet;
        ByteBuffer packetBuffer;
        currentOffset.set(startOffset);
        for(;;) //this forloop keep sending requests till termination is received for file size is reached
        {
            if (Thread.interrupted()) {
                _log .debug(TAG +" Executor ShutdownNow requested. Shutting down sendRequestFuture ");
                return currentOffset.get();
            }
            if(currentOffset.get()< length)
            {
                if(pendingPackets.size() == 0)//change this to allow to send multiple requests even when previous requests were not fulfilled
                {
                    _log.debug(TAG + " SendDataRequestPacket: pending packet size zero. Sending new request");
                    try
                    {
                        boolean isLessThanDefaultSize = ((length - currentOffset.get()) / defaultPacketSize) == 0 ;
                        if(isLessThanDefaultSize)
                        {
                            DataRequest request = new DataRequest(identifier, fileName, _hashCode, (int)currentOffset.get(), (length - currentOffset.get()), length);

                            _log.debug(TAG + " Send Request for last packet");
                            packetBuffer = ByteBuffer.wrap(_framer.frame(request));
                            Pair<ByteBuffer ,SocketAddress> p = Pair.with(packetBuffer, destination);
                            _sendBuffer.add(p);
                            pendingPackets.putIfAbsent(currentOffset.get(), p);
                            currentOffset.set(length);
                        }
                        else
                        {
                            int remainingWindow = 32 - pendingPackets.size();
                            long remainingPackets = ((length - currentOffset.get()) / defaultPacketSize) >=32 ? 32 : ((length - currentOffset.get()) / defaultPacketSize);

                            long packetsToSend = remainingWindow>remainingPackets ? remainingPackets:remainingWindow;
                            DataRequest request = new DataRequest(identifier, fileName, _hashCode, currentOffset.get(), defaultPacketSize*packetsToSend, length);
                            packetBuffer = ByteBuffer.wrap(_framer.frame(request));
                            Pair<ByteBuffer ,SocketAddress> p = Pair.with(packetBuffer, destination);
                            _sendBuffer.add(p);
                            //create required number of packet request for later request
                            for (int i = 0; i < packetsToSend; i++) {
                                DataRequest request1 = new DataRequest(identifier, currentOffset.get(), defaultPacketSize);
                                packetBuffer = ByteBuffer.wrap(_framer.frame(request1));
                                pendingPackets.putIfAbsent(currentOffset.get(), Pair.with(packetBuffer, destination));
                                long temp = currentOffset.get();
                                currentOffset.set(temp + defaultPacketSize);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        _log.error(TAG + " "+ex.getStackTrace());
                    }

                }
                else
                {
                    /*wait till few of the pending requests have been serviced*/
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException inex) {
                        _log.debug(TAG + " InterruptedException in Thread SendRequest");
                        return currentOffset.get();
                    }
                }
            }
        }
    }
}
