package pft;

/**
 * Created by rabbiddog on 6/18/16.
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.Logger.*;
import org.javatuples.*;

public class Client extends PftChannel{


    private Logger _log;

    private ConcurrentLinkedQueue<Pair<SocketAddress, ByteBuffer>> _sendBuffer;
    private ConcurrentLinkedQueue<Pair<SocketAddress, ByteBuffer>> _receiveBuffer;
    private ExecutorService _execService = Executors.newFixedThreadPool(2);

    public DatagramChannel get_clientChannel() {
        return _clientChannel;
    }

    private DatagramChannel _clientChannel;

    public Client(int port)
    {
        _log = LogManager.getRootLogger();
        try
        {
        _clientChannel = DatagramChannel.open();
        _clientChannel.socket().bind(new InetSocketAddress(port));
         _clientChannel.configureBlocking(false);
        }catch (IOException io)
            {
                _log.error("Server constructor while creating DatagramChannel. " + io.getMessage());
            }
    }

    @Override
    public void receive()
    {
        try
        {
            ByteBuffer buffer = ByteBuffer.allocate(8096);
            SocketAddress socketAddr = this._clientChannel.receive(buffer);
            /*push data to reveice buffer for further processing*/
            this._receiveBuffer.add(Pair.with(socketAddr, buffer));

        }catch (IOException ioe)
        {
            _log.error(ioe.getMessage() + " " + ioe.getStackTrace());
        }
    }

    @Override
    public void spin() {

        Future processIncoming = _execService.submit(new Runnable() {

            @Override
            public void run() {

            }
        });

        Future processOutgoing = _execService.submit(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
