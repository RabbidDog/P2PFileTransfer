import pft.Deframer;
import pft.Framer;
import pft.frames.DataRequest;

/**
 * Created by ankur on 20.06.2016.
 */
public class DataRequestTest {
    public static void main(String[] args){
        long offset = 1332;
        long length = 11112;
        String fileName = "fileName";
        byte[] sha = "11111111111111111111".getBytes();
        Framer framer = new Framer();
        Deframer deframer = new Deframer();
        DataRequest dataRequestToTest;
        DataRequest dataRequest = new DataRequest(1,offset,length);
        byte[] datarequestWithoutFile = framer.frame(dataRequest);
        dataRequestToTest = (DataRequest) deframer.deframe(datarequestWithoutFile);
        System.out.println(dataRequestToTest.fileName() + " " + dataRequestToTest.identifier() + " " + dataRequestToTest.offset() + " " + dataRequestToTest.length());

        DataRequest dataRequestFile = new DataRequest(1,fileName,sha,offset,length);
        byte[] datarequestWithFile = framer.frame(dataRequestFile);
        dataRequestToTest = (DataRequest) deframer.deframe(datarequestWithFile);
        System.out.println(dataRequestToTest.identifier() + " " + dataRequestToTest.fileName() + " " + dataRequestToTest.offset() + " " + dataRequestToTest.length());


    }
}
