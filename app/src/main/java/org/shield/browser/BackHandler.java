package org.shield.browser;

import android.os.Build;
import android.view.Window;

/** Uses Android 13+ back dispatch while retaining Android 8–12 navigation. */
final class BackHandler implements AutoCloseable {
    private final Window window;
    private final Runnable action;
    private Object registration;
    BackHandler(Window window,Runnable action){this.window=window;this.action=action;}
    void enable(boolean enabled){
        if(Build.VERSION.SDK_INT<33)return;
        if(enabled&&registration==null)registration=Api33.register(window,action);
        else if(!enabled&&registration!=null){Api33.unregister(registration);registration=null;}
    }
    @Override public void close(){enable(false);}
    @android.annotation.TargetApi(33)
    private static final class Api33 {
        static final class Registration {
            final android.window.OnBackInvokedDispatcher dispatcher;
            final android.window.OnBackInvokedCallback callback;
            Registration(Window window,Runnable action){dispatcher=window.getOnBackInvokedDispatcher();callback=action::run;}
        }
        static Object register(Window window,Runnable action){
            Registration r=new Registration(window,action);
            r.dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,r.callback);return r;
        }
        static void unregister(Object token){Registration r=(Registration)token;r.dispatcher.unregisterOnBackInvokedCallback(r.callback);}
    }
}
