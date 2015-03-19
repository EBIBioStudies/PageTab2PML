package uk.ac.ebi.biostd.tools.xlread;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class Main
{

 public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException
 {
  Workbook wb = WorkbookFactory.create(new FileInputStream("c:\\Dev\\data\\Test.xls"));

  Sheet sheet = wb.getSheetAt(0);

//  for(Row row : sheet)
//  {
//   for(Cell cell : row)
//   {
//    System.out.print(cell.getStringCellValue()+" |");
//   }
//   
//   System.out.println("");
//  }
  
  int lrn = sheet.getLastRowNum();
  
  for( int i=0; i <= lrn; i++ )
  {
   Row r = sheet.getRow(i);
   
   if( r == null )
   {
    System.out.println("");
    continue;
   }
   
   int lcn = r.getLastCellNum();
   
   for( int j=0; j <= lcn; j++ )
   {
    Cell c = r.getCell(j, Row.RETURN_BLANK_AS_NULL);
    
    if( c != null )
    {
//     System.out.print("CT"+c.getCellType());
     
     if( c.getCellType() == Cell.CELL_TYPE_BOOLEAN )
      System.out.print(c.getBooleanCellValue());
     else if( c.getCellType() == Cell.CELL_TYPE_NUMERIC )
     {
      if(DateUtil.isCellDateFormatted(c))
      {
       System.out.println(c.getDateCellValue());
      }
      else
      {
       System.out.println(c.getNumericCellValue());
      }
     }
     else if( c.getCellType() == Cell.CELL_TYPE_FORMULA )
     {
      try
      {
       System.out.print(c.getNumericCellValue());
      }
      catch( Throwable t )
      {
       try
       {
        System.out.print(c.getStringCellValue());
       }
       catch( Throwable t1 )
       {
        try
        {
         System.out.print(c.getBooleanCellValue());
        }
        catch( Throwable t3 )
        {
         System.out.print(c.getCellFormula());
        }
       }
      }
     }
     else
      System.out.print(c.getStringCellValue());
    }
    
    System.out.print("|");
   }
  
   System.out.println("");

  }
  
 }

}
