package pft;

/**
 * Created by rabbiddog on 6/16/16.
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.Logger.*;

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

}
