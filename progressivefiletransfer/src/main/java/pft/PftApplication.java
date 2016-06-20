package pft;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.Logger.*;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.*;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by rabbiddog on 6/18/16.
 */
public class PftApplication {
    public static String logFilePath;
    public static String mainFolder;

    public static void main(String [] args) {

        PftApplication.loadLogFile();
        PftApplication.setuplogging();

        /*currently running only as a listner*/
        PacketService pckService = new PacketService(mainFolder);
        Thread th = new Thread(pckService);
        th.start();
    }

    private static void loadLogFile()
    {
        File configFile = new File("config.properties");

        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);
            logFilePath= props.getProperty("logfile");
            mainFolder = props.getProperty("pathServer");
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
        AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        builder.add( appenderBuilder );
// create a rolling file appender
        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d [%t] %-5level: %msg%n");
        ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
        appenderBuilder = builder.newAppender("rolling", "RollingFile")
                .addAttribute("fileName", logFilePath)
                .addAttribute("filePattern", logFilePath+".gz")
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
