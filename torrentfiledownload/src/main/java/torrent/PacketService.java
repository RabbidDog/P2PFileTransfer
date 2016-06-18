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
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Properties;
import java.util.*;

public class PacketService {
    private Logger _log;
    private Server _server;
    private Client _client;
    private int _serverListenPort;
    private int _clientPort;
    private Selector _selector;
    private final int MAX_PACKET;
    private SelectionKey[] _keys; //size:2 key0 for server. key1 for client

    public PacketService()
    {
        _log = LogManager.getRootLogger();
        loadConfiguration();
        _server = new Server(_serverListenPort);
        _client = new Client(_clientPort);

        MAX_PACKET = 8096;


    }

    public void Start()
    {
        /*Selector service*/
        try
        {
            _keys = new SelectionKey[2];
            _selector = Selector.open();
            /*server channel*/
            DatagramChannel serverHandle = _server.get_serverChannel();
            _keys[0] = serverHandle.register(_selector, SelectionKey.OP_READ, _server);

            /*client channel*/
            /*DatagramChannel clientHandle = _client.get_clientChannel();
            _keys[1] = clientHandle.register(_selector, SelectionKey.OP_READ, _client);*/
        }catch (IOException ioe)
        {
            _log.error(ioe.getMessage() + " " + ioe.getStackTrace());
        }
        ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET);
        //spin
        int readyKeys = 0;
        Iterator _keysIterator = null;
        SelectionKey _currentKey = null;
        while(true)
        {
            try
            {
                readyKeys = _selector.select(100);

            }catch (IOException ioe)
            {
                _log.error(ioe.getMessage() +  " " + ioe.getStackTrace());
            }
            /*if no events happened then continue*/
            if(readyKeys>0)
            {
                _keysIterator = _selector.selectedKeys().iterator();
                while(_keysIterator.hasNext())
                {
                    _currentKey = (SelectionKey)_keysIterator.next();
                    /*check if key is registered*/
                    if(_currentKey.equals(_keys[0]))
                    {
                        _keysIterator.remove();
                        /*pass data to channel*/

                        PftChannel ch = (PftChannel)_currentKey.attachment();
                        ch.receive();
                    }
                }
            }
        }
    }

    public void Stop()
    {
        try {
           Set<SelectionKey> keys = _selector.keys();
            Iterator it = keys.iterator();
            while (it.hasNext())
            {
                PftChannel ch = (PftChannel)((SelectionKey)it.next()).attachment();
                ch.stop();
            }
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
            _serverListenPort= Integer.parseInt(props.getProperty("serverPort"));
            _clientPort = Integer.parseInt(props.getProperty("clientPort"));
            reader.close();

        } catch (FileNotFoundException ex) {
            _log.error("Error in reading Configuration file while searching for path to Log file");
        } catch (IOException ex) {
            _log.error("Error in reading Configuration file while searching for path to Log file");
        }
    }
}
