package uk.ac.ebi.biostd.tools.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ebi.biostd.treelog.LogNode;
import uk.ac.ebi.biostd.treelog.LogNode.Level;

public class Utils
{
 static final Character EE = new Character('\u2502');
 static final Character EL = new Character('\u2514');
 static final Character TEE = new Character('\u251C');
 
 public static void printLog(LogNode topLn, PrintStream out, boolean printInfoNodes)
 {
  if( ! printInfoNodes && topLn.getLevel() != Level.WARN && topLn.getLevel() != Level.ERROR  )
   return;

  printLog(topLn, out, printInfoNodes, new ArrayList<Character>() );
 }
 
 private static void printLog(LogNode ln, PrintStream out, boolean printInfoNodes, List<Character> indent)
 {
  for( Character ch : indent )
   out.print(ch);
  

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
}