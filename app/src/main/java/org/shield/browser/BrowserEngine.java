package org.shield.browser;

import android.content.Context;
import org.json.JSONObject;
import org.mozilla.geckoview.*;

/** Process-wide profile and extension; shared by browser windows and download service. */
public final class BrowserEngine {
    private static GeckoRuntime runtime;
    public static WebExtension extension;
    public static JSONObject stats = new JSONObject();
    public interface Ready { void ready(GeckoRuntime runtime, WebExtension extension); void failed(Throwable error); }
    public static synchronized GeckoRuntime runtime(Context context) {
        if(runtime==null)runtime=GeckoRuntime.create(context.getApplicationContext());
        return runtime;
    }
    public static void ensure(Context context, Ready callback) {
        android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()){main.post(()->ensure(context,callback));return;}
        Context app=context.getApplicationContext();GeckoRuntime r=runtime(app);
        r.getWebExtensionController().ensureBuiltIn("resource://android/assets/protection/","shield@browser.local").then(ext->{GeckoResult<WebExtension> allowed=new GeckoResult<>();main.post(()->r.getWebExtensionController().setAllowedInPrivateBrowsing(ext,true).accept(allowed::complete,allowed::completeExceptionally));return allowed;}).accept(ext->main.post(()->{
            extension=ext;
            ext.setMessageDelegate(new WebExtension.MessageDelegate(){
                @Override public void onConnect(WebExtension.Port port){
                    port.setDelegate(new WebExtension.PortDelegate(){
                        @Override public void onPortMessage(Object value,WebExtension.Port from){
                            if(value instanceof JSONObject){stats=(JSONObject)value;app.getSharedPreferences("browser",0).edit().putString("protection_stats",stats.toString()).apply();}
                        }
                    });
                }
            },"kosherStats");
            callback.ready(r,ext);
        }),error->main.post(()->callback.failed(error)));
    }
    private BrowserEngine(){}
}
