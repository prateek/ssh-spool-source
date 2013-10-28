package com.cloudera.sshHelper;

import java.util.ArrayList;

// TODO:Test class. needs to be refactored
public class SshClientDriver {

  public static void main(String[] args)
  {
    SshClientJ sc = new SshClientJ( "localhost", "user", "pass" );
    ArrayList< String > files = sc.getFilesInPath( "/remote-path/" );
    
    System.out.println( "Size of files array: " + files.size() ); 
    for( String file: files )
    {
      byte[] b = sc.getContents( file );
      System.out.println( "filename: " + file + ", size: " + b.length );
    }

  }
}
