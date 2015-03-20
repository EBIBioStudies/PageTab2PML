package uk.ac.ebi.biostd.tools.util;

public enum Format
{
 xlsx  ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
 xls   ("application/vnd.ms-excel"),
 json  ("application/json"),
 ods   ("application/vnd.oasis.opendocument.spreadsheet"),
 csv   ("text/csv"),
 tsv   ("text/tsv"),
 csvtsv("text/csvtsv"),
 xml   ("text/xml");
 

 
 private String contentType;

 Format(String ctype)
 {
  contentType = ctype;
 }
 
 public String getContentType()
 {
  return contentType;
 }
}
