import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.Logger.*;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.*;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Created by rabbiddog on 6/14/16.
 */


import  pft.file_operation.PftFileManager;
public class FileDistributionApplication {



    private static String _logFilePath;
    private static String fileName;
    private static LinkedList<String> hostList = new LinkedList<String>();
    private static String duplicateFileMessage = "";
    public static void main(String args[])
    {
        /*set up logging*/
        FileDistributionApplication.loadLogFile();
        FileDistributionApplication.setuplogging();
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

    private static void loadLogFile()
    {
        File configFile = new File("config.properties");

        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);
            _logFilePath= props.getProperty("logfile");
            reader.close();

        } catch (FileNotFoundException ex) {
            System.out.print("Error in reading Configuration file while searching for path to Log file");
        } catch (IOException ex) {
            System.out.print("Error in reading Configuration file while searching for path to Log file");
        }
    }

    private static void setuplogging()
    {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setStatusLevel( Level.ERROR);
        builder.setConfigurationName("RollingBuilder");
// create a console appender
        /*AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        builder.add( appenderBuilder );*/
// create a rolling file appender
        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d [%t] %-5level: %msg%n");
        ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
        AppenderComponentBuilder appenderBuilder = builder.newAppender("rolling", "RollingFile")
                .addAttribute("fileName", _logFilePath)
                .addAttribute("filePattern", _logFilePath+".gz")
                .add(layoutBuilder)
                .addComponent(triggeringPolicy);
        builder.add(appenderBuilder);
        builder.add( builder.newLogger( "TestLogger", Level.DEBUG )
                .add( builder.newAppenderRef( "rolling" ) )
                .addAttribute( "additivity", false ) );

        builder.add( builder.newRootLogger( Level.DEBUG )
                .add( builder.newAppenderRef( "rolling" ) ) );
        LoggerContext ctx = Configurator.initialize(builder.build());
    }
}
