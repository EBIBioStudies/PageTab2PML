package uk.ac.ebi.biostd.pt2pml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ebi.biostd.export.SubmissionPageMLFormatter;
import uk.ac.ebi.biostd.model.PreparedSubmission;
import uk.ac.ebi.biostd.pagetab.PageTabSyntaxParser2;
import uk.ac.ebi.biostd.pagetab.ParserConfig;
import uk.ac.ebi.biostd.pagetab.ParserException;
import uk.ac.ebi.biostd.treelog.ErrorCounter;
import uk.ac.ebi.biostd.treelog.ErrorCounterImpl;
import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.FileUtil;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.InvalidOptionSpecificationException;

public class Main
{
 static final Character EE = new Character('\u2502');
 static final Character EL = new Character('\u2514');
 static final Character TEE = new Character('\u251C');
 
 public static void main( String[] args )
 {
  Config config = null;
    
  try
  {
   config = CliFactory.parseArguments(Config.class, args);
  }
  catch(InvalidOptionSpecificationException e)
  {
   System.err.println("Command line processing ERROR: "+e.getMessage());
   usage();
   System.exit(1);
  }
  
  if( config.getFiles() == null || config.getFiles().size() != 2 )
  {
   System.err.println("Command line processing ERROR: invalid number of files specified");
   usage();
   System.exit(1);
  }
  
  File infile = new File( config.getFiles().get(0) );
  
  if( ! infile.canRead() )
  {
   System.err.println("Input file '"+infile.getAbsolutePath()+"' not exist or not readable");
   usage();
   System.exit(1);
  }
  
  File outfile = new File( config.getFiles().get(1) );
  
  if(  outfile.exists() && ! outfile.canWrite() )
  {
   System.err.println("Output file '"+outfile.getAbsolutePath()+"' is not writable");
   usage();
   System.exit(1);
  }
  

  ParserConfig pc = new ParserConfig();
  
  pc.setMultipleSubmissions(true);
  
  PageTabSyntaxParser2 parser = new PageTabSyntaxParser2(new AdHocTagResolver(), pc);
  
  String pTab = null;
  
  try
  {
   pTab = FileUtil.readUnicodeFile(infile);
  }
  catch(IOException e)
  {
   System.err.println("Input file read ERROR: "+e.getMessage());
   System.exit(1);
  }
  
  ErrorCounter ec = new ErrorCounterImpl();
  
  SimpleLogNode topLn = new SimpleLogNode(Level.SUCCESS, "Parsing file: '"+infile.getAbsolutePath()+"'", ec);
  
  List<PreparedSubmission> submissions = null;
  
  try
  {
   submissions = parser.parse(pTab, topLn);
  }
  catch(ParserException e)
  {
   System.err.println("Can't parse Page-Tab file: "+e.getMessage());
   System.exit(1);
  }
  

  if( topLn.getLevel() != Level.SUCCESS || config.getPrintInfoNodes() )
  {
   PrintStream out = null;
   
   if( config.getLogFile().equals("-"))
    out = System.err;
   else
   {
    File lf = new File( config.getLogFile() );
    
    if( lf.exists() && ! lf.canWrite() )
    {
     System.err.println("Log file '"+config.getLogFile()+"' is not writable");
     System.exit(1);
    }

    try
    {
     out = new PrintStream(lf);
    }
    catch(FileNotFoundException e)
    {
     System.err.println("Can't open log file '"+config.getLogFile()+"'");
     System.exit(1);
    }
    
   }
   
   printLog(topLn, out, config.getPrintInfoNodes() );
   
  }
  
  if( topLn.getLevel() != Level.SUCCESS )
   System.exit(1);
  
  PrintStream out =null;
  
  try
  {
   out = "-".equals( config.getFiles().get(1) )? System.out : new PrintStream( outfile );
  }
  catch(FileNotFoundException e)
  {
   System.err.println("Can't open output file '"+outfile.getAbsolutePath()+"': "+e.getMessage());
   System.exit(1);
  }
  
  
  SubmissionPageMLFormatter pageMLfmt = new SubmissionPageMLFormatter();
  
  out.println("<Submissions>");
  
  try
  {
   for( PreparedSubmission ps : submissions )
    pageMLfmt.format(ps.getSubmission(), out);
  }
  catch(IOException e)
  {
   System.err.println("Output file write error '"+outfile.getAbsolutePath()+"': "+e.getMessage());
   System.exit(1);
  }
  
  out.println("</Submissions>");

  out.close();
 }
 
 private static void printLog(LogNode topLn, PrintStream out, boolean printInfoNodes)
 {
  if( ! printInfoNodes && topLn.getLevel() != Level.WARN && topLn.getLevel() != Level.ERROR  )
   return;

  printLog(topLn, out, printInfoNodes, new ArrayList<Character>() );
 }
 
 private static void printLog(LogNode ln, PrintStream out, boolean printInfoNodes, List<Character> indent)
 {
  for( Character ch : indent )
   System.out.print(ch);
  

  out.println(ln.getLevel().name()+": "+ln.getMessage());

  
  if( ln.getSubNodes() != null && ln.getSubNodes().size() > 0 )
  {
   if( indent.size() > 0 )
   {
    if( indent.get(indent.size()-1) == EL )
     indent.set(indent.size()-1,' ');
    else if( indent.get(indent.size()-1) == TEE )
     indent.set(indent.size()-1,EE);
   }
   
   int n = ln.getSubNodes().size();
   
   for( int i=0; i < n; i++ )
   {
    LogNode snd = ln.getSubNodes().get(i);
    
    if( ! printInfoNodes && snd.getLevel() != Level.WARN && snd.getLevel() != Level.ERROR )
     continue;
    
    if( i == n-1 )
     indent.add(EL);
    else
     indent.add(TEE);
    
    printLog(snd, out, printInfoNodes, indent);
    
    indent.remove(indent.size()-1);
   }
  
  }
  
 }

 static void usage()
 {
  
 }
}
