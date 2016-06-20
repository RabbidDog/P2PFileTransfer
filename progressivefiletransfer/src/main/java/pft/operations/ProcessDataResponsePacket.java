package pft.operations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.file_operation.IFileFacade;
import pft.frames.*;
import pft.Framer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rabbiddog on 6/19/16.
 */
public class ProcessDataResponsePacket implements Callable<Long>{

    private  int identifier;
    private String fileName;
    private AtomicLong currentOffset;
    private volatile AtomicLong highestOffsetReceived;
    private  long length;
    private  ConcurrentLinkedQueue<DataResponse> incomingFrames;
    private ConcurrentHashMap<Long, Pair<ByteBuffer ,SocketAddress>> pendingPackets;
    private final Logger _log;
    private String TAG;
    private IFileFacade fileManager;
    private Framer framer;
    private SocketAddress destination;
    private ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer;

    public ProcessDataResponsePacket(int identifier, String fileName, AtomicLong currentOffset, long length, AtomicLong highestOffsetReceived, ConcurrentLinkedQueue<DataResponse> incomingFrames, ConcurrentHashMap<Long, Pair<ByteBuffer ,SocketAddress>> pendingPackets,SocketAddress destination, ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> sendBuffer)
    {
        this.identifier = identifier;
        this.fileName = fileName;
        this.currentOffset = currentOffset;
        this.length = length;
        this.incomingFrames = incomingFrames;
        this.highestOffsetReceived = highestOffsetReceived;
        this.pendingPackets = pendingPackets;
        this.destination = destination;
        this.sendBuffer = sendBuffer;
        _log = LogManager.getRootLogger();
        TAG = "FileName: "+fileName+ " identifier: " + identifier;
        framer = new Framer();
    }

    @Override
    public Long call() throws Exception {
        _log.debug(TAG + "Start Thread to process incoming packets");

        byte[] packetbuffer = new byte[8196]; //check what happens if datagram is larger tha 512
        DatagramPacket packet = new DatagramPacket(packetbuffer, packetbuffer.length);
        for(;;)
        {
            if (Thread.interrupted()) {
                _log.debug(TAG + " Executor ShutdownNow requested. Shutting down processPacketFuture");
                break;
            }
            _log.debug(TAG + " will process packet now");
            try
            {
                DataResponse response = incomingFrames.poll();
                if(null == response)
                {
                    Thread.sleep(10);
                    continue;
                }
                _log.debug(TAG + " incomingFrames poll returned non null");
                /*if(f instanceof DataResponse)
                {*/
                    if(response.identifier() == identifier)
                    {
                        /* remove Data request from resend
                        * if key not found then already removed. don't rewite to file
                        * if removed then new packet, then remove and write to file
                        */
                        if(pendingPackets.containsKey(response.offset()))
                        {
                            _log.debug(TAG + "received packet for offset: "+response.offset()+" was pending");
                            //boolean checkToShutExecutor = false;
                            _log.debug(TAG +"pendingPackets size before removal: "+pendingPackets.size());
                            pendingPackets.remove(response.offset());
                            _log.debug(TAG + "pendingPackets size after removal: "+pendingPackets.size());
                            if(highestOffsetReceived.get() < response.offset())
                            {
                                highestOffsetReceived.set(response.offset());
                            }
                            boolean checkToShutExecutor = false;
                            if(pendingPackets.size() == 0)
                            {
                                /*System.out.println("Process Packet: pending packet size 0. update offset in file");
                                byte[] offset = ByteBuffer.allocate(4).putInt((int)currentOffset).array();
                                writeOffsetInPftFile(offset);*/
                                checkToShutExecutor=true;
                            }
                            //delegate writing to file to a seperate thread
                            //long writePosition = fileManager.writeFromPosition(response.offset(), response.length(), response.data());
                            long writePosition = IFileFacade.writeBytesToFile(fileName, response.offset(), response.data());

                            _log.debug(TAG+" Data Response received for "+response.offset());
                            if(writePosition == (response.offset() + response.data().length)) /*data was successfully written*/
                            {
                                if(checkToShutExecutor && currentOffset.get() == length)
                                {
                                    _log.debug(TAG + "Current offset is file size. Will stop precess");
                                    /*send request for offset greater than chuck positioo request*/
                                    ByteBuffer terminationBuffer = ByteBuffer.wrap(framer.frame(new DataRequest(identifier, currentOffset.get() + 1, length)));
                                    sendBuffer.add(Pair.with(terminationBuffer, destination));
                                    /*update DB*/
                                    /*update tracker*/
                                    return length;
                                }
                            }
                        }
                        else{
                            _log.debug(TAG + "ProcessPacket: packet at offset: "+response.offset()+" already received. Ignoring this packet");
                        }

                    }
                    else
                    {
                        _log.debug("Data Response with incorrect identifier was received. "+"Expected "+identifier+". "+"Received "+ response.identifier());
                    }
                //}
                /*else if(f instanceof TerminationRequest)
                {
                    //write remaining packets to the file system and close this thread
                    TerminationRequest terminationRequest = (TerminationRequest) f;
                    if(terminationRequest.identifier() == identifier)
                    {
                        _log.debug(TAG + "Received Termination request. Will stop process");
                        return currentOffset;
                    }
                    else
                    {
                        _log.debug(TAG + "Termination Request with incorrect identifier was received. "+"Expected "+identifier+". "+"Received "+ terminationRequest.identifier());
                    }
                }
                else
                {
                    _log.debug(TAG +"Received packet is neither dataresponse nor terminaton");
                }*/
            }
            catch (InterruptedException iex)
            {
                _log.debug(TAG + " "+iex.getStackTrace());
                break;
            }
        }

        _log.debug(TAG + "For loop ended for processPacketFuture");
        return currentOffset.get();
    }
}
