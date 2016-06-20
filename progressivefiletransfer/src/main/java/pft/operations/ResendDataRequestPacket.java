package pft.operations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;
import org.javatuples.Pair;
import pft.frames.DataRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rabbiddog on 6/19/16.
 */
public class ResendDataRequestPacket implements Callable<Long> {

    private int identifier;
    private String fileName;
    private SocketAddress destination;
    private volatile AtomicLong currentOffset;
    private volatile AtomicLong highestOffsetReceived;
    private long length;
    private  ConcurrentHashMap<Long, Pair<ByteBuffer ,SocketAddress>> pendingPackets;
    private ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    private final Logger _log;
    private String TAG;

    public ResendDataRequestPacket(int identifier, String fileName, SocketAddress destination, AtomicLong currentOffset, long length, AtomicLong highestOffsetReceived, ConcurrentHashMap<Long, Pair<ByteBuffer ,SocketAddress>> pendingPackets, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer)
    {
        this.identifier = identifier;
        this.fileName = fileName;
        this.destination = destination;
        this.pendingPackets = pendingPackets;
        _sendBuffer = sendBuffer;
        this.currentOffset = currentOffset;
        this.length = length;
        this.highestOffsetReceived = highestOffsetReceived;
        _log = LogManager.getRootLogger();
        TAG = "FileName: "+fileName+ " identifier: " + identifier;
    }
    @Override
    public Long call() {
        _log.debug(TAG + " Resend packet Thread started");
        DatagramPacket packet;
        byte[] requestBuffer;
        for(;;)
        {
            if (Thread.interrupted()) {
                _log.debug(TAG + " Executor ShutdownNow requested. Shutting down resendRequestFuture");
                return currentOffset.get();
            }
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException iex)
            {
                iex.printStackTrace();
                return currentOffset.get();
            }
            _log.debug(TAG + "Will Check If packets need to be resent");
            if(pendingPackets.size() == 0 && (currentOffset.get() == length))
            {
                _log.debug(TAG + "No more packets to resend. Will shut down thread");
                return currentOffset.get();
            }
            else
            {
                Set<Long> keySet = pendingPackets.keySet();
                for (Long offset: keySet)
                {
                    if(offset < highestOffsetReceived.get())
                    {
                        try
                        {
                            Pair<ByteBuffer, SocketAddress> p = pendingPackets.get(offset);
                            if(null != p)
                            {
                                _sendBuffer.add(p);
                                _log.debug("Data Request resend for offset: "+offset);
                            }
                        }
                        catch (Exception ex)
                        {
                            _log.error(TAG + " "+ex.getStackTrace());
                        }
                    }
                }
            }
        }
    }
}
