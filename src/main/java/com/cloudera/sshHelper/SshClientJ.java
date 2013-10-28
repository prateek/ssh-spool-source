package com.cloudera.sshHelper;

import java.util.List;
import java.util.Vector;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.File;

import org.apache.commons.io.FileUtils; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;

public class SshClientJ {

  private static final Logger logger =
      LoggerFactory.getLogger(SshClientJ.class);
  
  public static final int SSH_PORT = 22;

  protected String hostName, userName, userPass;

  public SshClientJ( String hostName, String userName, String userPass )
  {
    this.hostName = hostName;
    this.userName = userName;
    this.userPass = userPass;
  }

  public ArrayList< String > getFilesInPath( String path ) 
  {
    ArrayList< String > ret = new ArrayList< String >();

    SSHClient client = new SSHClient();
    client.addHostKeyVerifier(new PromiscuousVerifier());

    try {
      client.connect(hostName);
      client.authPassword(userName, userPass);

      SFTPClient sftp                  = client.newSFTPClient();
      List< RemoteResourceInfo > files = sftp.ls( path );
      for( RemoteResourceInfo file: files )
      {
        ret.add( file.getPath() );
      }

      sftp.close();
      client.disconnect();

    } catch( Exception e ) {
      logger.error( e.toString() );
    } 

    return ret;
  }

  public byte[] getContents( String filePath ) 
  {
    byte[] buffer    = null;
    SSHClient client = new SSHClient();
    // required if host is not in knownLocalHosts
    client.addHostKeyVerifier(new PromiscuousVerifier());

    //TODO: stream instead of copy entire file
    try {
      client.connect(hostName);
      client.authPassword(userName, userPass);

      File tempFile   = File.createTempFile("tmp", null, null);
      tempFile.deleteOnExit();

      SFTPClient sftp = client.newSFTPClient();
      sftp.get(filePath, new FileSystemFile(tempFile));

      buffer = FileUtils.readFileToByteArray( tempFile );
      sftp.close();
      client.disconnect();

    } catch( Exception e ) { 
      //TODO: this needs to be more robust than just catching all exceptions
      e.printStackTrace();
      logger.error( e.toString() );
    } 
    
    return buffer;
  }

}
