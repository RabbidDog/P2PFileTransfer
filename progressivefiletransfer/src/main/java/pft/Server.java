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

    private DatagramChannel _serverChannel;
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

    public void spin()
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
                        byte[] payLoad = _receiveBuffer.poll().getValue0().array();
                        Frame f = _deframer.deframe(payLoad);
                        /*Send to Thread depending on the identifier*/

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
        try
        {
            ByteBuffer buffer = ByteBuffer.allocate(8096);
            SocketAddress sockAddr = this._serverChannel.receive(buffer);
            /*push data to reveice buffer for further processing*/
            this._receiveBuffer.add(Pair.with( buffer,sockAddr));

        }catch (IOException ioe)
        {
            _log.error(ioe.getMessage() + " " + ioe.getStackTrace());
        }
    }
}
