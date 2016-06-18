package pft;

/**
 * Created by rabbiddog on 6/16/16.
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Server {


    private Logger _log;

    public DatagramChannel get_serverChannel() {
        return _serverChannel;
    }

    private DatagramChannel _serverChannel;

    public Server(int port)
    {
        _log = LogManager.getRootLogger();
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

    public SocketAddress receive(ByteBuffer buffer)
    {
        try {
            return _serverChannel.receive(buffer);
        }catch (IOException ioe)
        {
            _log.error(ioe.getMessage() + " " + ioe.getStackTrace());
            return null;
        }

    }

}
