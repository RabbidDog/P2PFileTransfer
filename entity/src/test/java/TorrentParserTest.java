import entity.FileChunkInfo;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ankur on 18.06.2016.
 */
public class TorrentParserTest {
    public static void main(String [] args){
        TorrentParser torrentParser = new TorrentParser("D:\\torrents\\a.mp3.torrent");
        ConcurrentHashMap<Long, String[]> chunkPeerMap = new ConcurrentHashMap<Long, String[]>();

        try {
            FileChunkInfo f = torrentParser.fileParser();
            System.out.println(f.FileName);
            System.out.println(f.size);
            System.out.println(f.chunkCount);
            System.out.println(f.chunkSize);
            Iterator it = f.chunkInfo.entrySet().iterator();
            while(it.hasNext()){
                ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry)it.next();
                String[] value = (String[]) pair.getValue();
                System.out.println(pair.getKey() + " " + value[0] + value[1]);

            }



        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
