/**
 * Created by rabbiddog on 6/14/16.
 */
import entities.Node;

public interface IDatabase {

    Node addNode(Node node);
    boolean deleteNode(Node node);


}
