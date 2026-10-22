package org.shield.browser;
import java.io.*;import java.util.zip.*;
public class DownloadPolicyTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static File zip(String name)throws Exception{File f=File.createTempFile("download-policy-",".tmp");try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(f))){out.putNextEntry(new ZipEntry(name));out.write(new byte[]{1,2});out.closeEntry();}return f;}
 public static void main(String[] args)throws Exception{
  for(String s:new String[]{"test.APK","https://example.org/test%2Eapk?token=x","attachment; filename=\"app.apk\"","application/vnd.android.package-archive","app.xapk","app.apks"})check(DownloadPolicy.blocked(s));
  check(!DownloadPolicy.blocked("photo.jpg","https://example.org/file.pdf","application/pdf"));
  for(String name:new String[]{"AndroidManifest.xml","files/app.apk"}){File f=zip(name);try{DownloadPolicy.verify(f);throw new AssertionError("Renamed APK accepted");}catch(DownloadPolicy.BlockedPackage expected){}finally{f.delete();}}
  File ordinary=zip("document.txt");DownloadPolicy.verify(ordinary);ordinary.delete();
  File pdf=File.createTempFile("download-policy-",".pdf");try(FileOutputStream o=new FileOutputStream(pdf)){o.write("%PDF-test".getBytes("UTF-8"));}DownloadPolicy.verify(pdf);pdf.delete();
  File bad=File.createTempFile("download-policy-",".bin");try(FileOutputStream o=new FileOutputStream(bad)){o.write(new byte[]{80,75,0,1});}try{DownloadPolicy.verify(bad);throw new AssertionError("Uninspectable ZIP accepted");}catch(IOException expected){}finally{bad.delete();}
  System.out.println("Download policy passed: names, encoded URLs, MIME, disguised APKs, bundled APKs, ordinary ZIP/PDF, corrupt ZIP fail closed");
 }
}
