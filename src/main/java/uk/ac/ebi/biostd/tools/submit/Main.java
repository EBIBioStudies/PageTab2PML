package uk.ac.ebi.biostd.tools.submit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import uk.ac.ebi.biostd.tools.util.Format;
import uk.ac.ebi.biostd.tools.util.Utils;
import uk.ac.ebi.biostd.treelog.ConvertException;
import uk.ac.ebi.biostd.treelog.JSON2Log;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.FileUtil;
import uk.ac.ebi.biostd.util.StringUtils;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import com.lexicalscope.jewel.cli.InvalidOptionSpecificationException;

public class Main
{
 static final String SessionKey = "BIOSTDSESS";
 
 static final String authEndpoint = "auth/signin";
 static final String submitEndpoint = "submit/create";
 static final String updateEndpoint = "submit/update";
 static final String deleteEndpoint = "submit/delete";

 public static void main(String[] args)
 {
  Config config = null;

  try
  {
   config = CliFactory.parseArguments(Config.class, args);
  }
  catch(HelpRequestedException e)
  {
   usage();
   System.exit(1);
  }
  catch(InvalidOptionSpecificationException | ArgumentValidationException e)
  {
   System.err.println("Command line processing ERROR: " + e.getMessage());
   usage();
   System.exit(1);
  }

  if(config.getFiles() == null || config.getFiles().size() != 1)
  {
   System.err.println("Command line processing ERROR: invalid number of files specified");
   usage();
   System.exit(1);
  }
  
  File infile = null;
  String delAccNo = null;
  boolean update = false;
  
  if( "update".equalsIgnoreCase(config.getOperation() ) )
   update=true;
  else if("delete".equalsIgnoreCase(config.getOperation()) )
   delAccNo = config.getFiles().get(0);
  else if( ! "new".equalsIgnoreCase(config.getOperation() ) )
  {
   System.err.println("Invalid operation. Valid are: new, update or delete");
   System.exit(1);
  }
   
  if( delAccNo == null )
   infile = new File(config.getFiles().get(0));
  else
  {
   String sess = login(config);
   LogNode topLn = delete(delAccNo, sess, config);
   printLog(topLn, config);
   return;
  }

  if(!infile.canRead())
  {
   System.err.println("Input file '" + infile.getAbsolutePath() + "' not exist or not readable");
   usage();
   System.exit(1);
  }

  Format fmt = null;

  if( "auto".equalsIgnoreCase( config.getInputFormat() ) )
  {
   String ext = null;
   
   int pos = infile.getName().lastIndexOf('.');
   
   if( pos >=0 )
    ext = infile.getName().substring(pos+1);
   
   if( "xlsx".equalsIgnoreCase(ext) )
    fmt = Format.xlsx;
   else if( "xls".equalsIgnoreCase(ext) )
    fmt = Format.xls;
   else if( "json".equalsIgnoreCase(ext) )
    fmt = Format.json;
   else if( "ods".equalsIgnoreCase(ext) )
    fmt = Format.ods;
   else if( "csv".equalsIgnoreCase(ext) )
    fmt = Format.csv;
   else if( "tsv".equalsIgnoreCase(ext) )
    fmt = Format.tsv;
   else
    fmt = Format.csvtsv;
  }
  else
  {
   try
   {
    fmt = Format.valueOf(config.getInputFormat());
   }
   catch(Exception e)
   {
    System.err.println("Invalid input format: '"+config.getInputFormat()+"'");
    System.exit(1);
   }
  }
  

  String text = null;

  try
  {
   if( "auto".equalsIgnoreCase(config.getCharset()) )
    text = FileUtil.readUnicodeFile(infile);
   else
   {
    Charset cs = null;
    
    try
    {
     cs = Charset.forName(config.getCharset());
    }
    catch( Throwable t )
    {
     System.err.println("Invalid charset: "+config.getCharset());
     System.exit(1);
    }
    
    text = FileUtil.readFile(infile,cs);
   }
  }
  catch(IOException e)
  {
   System.err.println("Input file read ERROR: " + e.getMessage());
   System.exit(1);
  }


  String sess = login(config);

  LogNode topLn = submit(infile, fmt, sess, config, update);

  printLog(topLn, config);

 }

 
 private static LogNode delete(String delAccNo, String sess, Config config)
 {
  String appUrl = config.getServer();

  if(!appUrl.endsWith("/"))
   appUrl = appUrl + "/";
  
  URL loginURL = null;

  try
  {
   loginURL = new URL(appUrl + deleteEndpoint + "?id="+delAccNo+"&"+SessionKey+"="+URLEncoder.encode(sess, "utf-8"));
  }
  catch(MalformedURLException e)
  {
   System.err.println("Invalid server URL: " + config.getServer());
   System.exit(1);
  }
  catch(UnsupportedEncodingException e)
  {
  }

  try
  {
   HttpURLConnection conn = (HttpURLConnection) loginURL.openConnection();
   
   String resp = StringUtils.readFully((InputStream)conn.getContent(), Charset.forName("utf-8"));

   conn.disconnect();

   try
   {
    return JSON2Log.convert(resp);
   }
   catch(ConvertException e)
   {
    System.err.println("Invalid server response. JSON log expected");
    System.exit(1);
   }
   
   
  }
  catch(IOException e)
  {
   System.err.println("Connection to server '"+config.getServer()+"' failed: "+e.getMessage());
   System.exit(1);
  }
  
  return null;
 }


