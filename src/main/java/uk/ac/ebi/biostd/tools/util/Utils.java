package uk.ac.ebi.biostd.tools.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;

public class Utils
{
 static final Character AI = new Character('\u2502');
 static final Character EL = new Character('\u2514');
 static final Character TEE = new Character('\u251C');
 
 public static void printLog(LogNode topLn, PrintStream out, Level minLevel )
 {
 
  if( topLn.getLevel().getPriority() < minLevel.getPriority() )
   return;

  printLog(topLn, out, minLevel, new ArrayList<Character>() );
 }
 
 private static void printLog(LogNode ln, PrintStream out, Level minLevel, List<Character> indent)
 {
  for( Character ch : indent )
   out.print(ch);
  

  out.println(ln.getLevel().name()+": "+ln.getMessage());

  int snSz = 0;
  
  if( ln.getSubNodes() != null )
  {
   for( LogNode sln : ln.getSubNodes() )
    if( sln.getLevel().getPriority() >= minLevel.getPriority() )
     snSz++;
  }
  
  if( snSz > 0 )
  {
   if( indent.size() > 0 )
   {
    if( indent.get(indent.size()-1) == EL )
     indent.set(indent.size()-1,' ');
    else if( indent.get(indent.size()-1) == TEE )
     indent.set(indent.size()-1,AI);
   }
   
   int n = ln.getSubNodes().size();
   int elgn=0;
   
   for( int i=0; i < n; i++ )
   {
    LogNode snd = ln.getSubNodes().get(i);
    
    if( snd.getLevel().getPriority() < minLevel.getPriority() )
     continue;
    
    elgn++;
    
    if( elgn == snSz )
     indent.add(EL);
    else
     indent.add(TEE);
    
    printLog(snd, out, minLevel, indent);
    
    indent.remove(indent.size()-1);
   }
  
  }
 }
}