package org.shield.browser;
import android.os.Build;
import android.view.Window;
public final class BackHandlerTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  Build.VERSION.SDK_INT=32;Window legacy=new Window();BackHandler old=new BackHandler(legacy,()->{throw new AssertionError("Legacy dispatch must stay in Activity");});old.enable(true);old.enable(false);old.close();check(legacy.reads==0,"API32 must not access API33 dispatcher");
  for(int sdk:new int[]{33,36}){
   Build.VERSION.SDK_INT=sdk;Window browser=new Window();int[] calls={0};BackHandler handler=new BackHandler(browser,()->calls[0]++);
   handler.enable(false);check(browser.reads==0,"Do not intercept root Back by default");
   handler.enable(true);handler.enable(true);check(browser.dispatcher.added==1,"Idempotent registration");browser.dispatcher.callback.onBackInvoked();check(calls[0]==1,"Back reaches the actual browser action");
   Window settings=new Window();BackHandler nested=new BackHandler(settings,()->calls[0]+=10);nested.enable(true);settings.dispatcher.callback.onBackInvoked();check(calls[0]==11,"Settings dispatch");nested.close();check(settings.dispatcher.callback==null&&browser.dispatcher.callback!=null,"Dialogs clean up only their own callback");
   handler.enable(false);handler.enable(false);check(browser.dispatcher.removed==1&&browser.dispatcher.callback==null,"Root returns Back to Android");
   handler.enable(true);handler.close();check(browser.dispatcher.added==2&&browser.dispatcher.removed==2,"Lifecycle cleanup");
  }
  System.out.println("Back handler JVM checks passed: legacy gating, Android33/36 dispatch, root fallback, dialog isolation and cleanup");
 }
}
