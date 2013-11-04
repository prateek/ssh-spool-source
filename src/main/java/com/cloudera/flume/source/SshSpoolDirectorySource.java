package com.cloudera.flume.source;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;
import org.apache.flume.PollableSource;
import org.apache.flume.EventDeliveryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.sshHelper.SshClientJ;

public class SshSpoolDirectorySource extends AbstractSource 
  implements Configurable, PollableSource 
{
  private static final Logger logger =
      LoggerFactory.getLogger(SshSpoolDirectorySource.class);
  
  private String hostName, userName, userPass;
  private String remoteSpoolPath;
  private String localPersistPath;

  private SshClientJ            sshClient ;
  private SshSpoolStateManager  filesState;

  @Override
  public void configure(Context context) {
    hostName = context.getString( SshSpoolDirectorySourceConstants.HOST_NAME );
    userName = context.getString( SshSpoolDirectorySourceConstants.USER_NAME );
    userPass = context.getString( SshSpoolDirectorySourceConstants.USER_PASS );

    remoteSpoolPath  = context.getString( SshSpoolDirectorySourceConstants.REMOTE_DIR_PATH );
    localPersistPath = context.getString( SshSpoolDirectorySourceConstants.LOCAL_PERSIST_PATH );

    sshClient  =  new SshClientJ( hostName, userName, userPass );
    filesState =  new SshSpoolStateManager( localPersistPath );
  }

  @Override
  public void start() { }

  @Override
  public void stop () {
    filesState.saveState();
  }

  @Override
  public Status process() throws EventDeliveryException {

    // Get pending files
    ArrayList< String > pendingFiles;
    try {
      ArrayList< String > files = sshClient.getFilesInPath( remoteSpoolPath );
      filesState.addProcessingList( files );
      pendingFiles = filesState.getPending();
    } catch (Exception e) {
      logger.error( e.toString() );
      return Status.BACKOFF;
    }   

    // Start transaction
    for( String file: pendingFiles )
    {
      try {

        filesState.markInProcess( file );
        File tempFile = sshClient.getTempLocalInstance( file );
        if( tempFile == null ) {
          logger.error( "Unable to retrieve contents: " + file );
          logger.error( "Marking file in error state: " + file );
          filesState.markError( file );
          continue;
        }

        Map< String, String > headers = new HashMap< String, String >();
        headers.put( "filename", file );
        int line_counter = 1;

        Scanner s = new Scanner( tempFile )
          .useDelimiter( SshSpoolDirectorySourceConstants.RECORD_DELIMITER);

        while( s.hasNext() ) {
          String record = s.next();
          logger.debug("f:" + file + ", l: " + line_counter + ", r: " + record );

          headers.put( "line_number", Integer.toString(line_counter++) );
          Event e = EventBuilder.withBody( record, 
              SshSpoolDirectorySourceConstants.FILE_CHARSET, headers );
          // Store the Event into this Source's associated Channel(s)
          getChannelProcessor().processEvent(e);
        }
        s.close();

        filesState.markFinished( file );
        logger.info("Successfully parsed: " + file );
      } catch (Throwable t) {
        // Log exception, handle individual exceptions as needed
        logger.error( "While processing: " + file + " - " + t.toString() );
        filesState.markError( file );
      } 

    }

    return Status.READY;
  }
}
