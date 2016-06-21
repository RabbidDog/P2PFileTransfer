package pft;

/**
 * Created by rabbiddog on 6/16/16.
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.frames.Frame;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Server extends PftChannel{

    private Logger _log;
    private final String TAG = "pft.Server";

    public DatagramChannel get_serverChannel() {
        return _serverChannel;
    }

    public DatagramChannel _serverChannel;
    public DatagramSocket _serverSocket;
    public final ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    public final ConcurrentLinkedQueue<Pair<ByteBuffer, SocketAddress>> _receiveBuffer;
    private ExecutorService _execService = Executors.newFixedThreadPool(2);
    Future _processIncoming, _processOutgoing;
    Framer _framer;
    Deframer _deframer;
    private static final PooledByteBufAllocator ALLOCATOR =
            PooledByteBufAllocator.DEFAULT;
    private byte[] _receiveBuf = new byte[1024];
    public Server(int port)
    {
        _log = LogManager.getRootLogger();
        _framer = new Framer();
        _deframer = new Deframer();

        _sendBuffer = new ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>>();
        _receiveBuffer = new ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>>();
        try
        {
             /*_serverChannel = DatagramChannel.open();
            _serverChannel.socket().bind(new InetSocketAddress(port));
            _serverChannel.configureBlocking(false);*/
            _serverSocket = new DatagramSocket(port);
        }catch (IOException io)
        {
            _log.error("Server constructor while creating DatagramChannel. " + io.getMessage());
        }
    }

    public void spin(ConcurrentLinkedQueue<Pair<Frame,SocketAddress>> allreceivedframe)
    {
         _processIncoming = _execService.submit(new Runnable() {

            @Override
            public void run() {
                for(;;)
                {
                    if(Thread.currentThread().interrupted())
                    {
                        _log.debug(TAG +"spin : processIncoming: Thread requested to stop. Cosing.......");
                        break;
                    }
                    try
                    {
                        if(_receiveBuffer.size() == 0)
                        {
                            Thread.sleep(100);
                            continue;
                        }
                        /*if there is a packet to prcess*/
                        Pair<ByteBuffer, SocketAddress> poll = _receiveBuffer.poll();
                        byte[] payLoad = poll.getValue0().array();
                        Frame f = _deframer.deframe(payLoad);
                        allreceivedframe.add(Pair.with(f, poll.getValue1()));

                    }catch(InterruptedException ie)
                    {
                        _log.error(TAG + " spin: processIncoming: " + ie.getMessage() + " " + ie.getStackTrace());
                    }
                }

            }
        });

        _processOutgoing = _execService.submit(new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    if(Thread.currentThread().interrupted())
                    {
                        _log.debug(TAG + "spin : processOutgoing: Thread requested to stop. Cosing.......");
                        break;
                    }
                    try{
                        if(_sendBuffer.size() == 0)
                        {
                            Thread.sleep(100);
                            continue;
                        }
                        Pair<ByteBuffer, SocketAddress> toSend = _sendBuffer.poll();
                        byte[] _sendBuf = toSend.getValue0().array();
                        InetSocketAddress destination = (InetSocketAddress)toSend.getValue1();
                        DatagramPacket packet = new DatagramPacket(_sendBuf, _sendBuf.length, destination.getAddress(), destination.getPort());
                        _serverSocket.send(packet);
                        //_serverChannel.send(toSend.getValue0(), toSend.getValue1() );
                        _log.debug(TAG + "Sent out a packet on the channel");

                    }catch(IOException ioe)
                    {
                        _log.error(TAG + " spin : ProcessOutgoing : "+ioe.getMessage() + " " + ioe.getStackTrace());
                    }
                    catch (InterruptedException ie)
                    {
                        _log.error(TAG + " spin : ProcessOutgoing : "+ie.getMessage() + " " + ie.getStackTrace());
                    }
                }
            }
        });
    }

    @Override
    public void stop() {

        _processIncoming.cancel(true);
        _processOutgoing.cancel(true);
        try
        {
            _serverChannel.disconnect();
            _serverChannel.close();

        }catch (IOException ioe)
        {
            _log.error(TAG + "Error while closing DatagramChanel");
        }
    }

    @Override
    public void receive()
    {
        try {
            /*SocketAddress sockAddr = _serverChannel.receive(_receiveBuf);
            if(sockAddr == null)
            {
                Thread.currentThread().sleep(100);
                return;
            }
            _log.debug(TAG + "channel received packet");
            if(_receiveBuf.limit() == 0)
            {
                int i = 1;
            }
            System.out.println("Limit: " + _receiveBuf.limit());
            System.out.println("Posiion: " + _receiveBuf.position());
            System.out.println("Mark" + _receiveBuf.mark());
            byte[] actual = Arrays.copyOfRange(_receiveBuf.array(), 0, _receiveBuf.position());
            Frame f = _deframer.deframe(actual);
            _log.debug(TAG + " type of packet "+ f.type());
            this._receiveBuffer.add(Pair.with( ByteBuffer.wrap(actual),sockAddr));*/
            _log.debug(TAG + "Expecting to receive a packet in socket");
            DatagramPacket packet = new DatagramPacket(_receiveBuf, _receiveBuf.length);
            _serverSocket.receive(packet);
            _log.debug("Received packet");
            int length = packet.getLength();
            byte[] data = Arrays.copyOf(packet.getData(), length);
            Frame f = _deframer.deframe(data);
            _log.debug(TAG + " Type of received packet" + f.type());
            this._receiveBuffer.add(Pair.with( ByteBuffer.wrap(data),new InetSocketAddress(packet.getAddress(), packet.getPort())));

        } catch (IOException e) {
            _log.error(TAG + e.getStackTrace());
        }
        /*catch(InterruptedException ie)
        {
            _log.debug(TAG + ie.getStackTrace());
        }*/
    }
}
