#Introduction

Project aims to create a minimalist torrent file application. There are 2 kinds of nodes in our design. One is the Tracker and the other is the Peer. Only one Tracker will be operational in a torrent network. Peers will be connected with the Tracker and other peers.

The source code is divided into projects, each with their own responsibilities. 
#entity

The Entity Class consists of the Chunk, FileChunkInfo and the Peer Classes. Every File is divided into chunks based on the following logic:

The total number of chunks is always less than 2400. The size of each chunk is a multiple of 16384 Bytes. For optimization, an attempt is made to keep the number of chunks between 1200 to 2400. If the file is not a multiple of 16384, then a leftover is
also maintained. For instance, if the size of the file is 19660800 bytes, a total of 1200 chunks of 16384 bytes are created. But if the size of the file is less than 19660800 bytes, in that case, number of chunks will be less than 1200, since the size of each chunk has to be a multiple of 16384(cannot be less than this).Similarly, if the size of the file is 58900000, 1797 chunks are created. The size of each chunk will be 32768 bytes and there will be a left over of 15904 bytes. Allthis information about the file is stored in the torrent file. 

So the FileChunkInfo Class consists of all the information required to identify a Chunk. Along with these classes, the package also consists of a Parser.The Parser reads the torrent file, given as a parameter, and parses the entire Torrent File and returns a FileChunkInfo Object. This contains all the required information about a file, its chunks and the location of the peers having the chunks.

#progressivefiletransfer: 
Manages the UDP channel for commiunication with other nodes.

Server class opens a UDP channel for both sending and receiving messages. 2 public message queues {_senfBuffer, _receiveBuffer} are provided to all message producers/consumers to push/consume messsages. 2 threads in Server Class run continuously to check if any message needs to be delivered to their destination.

PacketService class is an abstraction layed infront of the Server. This class manages all producers and consumers of messages and handles message delivery to their respective message queues based on process identity(Identifier in the message).
	
/operations/*.java has Callable/Runnable that manage the underlying communication for transfering pieces of files. They need to intercommunicate to process a Job.
	
DownloadResponder class manages response to download requests. It is another layer of abstraction infront of the PacketService class to work with the operations. It runs the required operations in their own thread so that communication pertaining to a particular download request can work independent to all other download requests.
	
UploadResponder class, similar to DownloadResponder manages response to upload requests.  Again all operations are run in their own threads.

#database:
Manages application related information in a persistent storage. This particular implementation uses a MongoDB to store the information about file, its chunks and the source peers for chunks. When the request for a particular chunk arrives or when the command for downloading a file is executed then the database is queried to get relevant information

#torrentfiledownload:
Main application entry point to start downloading a file from peers. Initializes the logging and folders for the file operations and executes the PacketServer in project:-progressivefiletransfer.

DownloadHandler class is a layer of abstraction added to start  and manage the operations required to download a file in chunks from its source peers. Ideally this should allow for multiple files to be downloaded simultaniously.

#fileDistributionPackage

This Package Deals with the Distribution of the File across multiple peers. As as input, the fileName and the Peers address are given in the form localhost:7000. 

An example input could be: Filename localhost:7000 localhost:7001 localhost:7002 localhost:7003.

The File Distribution Package first looks for the file and based on the logic explained in the Entity Section creates a torrent file. The location of the torrent file is specified in the config.properties file. The creation of the torrent file is taken care by the FileDistributionHandler.java class in the method generateTorrentFile(). Following the torrent File creation, the distribution of the Filechunks into the peers mentioned take place. This is handled in the class UploadHandler.java. This class uses the previous protocol PartialFileTransfer to upload the chunks to the destined peers. A few modification to the protocol Progressive File Transfer has been made that is explained in the section progressivefiletransfer.



