package uk.ac.ebi.biostd.tools.convert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.ParserConfig;
import uk.ac.ebi.biostd.in.ParserException;
import uk.ac.ebi.biostd.in.json.JSONReader;
import uk.ac.ebi.biostd.in.pagetab.PageTabSyntaxParser;
import uk.ac.ebi.biostd.in.pagetab.ReferenceOccurrence;
import uk.ac.ebi.biostd.in.pagetab.SectionOccurrence;
import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.out.Formatter;
import uk.ac.ebi.biostd.out.json.JSONFormatter;
import uk.ac.ebi.biostd.out.pageml.PageMLFormatter;
import uk.ac.ebi.biostd.tools.util.Format;
import uk.ac.ebi.biostd.tools.util.Utils;
import uk.ac.ebi.biostd.treelog.ErrorCounter;
import uk.ac.ebi.biostd.treelog.ErrorCounterImpl;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;
import uk.ac.ebi.biostd.util.FileUtil;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import com.lexicalscope.jewel.cli.InvalidOptionSpecificationException;

public class Main
{

 
 public static void main( String[] args )
 {
  Config config = null;
    
  try
  {
   config = CliFactory.parseArguments(Config.class, args);
  }
  catch (HelpRequestedException e)
  {
   usage();
   System.exit(1);
  }
  catch(InvalidOptionSpecificationException | ArgumentValidationException e)
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
  
  Format fmt = null;
  
  if( config.getOutputFormat().equalsIgnoreCase("xml") )
   fmt=Format.XML;
  else if( config.getOutputFormat().equalsIgnoreCase("json") )
   fmt=Format.JSON;
  else
  {
   System.err.println("Invalid output formatl '"+config.getOutputFormat()+"'");
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
   System.err.println("Input file read ERROR: "+e.getMessage());
   System.exit(1);
  }

  PMDoc doc = null;
  ErrorCounter ec = new ErrorCounterImpl();
  SimpleLogNode topLn = new SimpleLogNode(Level.SUCCESS, "Parsing file: '" + infile.getAbsolutePath() + "'", ec);

  if( config.getInputFormat().equalsIgnoreCase("tab") )
  {
   ParserConfig pc = new ParserConfig();

   pc.setMultipleSubmissions(true);

   PageTabSyntaxParser parser = new PageTabSyntaxParser(new AdHocTagResolver(), pc);

   try
   {
    doc = parser.parse(text, topLn);
   }
   catch(ParserException e)
   {
    System.err.println("Can't parse Page-Tab file: " + e.getMessage());
    System.exit(1);
   }
  }
  else if( config.getInputFormat().equalsIgnoreCase("json") )
  {
   ParserConfig pc = new ParserConfig();

   pc.setMultipleSubmissions(true);

   JSONReader jsnReader = new JSONReader(new AdHocTagResolver(), pc);
   
   doc = jsnReader.parse(text, topLn);
  }
  
  SimpleLogNode.setLevels(topLn);
  
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
     out = new PrintStream(lf,"UTF-8");
    }
    catch(FileNotFoundException e)
    {
     System.err.println("Can't open log file '"+config.getLogFile()+"'");
     System.exit(1);
    }
    catch(UnsupportedEncodingException e)
    {
     System.err.println("UTF-8 encoding is not supported");
     System.exit(1);
    }
    
   }
   

   
   Utils.printLog(topLn, out, config.getPrintInfoNodes()? Level.DEBUG : Level.WARN );
   
  }
  
  if( topLn.getLevel() == Level.ERROR )
   System.exit(1);
  
  int gen=1;
  
  for( SubmissionInfo ps : doc.getSubmissions() )
  {
   if( ps.getAccNoPrefix() != null  || ps.getAccNoSuffix() != null )
    ps.getSubmission().setAccNo( (ps.getAccNoPrefix() != null?ps.getAccNoPrefix():"")+(gen++)+(ps.getAccNoSuffix() != null?ps.getAccNoSuffix():"") );
   else if( ps.getSubmission().getAccNo() == null )
    ps.getSubmission().setAccNo("SBM"+(gen++));
    
   
   if( ps.getGlobalSections() == null )
    continue;
   
   for( SectionOccurrence sr :  ps.getGlobalSections() )
   {
    if( sr.getPrefix() != null  || sr.getSuffix() != null )
     sr.getSection().setAccNo( (sr.getPrefix() != null?sr.getPrefix():"")+(gen++)+(sr.getSuffix() != null?sr.getSuffix():"") );
   }
   
   if( ps.getReferenceOccurrences() == null )
    continue;
   
   for( ReferenceOccurrence ro : ps.getReferenceOccurrences() )
    ro.getRef().setValue( ro.getSection().getAccNo() );
   
  }
  
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
  
  Formatter outfmt = null;
  
  if( fmt == Format.XML )
   outfmt = new PageMLFormatter();
  else if( fmt == Format.JSON )
   outfmt = new JSONFormatter();
  
  
  try
  {
   outfmt.header(doc.getHeaders(), out);
   
   for( SubmissionInfo ps : doc.getSubmissions() )
    outfmt.format(ps.getSubmission(),out);
   
   outfmt.footer(out);
  }
  catch(IOException e)
  {
   System.err.println("Output file write error '"+outfile.getAbsolutePath()+"': "+e.getMessage());
   System.exit(1);
  }
  
  out.close();
 }
 
 static void usage()
 {
  System.err.println("Usage: java -jar PT2PML [-h] [-i in fmt] [-o out fmt] [-d] [-l logfile] <input file> <output file>");
  System.err.println("-h or --help print this help message");
  System.err.println("-i or --inputFormat input file format. Can be json or tab");
  System.err.println("-o or --outputFormat output file format. Can be json or xml");
  System.err.println("-d or --printInfoNodes print info messages along with errors and warnings");
  System.err.println("-l or --logFile defines log file. By default stdout");
  System.err.println("<input file> PagaTab input file. Supported UCS-2 (UTF-16), UTF-8 CSV or TSV or MS Excel XML files");
  System.err.println("<output file> XML output file. '-' means output to stdout");
 }
}
