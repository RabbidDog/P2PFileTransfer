package torrent;

/**
 * Created by rabbiddog on 6/18/16.
 */
public class TorrentFileParser {
    private static TorrentFileParser ourInstance = new TorrentFileParser();

    public static TorrentFileParser getInstance() {
        return ourInstance;
    }

    private TorrentFileParser() {
    }
}
