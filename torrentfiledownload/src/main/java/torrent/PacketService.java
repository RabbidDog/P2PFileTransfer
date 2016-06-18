package torrent;

/**
 * Created by rabbiddog on 6/16/16.
 */
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.Logger.*;

import pft.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.Properties;

public class PacketService {
    private Logger _log;
    private Server _server;
    private int _serverListenPort;
    private int _clientPort;
    private Selector _selector;

    public PacketService()
    {
        _log = LogManager.getRootLogger();
        loadConfiguration();
        _server = new Server(_serverListenPort);



    }

    public void start()
    {
        /*Selector service*/
        try
        {
            _selector = Selector.open();
            DatagramChannel temp = _server.get_serverChannel();
            int s = temp.validOps();
            temp.register(_selector, temp.validOps(), null);

        }catch (IOException ioe)
        {
            _log.error(ioe.getMessage() + " " + ioe.getStackTrace());
        }
    }

    public void Stop()
    {
        try {
            _selector.close();
        }catch (IOException ioe)
        {
            _log.error(ioe.getMessage() + " " + ioe.getStackTrace());
        }
    }

    private void loadConfiguration()
    {
        File configFile = new File("config.properties");

        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);
            _serverListenPort= Integer.parseInt(props.getProperty("server"));
            reader.close();

        } catch (FileNotFoundException ex) {
            _log.error("Error in reading Configuration file while searching for path to Log file");
        } catch (IOException ex) {
            _log.error("Error in reading Configuration file while searching for path to Log file");
        }
    }
}
