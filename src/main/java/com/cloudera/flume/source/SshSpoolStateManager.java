package com.cloudera.flume.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshSpoolStateManager
{
  private static final Logger logger =
      LoggerFactory.getLogger(SshSpoolDirectorySource.class);

  public enum FileProcessingState {
    PENDING, IN_PROCESS, FAILED, SUCCEDED
  }

  protected HashMap< String, FileProcessingState > stateMap;
  protected String filePath;

  public SshSpoolStateManager( String filePath ) throws IOException
  {
    this.filePath = filePath + "/ssh-spool-state.out";
    this.stateMap = new HashMap< String, FileProcessingState >();

    loadMap();
    
    // Mark any in-process files as errornoues
    markUnprocessedAsError();
  }

  public ArrayList< String > getPending() 
  {
    ArrayList< String > pending = new ArrayList< String >();
    for( Map.Entry< String, FileProcessingState > entry: stateMap.entrySet() )
    {
      if( entry.getValue() == FileProcessingState.PENDING )
      {
        pending.add( entry.getKey() );
      }
    }
    
    return pending;
  }

  public void addProcessingList( ArrayList< String > paths ) throws IOException
  {
    for( String file: paths ) 
    {
      if( !stateMap.containsKey( file ) ) 
      {
        stateMap.put( file, FileProcessingState.PENDING );
      }
    }
    saveState();
  }

  public void markFinished( String file ) throws IOException
  {
    stateMap.put( file, FileProcessingState.SUCCEDED );
    saveState();
  }

  public void markError( String file ) throws IOException
  {
    stateMap.put( file, FileProcessingState.FAILED );
    saveState();
  }

  public void markInProcess( String file ) throws IOException
  {
    stateMap.put( file, FileProcessingState.IN_PROCESS );
    saveState();
  }

  public void markUnprocessedAsError() throws IOException
  {
    for( Map.Entry< String, FileProcessingState > entry: stateMap.entrySet() )
    {
      if( entry.getValue() == FileProcessingState.IN_PROCESS )
      {
        logger.info( entry.getKey() + " is marked as in_process, updating to error " );
        entry.setValue( FileProcessingState.FAILED );
      }
    }
    saveState();
  }

  public void saveState() throws IOException
  {
    saveMap();
  }

  protected void saveMap() throws IOException {
    ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream( filePath ));
    oos.writeObject(stateMap);
    oos.close();
  }

  protected void loadMap() 
  {
    try{ 
      ObjectInputStream ois = new ObjectInputStream( new FileInputStream( new File(filePath) ));
      Object readMap = ois.readObject();
      if(readMap != null && readMap instanceof HashMap) {
        stateMap.putAll((HashMap) readMap);
      }
      ois.close();
    } catch ( FileNotFoundException e ) {
    } catch ( IOException e ) {
    } catch ( ClassNotFoundException e ) {
      // indicates the file doesnt exist. 
      // no worries, we'll start from scratch
    }
  }

}
