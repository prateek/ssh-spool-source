package com.cloudera.flume.source;
import java.nio.charset.Charset;

public class SshSpoolDirectorySourceConstants {
  public static final String USER_NAME          = "userName";
  public static final String USER_PASS          = "userPass";
  public static final String HOST_NAME          = "hostName";

  public static final String REMOTE_DIR_PATH    = "remotePath";
  public static final String LOCAL_PERSIST_PATH = "localPersistPath";

  public static final String RECORD_DELIMITER = "\n";
  public static final Charset FILE_CHARSET    = Charset.forName( "UTF-8" );
}
