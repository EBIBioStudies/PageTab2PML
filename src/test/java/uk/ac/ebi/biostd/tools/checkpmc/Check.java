package uk.ac.ebi.biostd.tools.checkpmc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.poi.util.IOUtils;

import uk.ac.ebi.biostd.in.PMDoc;
import uk.ac.ebi.biostd.in.pagetab.FileOccurrence;
import uk.ac.ebi.biostd.in.pagetab.SubmissionInfo;
import uk.ac.ebi.biostd.tools.convert.CVSTVSParse;
import uk.ac.ebi.biostd.treelog.ErrorCounter;
import uk.ac.ebi.biostd.treelog.ErrorCounterImpl;
import uk.ac.ebi.biostd.treelog.LogNode.Level;
import uk.ac.ebi.biostd.treelog.SimpleLogNode;

public class Check
{

 public static void main(String[] args) throws MalformedURLException
 {
  String srcFile = "c:/dev/data/BioStudyMetadataSampleDraft7.txt";
  String outDirPath =  "c:/dev/data/pmc";

  File outDir = new File(outDirPath);
  
  outDir.mkdir();
  
  ErrorCounter ec = new ErrorCounterImpl();
  SimpleLogNode topLn = new SimpleLogNode(Level.SUCCESS, "Parsing file: '" + srcFile + "'", ec);

  
  PMDoc doc = CVSTVSParse.parse(new File(srcFile), "utf-8", '\t', topLn);
  
  SimpleLogNode.setLevels(topLn);
  
  if( topLn.getLevel() == Level.ERROR )
   System.err.println("Parse file failed");
  
  int nSub = doc.getSubmissions().size();
  int cSub=0;
  
  for( SubmissionInfo si : doc.getSubmissions() )
  {
   cSub++;
   
   String accNo = si.getRootSectionOccurance().getSection().getAccNo();
   
   System.out.println( "Processing: "+accNo+" ("+cSub+"/"+nSub+")" );
   
   int pos = accNo.indexOf("PMC");
   
   if( pos >=0 )
    accNo = accNo.substring(pos);
   
   File stdDir = new File( outDir, accNo);
   
   stdDir.mkdir();
   
   int nFil = si.getFileOccurrences().size();
   int cFil=0;

   
   for( FileOccurrence fo :  si.getFileOccurrences() )
   {
    cFil++;
    
    String fn = fo.getFileRef().getName();
    
    File outFile = new File(stdDir,fn);
    
    if( outFile.exists() )
    {
     System.out.println("File "+accNo+": "+fn+" OK ("+cFil+"/"+nFil+")");
     continue;
    }
    
    System.out.println("Downloading "+accNo+": "+fn+" ("+cFil+"/"+nFil+")");
    
    URL url = new URL("http://europepmc.org/articles/"+accNo+"/bin/"+fn);
    
    try
    {
     HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    
     byte[] data = IOUtils.toByteArray(conn.getInputStream());
     
     System.out.println("Downloaded: "+data.length+" bytes");
     
     FileOutputStream fos = new FileOutputStream( outFile );
     
     fos.write(data);
     
     fos.close();
     
    }
    catch(IOException e)
    {
     System.err.println("Can't open connection: "+url+" "+e.getMessage());
     continue;
    }
    
   }
   
  }
  
 }

}
