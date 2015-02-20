package uk.ac.ebi.biostd.tools.submit;

import java.util.List;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface Config
{
  @Unparsed
   public List<String> getFiles();

  @Option( shortName="i", defaultValue="tab")
  String getInputFormat();
  
  @Option( shortName="u")
  String getUser();

  @Option( shortName="p")
  String getPassword();
  
  @Option(shortName="s")
  public String getServer();

  @Option(shortName="l",defaultValue="-")
  public String getLogFile();
  
  @Option(shortName="d")
  public boolean getPrintInfoNodes();
  
  @Option(helpRequest = true,shortName="h")
  boolean getHelp();
  
}