 private static LogNode submit(File infile, Format fmt, String sess, Config config, boolean update)
 {
  String appUrl = config.getServer();

  if(!appUrl.endsWith("/"))
   appUrl = appUrl + "/";
  
  URL loginURL = null;

  try
  {
   loginURL = new URL(appUrl + (update? updateEndpoint : submitEndpoint) + "?"+SessionKey+"="+URLEncoder.encode(sess, "utf-8"));
  }
  catch(MalformedURLException e)
  {
   System.err.println("Invalid server URL: " + config.getServer());
   System.exit(1);
  }
  catch(UnsupportedEncodingException e)
  {
  }

  try
  {
   HttpURLConnection conn = (HttpURLConnection) loginURL.openConnection();
   
   
   conn.setDoOutput(true);
   conn.setRequestMethod("POST");
   
   if( fmt == Format.json || fmt == Format.csv || fmt == Format.tsv || fmt == Format.csvtsv )
   {
    conn.setRequestProperty("Content-Type", fmt.getContentType()+"; charset=utf-8");
   
    Charset cs = null;
    
    try
    {
     cs = Charset.forName(config.getCharset());
    }
    catch( Throwable t )
    {
     System.err.println("Invalid charset: "+config.getCharset());
     System.exit(1);
    }
    
    String txt = FileUtil.readFile(infile, cs);
    
    byte[] postData   = txt.getBytes( Charset.forName( "UTF-8" ));
    
    conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
    conn.getOutputStream().write( postData );
    conn.getOutputStream().close();

   }
   else
   {
    conn.setRequestProperty("Content-Type",fmt.getContentType());
    
    byte[] data = FileUtil.readBinFile(infile);
   
    conn.setRequestProperty("Content-Length", String.valueOf(data.length));
    conn.getOutputStream().write( data );
    conn.getOutputStream().close();
   }

   
   String resp = StringUtils.readFully((InputStream)conn.getContent(), Charset.forName("utf-8"));

   conn.disconnect();

   try
   {
    return JSON2Log.convert(resp);
   }
   catch(ConvertException e)
   {
    System.err.println("Invalid server response. JSON log expected");
    System.exit(1);
   }
   
   
  }
  catch(IOException e)
  {
   System.err.println("Connection to server '"+config.getServer()+"' failed: "+e.getMessage());
   System.exit(1);
  }
  
  return null;
 }


