package pft;

/**
 * Created by rabbiddog on 6/18/16.
 */
public abstract class PftChannel {

    public abstract void receive();
    public abstract void spin();
    public abstract  void stop();
}
