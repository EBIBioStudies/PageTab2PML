package uk.ac.ebi.biostd.tools.convert;

import java.util.List;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface Config
{
  @Unparsed
   public List<String> getFiles();

  @Option( shortName="i", defaultValue="auto")
  String getInputFormat();

  @Option( shortName="o", defaultValue="xml")
  String getOutputFormat();
  
  @Option(shortName="l",defaultValue="-")
  public String getLogFile();

  @Option(shortName="d")
  public boolean getPrintInfoNodes();

  @Option(shortName="g")
  public boolean getGenAcc();
  
  @Option(shortName="c", defaultValue="utf-8")
  public String getCharset();
  
  @Option(helpRequest = true,shortName="h")
  boolean getHelp();
  
}
