package uk.ac.ebi.biostd.tools.odread;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;

public class Main
{

 public static void main(String[] args) throws FileNotFoundException, Exception
 {
  SpreadsheetDocument doc = SpreadsheetDocument.loadDocument(new FileInputStream("c:\\Dev\\data\\Test.ods"));

  Table tbl = doc.getSheetByIndex(0);
  
  int rn = tbl.getRowCount();
  
  for( int i=0; i < rn; i++ )
  {
   Row r = tbl.getRowByIndex(i);
   
   if( r == null )
   {
    System.out.println("");
    continue;
   }
   
   int lcn = r.getCellCount();
   
   for( int j=0; j <= lcn; j++ )
   {
    Cell c = r.getCellByIndex(j);
    
    if( c != null )
    {
//     System.out.print(c.getValueType());
  
     if( "date".equals(c.getValueType()))
      System.out.print( c.getDateValue().getTime() );
     else
      System.out.print(c.getStringValue());
    }
    
    System.out.print("|");
   }
  
   System.out.println("");

  }
  
  doc.close();
  
 }

}
