package entities;

/**
 * Created by rabbiddog on 6/14/16.
 */
public class Node {
    public String get_address() {
        return _address;
    }

    public void set_address(String _address) {
        this._address = _address;
    }

    private String _address;

    public int get_port() {
        return _port;
    }

    public void set_port(int _port) {
        this._port = _port;
    }

    private int _port;
}
