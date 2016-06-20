import entity.Chunk;
import entity.FileChunkInfo;
import entity.Peer;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ankur on 18.06.2016.
 */
public class TorrentParserTest {
    public static void main(String [] args){
        TorrentParser torrentParser = new TorrentParser("D:\\torrents\\a.mp3.torrent");
        List<Chunk> chunks = new LinkedList<Chunk>();
        List<Peer> peers = new LinkedList<Peer>();
        try {
            FileChunkInfo f = torrentParser.fileParser();
            System.out.println(f.FileName);
            System.out.println(f.size);
            System.out.println(f.chunkCount);
            System.out.println(f.chunkSize);
            chunks = f.chunkInfo;
            for(int i=0;i<chunks.size();i++){
                System.out.print(chunks.get(i).offset + " ");
                System.out.print(chunks.get(i).length + " ");
                System.out.print(chunks.get(i).isDownloaded + " ");
                peers = chunks.get(i).peerList;
                for(int j = 0; j<peers.size();j++){
                    System.out.print(peers.get(j).address + " ");
                    System.out.print(peers.get(j).port + " ");

                }
            System.out.println();

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
