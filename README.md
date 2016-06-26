#Introduction

Project aims to create a minimalist torrent file application. There are 2 kinds of nodes in our design. One is the Tracker and the other is the Peer. Only one Tracker will be operational in a torrent network. Peers will be connected with the Tracker and other peers.

The source code is divided into projects, each with their own responsibilities. 

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



