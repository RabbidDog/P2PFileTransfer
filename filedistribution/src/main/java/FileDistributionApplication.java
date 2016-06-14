import java.util.LinkedList;

/**
 * Created by rabbiddog on 6/14/16.
 */

import  pft.*;
public class FileDistributionApplication {


    private static String fileName;
    private static LinkedList<String> hostList = new LinkedList<String>();
    private static String duplicateFileMessage = "";
    public static void main(String args[])
    {
        if(args.length < 2){
            //Check if atleast one host and filename is added
            System.out.println("Print Usage here...");
            System.exit(0);
        }
        parse(args);

        addInformationToDatabase();

    }

    private static void addInformationToDatabase() {
        System.out.println("Adding hostlist and filename to database");
        System.out.println("FileName:" + fileName);
        System.out.println("HostNames"+ hostList);
        System.out.println(duplicateFileMessage);

    }

    private static void parse(String[] args) {
        boolean fileParsed = false;
        for(int i=0;i<args.length;i++) {
            if(isHost(args[i])) {
                String trimmedHost = trimHost(args[i]);
                hostList.add(trimmedHost);
            }
            else {
                if(fileParsed == true){
                    //Print a message at the end that ignoring second file
                    duplicateFileMessage += "Duplicate file" + args[i] + " will be ignored. \n";

                }
                //Store Filename
                //Set file parsed to true
                else{
                    fileParsed = true;
                    fileName = args[i];
                }

            }
        }
    }

    private static String trimHost(String arg) {
       // System.out.println("Substring: " + arg.substring(0,(arg.length() -1)));
        if(arg.substring(0,9).equals("localhost")) return "localhost" + arg.substring(10, arg.length());
        return arg;
    }

    private static boolean isHost(String arg) {
        String delimiter = ":";
        String tokens[] = arg.split(delimiter);
        if(tokens.length == 1) return false;
        return true;

    }
}
