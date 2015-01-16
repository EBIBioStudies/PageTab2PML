package uk.ac.ebi.biostd.pt2pml;

import java.util.List;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface Config
{
  @Unparsed
   public List<String> getFiles();

  @Option(shortName="l",defaultValue="-")
  public String getLogFile();

  @Option(shortName="i")
  public boolean getPrintInfoNodes();
  
}
