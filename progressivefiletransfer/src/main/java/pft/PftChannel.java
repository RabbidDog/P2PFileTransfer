package pft;

import org.javatuples.Pair;
import pft.frames.Frame;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by rabbiddog on 6/18/16.
 */
public abstract class PftChannel {

    public abstract void receive();
    public abstract void spin(ConcurrentLinkedQueue<Pair<Frame,SocketAddress>> allreceivedframe);
    public abstract  void stop();
}