 private static String login( Config config )
 {
  String appUrl = config.getServer();

  if(!appUrl.endsWith("/"))
   appUrl = appUrl + "/";
  
  URL loginURL = null;

  try
  {
   loginURL = new URL(appUrl + authEndpoint + "?login=" + URLEncoder.encode(config.getUser(), "utf-8") + "&password="
     + URLEncoder.encode(config.getPassword(), "utf-8"));
  }
  catch(MalformedURLException e)
  {
   System.err.println("Invalid server URL: " + config.getServer());
   System.exit(1);
  }
  catch(UnsupportedEncodingException e)
  {
  }

  try
  {
   HttpURLConnection conn = (HttpURLConnection) loginURL.openConnection();
   String resp = StringUtils.readFully((InputStream)conn.getContent(), Charset.forName("utf-8"));

   conn.disconnect();

   if( ! resp.startsWith("OK") )
   {
    System.err.println("Login failed");
    System.exit(1);
   }
   
   String sessId = getSessId(resp);
   
   if( sessId == null )
   {
    System.err.println("Invalid server response. Can't extract session ID");
    System.exit(1);
   }
   
   return sessId;
  }
  catch(IOException e)
  {
   System.err.println("Connection to server '"+config.getServer()+"' failed: "+e.getMessage());
   System.exit(1);
  }
  
  return null;
 }
 
 private static String getSessId(String resp)
 {
  int pos  = resp.indexOf("sessid:");
  
  if( pos == -1 )
   return null;
  
  pos += "sessid:".length();
  
  while( Character.isWhitespace(resp.charAt(pos)))
   pos++;
  
  int beg = pos;
  
  while( Character.isLetterOrDigit(resp.charAt(pos)))
   pos++ ;

  
  return resp.substring(beg,pos);
 }

 static void printLog(LogNode topLn, Config config)
 {
  if( topLn == null )
   return;
  
  SimpleLogNode.setLevels(topLn);

  if(topLn.getLevel() != Level.SUCCESS || config.getPrintInfoNodes())
  {
   PrintStream out = null;

   if(config.getLogFile().equals("-"))
    out = System.err;
   else
   {
    File lf = new File(config.getLogFile());

    if(lf.exists() && !lf.canWrite())
    {
     System.err.println("Log file '" + config.getLogFile() + "' is not writable");
     System.exit(1);
    }

    try
    {
     out = new PrintStream(lf, "UTF-8");
    }
    catch(FileNotFoundException e)
    {
     System.err.println("Can't open log file '" + config.getLogFile() + "'");
     System.exit(1);
    }
    catch(UnsupportedEncodingException e)
    {
     System.err.println("UTF-8 encoding is not supported");
     System.exit(1);
    }

   }

   Utils.printLog(topLn, out, config.getPrintInfoNodes()? Level.DEBUG : Level.WARN );

   if(out != System.err)
    out.close();
  }
 }

 static void usage()
 {
  System.err.println("Usage: java -jar PTSubmit -o new|update|delete -s serverURL -u user -p [password] [-h] [-i in fmt] [-c charset] [-d] [-l logfile] <input file|AccNo>");
  System.err.println("-h or --help print this help message");
  System.err.println("-i or --inputFormat input file format. Can be json,tsv,csv,xls,xlsx,ods. Default is auto (by file extension)");
  System.err.println("-c or --charset file charset (for text files only)");
  System.err.println("-s or --server server endpoint URL");
  System.err.println("-u or --user user login");
  System.err.println("-p or --password user password");
  System.err.println("-o or --operation requested operation. Can be new, update or delete");
  System.err.println("-d or --printInfoNodes print info messages along with errors and warnings");
  System.err.println("-l or --logFile defines log file. By default stdout");
  System.err.println("<input file> PagaTab input file. Supported UCS-2 (UTF-16), UTF-8 CSV or TSV or MS Excel XML files");
  System.err.println("<output file> XML output file. '-' means output to stdout");
 }
}
