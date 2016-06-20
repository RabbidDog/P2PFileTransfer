package pft;

/**
 * Created by rabbiddog on 6/16/16.
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.frames.Frame;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
    public final ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>> _sendBuffer;
    public final ConcurrentLinkedQueue<Pair<ByteBuffer, SocketAddress>> _receiveBuffer;
    private ExecutorService _execService = Executors.newFixedThreadPool(2);
    Future _processIncoming, _processOutgoing;
    Framer _framer;
    Deframer _deframer;
    public Server(int port)
    {
        _log = LogManager.getRootLogger();
        _framer = new Framer();
        _deframer = new Deframer();

        _sendBuffer = new ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>>();
        _receiveBuffer = new ConcurrentLinkedQueue<Pair<ByteBuffer ,SocketAddress>>();
        try
        {
             _serverChannel = DatagramChannel.open();
            _serverChannel.socket().bind(new InetSocketAddress(port));
            _serverChannel.configureBlocking(false);
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
                        _serverChannel.send(toSend.getValue0(), toSend.getValue1() );

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
        ByteBuffer buf = ByteBuffer.allocate(8096);
        try {
            SocketAddress sockAddr = _serverChannel.receive(buf);
            byte[] actual = Arrays.copyOfRange(buf.array(), 0, buf.position());
            this._receiveBuffer.add(Pair.with( ByteBuffer.wrap(actual),sockAddr));
            buf.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
