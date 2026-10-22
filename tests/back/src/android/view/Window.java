package android.view;
import android.window.*;
public final class Window {
 public int reads;
 public final FakeDispatcher dispatcher=new FakeDispatcher();
 public OnBackInvokedDispatcher getOnBackInvokedDispatcher(){reads++;return dispatcher;}
 public static final class FakeDispatcher implements OnBackInvokedDispatcher {
  public int added,removed;public OnBackInvokedCallback callback;
  public void registerOnBackInvokedCallback(int priority,OnBackInvokedCallback value){if(callback!=null)throw new AssertionError("Duplicate callback");added++;callback=value;}
  public void unregisterOnBackInvokedCallback(OnBackInvokedCallback value){if(callback!=value)throw new AssertionError("Wrong callback removed");removed++;callback=null;}
 }
}
