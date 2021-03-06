package pft;

/**
 * Created by rabbiddog on 6/16/16.
 */

import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import pft.file_operation.IFileFacade;
import pft.file_operation.PftFileManager;
import pft.frames.DataRequest;
import pft.frames.DataResponse;
import pft.frames.Frame;
import pft.frames.PartialUpoadRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketService implements Runnable{
    private Logger _log;
    public Server _server;
    //private Client _client;
    private int _serverListenPort;
    private int _clientPort;
    private Selector _selector;
    private final int MAX_PACKET;
    private SelectionKey[] _keys; //size:2 key0 for server. key1 for client
    private final String TAG = "PacketService ";
    private volatile ConcurrentHashMap<Integer, ExecutorService> _chunkToThreadMap;
    public ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataRequest>> _dataRequestQueueForIdentifier; //will be used by upload processes
    public ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataResponse>> _dataResponseQueueForIdentifier; //will be used by download processes
    public ConcurrentLinkedQueue<PartialUpoadRequest> _incomingUploadRequests; //new upload requests;
    public ConcurrentLinkedQueue<Pair<Frame,SocketAddress>> _allreceivedframe;
    public ConcurrentHashMap<String, IFileFacade> _fileManagerMap;
    private Random _rand;
    private final String _mainFolder;


    public PacketService(String mainFolder)
    {
        _log = LogManager.getRootLogger();
        loadConfiguration();
        /*create receive buffer by Identifer, where processes can register their buffer*/
        _dataRequestQueueForIdentifier = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataRequest>> ();
        _dataResponseQueueForIdentifier = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DataResponse>>();
        _incomingUploadRequests = new ConcurrentLinkedQueue<PartialUpoadRequest>();
        _allreceivedframe = new ConcurrentLinkedQueue<Pair<Frame,SocketAddress>>();
        _fileManagerMap = new ConcurrentHashMap<String, IFileFacade> ();
            _server = new Server(_serverListenPort);
        _mainFolder = mainFolder;
        //_client = new Client(_clientPort);

        MAX_PACKET = 8096;
        _chunkToThreadMap = new ConcurrentHashMap<Integer, ExecutorService>();
        _rand = new Random();
    }

    @Override
    public void run()
    {

        /*Selector service*/
       /* try
        {
            _keys = new SelectionKey[2];
            _selector = Selector.open();
            /*server channel*/
            /*DatagramChannel serverHandle = _server.get_serverChannel();
            _keys[0] = serverHandle.register(_selector, SelectionKey.OP_READ, _server);
            _log.debug(TAG + "Started PacketService");
            /*client channel*/
            /*DatagramChannel clientHandle = _client.get_clientChannel();
            _keys[1] = clientHandle.register(_selector, SelectionKey.OP_READ, _client);
        }catch (IOException ioe)
        {
            _log.error(TAG + ioe.getMessage() + " " + ioe.getStackTrace());
        }*/
        ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET);
        //spin server
        _server.spin(_allreceivedframe);
        //thread to process incoming requests
        ExecutorService exector = Executors.newFixedThreadPool(2); //only one is required
        exector.execute(new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    if(Thread.currentThread().interrupted())
                    {
                        _log.debug(TAG + "Polling of receving packets will stop ");
                        break;
                    }
                    Pair<Frame, SocketAddress> poll = _allreceivedframe.poll();
                    if(null == poll)
                        continue;
                    Frame f = poll.getValue0();
                    if(f instanceof DataRequest)
                    {
                        /*if queue not present then this is a new download request
                        * request must have identifer, file name*/
                        DataRequest req = (DataRequest) f;
                        if(_dataRequestQueueForIdentifier.containsKey(req.identifier()))
                        {
                            _log.debug(TAG + "Found existing datreq buffer. Pusing request in buffer");
                            ConcurrentLinkedQueue<DataRequest> reqBuffer = _dataRequestQueueForIdentifier.get(req.identifier());
                            reqBuffer.add(req);
                        }else
                        {
                            if(req.identifier() == 0)
                            {
                                _log.debug(TAG + "New Datarequest with identifier 0. Will not process");
                                continue;
                            }
                            ConcurrentLinkedQueue<DataRequest> reqBuffer = new ConcurrentLinkedQueue<DataRequest>();
                            reqBuffer.add(req);
                            _dataRequestQueueForIdentifier.putIfAbsent(req.identifier(), reqBuffer);
                            IFileFacade fm;
                            if(_fileManagerMap.containsKey(req.fileName()))
                            {
                                _log.debug(TAG + " Existing Filemanager not found. Create new one");
                                fm = new PftFileManager(_mainFolder+req.fileName());
                                _fileManagerMap.putIfAbsent(req.fileName(), fm);
                            }
                            else{
                                _log.debug(TAG + " Existing Filemanager found");
                                fm = _fileManagerMap.get(_mainFolder+req.fileName());
                            }
                            DownloadResponder.respond(req, poll.getValue1(), _server._sendBuffer, reqBuffer, fm);
                        }
                    }
                    else if(f instanceof DataResponse)
                    {
                        DataResponse resp = (DataResponse)f;

                        /*a buffer with identifier must be present*/
                        if(_dataResponseQueueForIdentifier.containsKey(resp.identifier()))
                        {
                            ConcurrentLinkedQueue<DataResponse> respBuffer = _dataResponseQueueForIdentifier.get(resp.identifier());
                            _log.debug(TAG + "Dataresponse for know responseBuffer received");
                            _log.debug(TAG + "DataresponseBuffer size before add"+respBuffer.size());
                            respBuffer.add(resp);
                            _log.debug(TAG + "DataresponseBuffer size after add"+respBuffer.size());
                        }else
                        {
                            _log.debug(TAG + " DataResponse is unexpected identifier received. Packet will not be processed");
                        }

                    }else if(f instanceof PartialUpoadRequest)
                    {
                        PartialUpoadRequest req = (PartialUpoadRequest)f;
                        /*dataresponse buffer for identifier will not be present for a new upload request*/
                        if(_dataResponseQueueForIdentifier.containsKey(req.identifier()))
                        {
                            _log.debug(TAG + " Duplicate PartialUpoadRequest with identifier "+req.identifier() + " received. Ignoring packet");
                        }
                        else
                        {
                            _log.debug(TAG + " New PartialUpoadRequest with identifier" + req.identifier() + " reeived recognized");
                            ConcurrentLinkedQueue<DataResponse> respBuffer = new ConcurrentLinkedQueue<DataResponse>();
                            _dataResponseQueueForIdentifier.putIfAbsent(req.identifier(), respBuffer);
                            IFileFacade fm;
                            if(_fileManagerMap.containsKey(req.fileName()))
                            {
                                _log.debug(TAG + " Existing Filemanager not found. Create new one");
                                fm = new PftFileManager(_mainFolder+req.fileName());
                                _fileManagerMap.putIfAbsent(req.fileName(), fm);
                            }
                            else{
                                _log.debug(TAG + " Existing Filemanager found");
                                fm = _fileManagerMap.get(_mainFolder+req.fileName());
                            }
                            UploadResponder.respond(req, poll.getValue1(), _server._sendBuffer, respBuffer, fm);
                        }
                    }else
                    {
                        _log.debug(TAG + "Unexpected type of packet reveiced");
                    }
                }
            }
        });


        int readyKeys = 0;
        Iterator _keysIterator = null;
        SelectionKey _currentKey = null;

        Thread receiveTh = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                    _server.receive();
            }
        });
        receiveTh.start();
        /*while (true)
        {
            _server.receive();
        }*/

        /*while(true)
        {
            try
            {
                readyKeys = _selector.select(100);

            }catch (IOException ioe)
            {
                _log.error(ioe.getMessage() +  " " + ioe.getStackTrace());
            }
            /*if no events happened then continue*/
            /*if(readyKeys>0)
            {
                _log.debug(TAG + "Selector key");
                _keysIterator = _selector.selectedKeys().iterator();
                while(_keysIterator.hasNext())
                {
                    _currentKey = (SelectionKey)_keysIterator.next();
                    /*check if key is registered*/
                   /* if(_currentKey.equals(_keys[0]))
                    {
                        _keysIterator.remove();
                        /*pass data to channel*/

                       /* PftChannel ch = (PftChannel)_currentKey.attachment();
                        ch.receive();
                    }
                }
            }
        }*/


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
            _serverListenPort = Integer.parseInt( props.getProperty("hostname").split(":")[1]);

            reader.close();

        } catch (FileNotFoundException ex) {
            _log.error("Error in reading Configuration file while searching for path to Log file");
        } catch (IOException ex) {
            _log.error("Error in reading Configuration file while searching for path to Log file");
        }
    }
}
