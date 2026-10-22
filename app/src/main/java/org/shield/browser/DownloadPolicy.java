package org.shield.browser;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.zip.*;

/** Blocks Android installation packages before exporting a completed download. */
final class DownloadPolicy {
 static boolean blocked(String... values){for(String value:values){if(value==null)continue;String v=value.toLowerCase(Locale.ROOT);for(int i=0;i<3;i++){if(v.contains("application/vnd.android.package-archive")||v.matches("(?s).*\\.(apk|apks|apkm|xapk)(?:$|[\\s\\\"';?#/&=]).*"))return true;try{String decoded=URLDecoder.decode(v,"UTF-8");if(decoded.equals(v))break;v=decoded;}catch(Exception e){break;}}}return false;}
 static void verify(File file)throws IOException {
  boolean zipMagic=false;try(InputStream in=new FileInputStream(file)){zipMagic=in.read()==0x50&&in.read()==0x4b;}
  try(ZipFile zip=new ZipFile(file)){
   Enumeration<? extends ZipEntry> entries=zip.entries();int count=0;
   while(entries.hasMoreElements()){if(++count>100000)throw new IOException("Archive cannot be safely checked");String name=entries.nextElement().getName();if(name.equalsIgnoreCase("AndroidManifest.xml")||blocked(name))throw new BlockedPackage();}
  }catch(BlockedPackage e){throw e;}catch(ZipException e){if(zipMagic)throw new IOException("Archive cannot be safely checked",e);}
 }
 static final class BlockedPackage extends IOException {BlockedPackage(){super("APK downloads are blocked");}}
}
