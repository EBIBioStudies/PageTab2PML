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

  File infile = new File(config.getFiles().get(0));

  if(!infile.canRead())
  {
   System.err.println("Input file '" + infile.getAbsolutePath() + "' not exist or not readable");
   usage();
   System.exit(1);
  }

  Format fmt = null;

  if(config.getInputFormat().equalsIgnoreCase("tab"))
   fmt = Format.PAGETAB;
  else if(config.getInputFormat().equalsIgnoreCase("json"))
   fmt = Format.JSON;
  else
  {
   System.err.println("Invalid input format '" + config.getInputFormat() + "'");
   usage();
   System.exit(1);
  }

  String text = null;

  try
  {
   text = FileUtil.readUnicodeFile(infile);
  }
  catch(IOException e)
  {
   System.err.println("Input file read ERROR: " + e.getMessage());
   System.exit(1);
  }

/*  
  ErrorCounter ec = new ErrorCounterImpl();
  SimpleLogNode topLn = new SimpleLogNode(Level.SUCCESS, "Submitting file: '" + infile.getAbsolutePath() + "'", ec);

  if(fmt != Format.JSON)
  {
   List<SubmissionInfo> submissions = null;

   LogNode ln = topLn.branch("Parsing Page-Tab file");

   if(config.getInputFormat().equalsIgnoreCase("tab"))
   {
    ParserConfig pc = new ParserConfig();

    pc.setMultipleSubmissions(true);

    PageTabSyntaxParser parser = new PageTabSyntaxParser(new AdHocTagResolver(), pc);

    try
    {
     submissions = parser.parse(text, ln);
    }
    catch(ParserException e)
    {
     System.err.println("Can't parse Page-Tab file: " + e.getMessage());
     System.exit(1);
    }
   }

   if(ec.getErrorCounter() != 0)
   {
    printLog(topLn, config);
    System.exit(1);
   }

   List<Submission> sbList = new ArrayList<Submission>(submissions.size());

   for(SubmissionInfo ps : submissions)
    sbList.add(ps.getSubmission());

   JSONFormatter jsFmt = new JSONFormatter();

   StringBuilder sb = new StringBuilder();

   try
   {
    jsFmt.format(sbList, sb);
   }
   catch(IOException e)
   {
   }

   text = sb.toString();
  }
*/  

  String sess = login(config);

  LogNode topLn = submit(text, fmt, sess, config);
  
  printLog(topLn, config);

 }

 
 private static LogNode submit(String text, Format fmt, String sess, Config config)
 {
  String appUrl = config.getServer();

  if(!appUrl.endsWith("/"))
   appUrl = appUrl + "/";
  
  URL loginURL = null;

  try
  {
   loginURL = new URL(appUrl + submitEndpoint + "?"+SessionKey+"="+URLEncoder.encode(sess, "utf-8"));
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
   
   if( fmt == Format.JSON )
    conn.setRequestProperty("Content-Type", "application/json");
   else if ( fmt == Format.PAGETAB )
    conn.setRequestProperty("Content-Type", "application/pagetab");
   else
   {
    System.err.println("Unsupported format: "+fmt.name());
    System.exit(1);
   }
   
   conn.setDoOutput(true);
   conn.setRequestMethod("POST");
   
   byte[] postData   = text.getBytes( Charset.forName( "UTF-8" ));
   
   conn.setRequestProperty("Content-Length", String.valueOf(postData.length));

   conn.getOutputStream().write( postData );
   
   conn.getOutputStream().close();
   
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

   Utils.printLog(topLn, out, config.getPrintInfoNodes());

   if(out != System.err)
    out.close();
  }
 }

 private static void usage()
 {
 }
}
