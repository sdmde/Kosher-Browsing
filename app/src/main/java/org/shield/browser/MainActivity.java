package org.shield.browser;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import org.mozilla.geckoview.*;
import java.util.*;

public class MainActivity extends Activity {
 static GeckoRuntime runtime;
 final ArrayList<Tab> tabs = new ArrayList<>();
 int selected=0; boolean ready=false, dark=false, destroying=false;
 LinearLayout root,bar,tabStrip; HorizontalScrollView tabScroll; GeckoView web; EditText address; IconButton count,backButton,forwardButton,bookmarkButton; ProgressBar progress;
 android.content.SharedPreferences prefs;
 DownloadsController downloads; BrowserFeatures features; BrowserUi chrome; BackHandler backHandler;
 class Tab { GeckoSession session; WebExtension.Port port; android.graphics.Bitmap preview; String state; String url,pendingUrl,title="New tab"; String pageLanguage;boolean translating;boolean desktop,home,back,forward,popupPending; Tab(String u){url=u;pendingUrl=u!=null&&u.startsWith("http")?u:null;} }
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
 @Override protected void attachBaseContext(Context base){super.attachBaseContext(BrowserLanguages.wrap(base));}
 @Override public void onCreate(Bundle state){
  super.onCreate(state); prefs=getSharedPreferences("browser",0); dark=prefs.getBoolean("dark",false);
  if(!prefs.getBoolean("chrome_layout_v1",false))prefs.edit().putString("toolbar_position","top").putBoolean("show_navigation",false).putBoolean("chrome_layout_v1",true).apply();
  downloads=new DownloadsController(this);
  features=new BrowserFeatures(this); runtime=BrowserEngine.runtime(this);
  layout();
  BrowserEngine.ensure(this,new BrowserEngine.Ready(){public void ready(GeckoRuntime r,WebExtension e){if(isFinishing()||isDestroyed())return;ready=true;restore();}public void failed(Throwable error){if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(MainActivity.this).setTitle("Protections could not start").setMessage("Browsing is locked. Restart the app to retry.").setPositiveButton("Close",(d,w)->finish()).setCancelable(false).show();}});
 }
 Button button(String label,Runnable action){Button b=new Button(this); b.setText(tr(label));b.setTextColor(ink());b.setContentDescription(label);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(6),0,dp(6),0);b.setOnClickListener(v->action.run());return b;}
 int ink(){return dark?0xffe8eaed:0xff202124;}
 int buttonFill(){return android.graphics.Color.TRANSPARENT;}
 int buttonInk(){return ink();}
 int disabledInk(){return dark?0xff9aa0a6:0xff80868b;}
 GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 IconButton icon(int symbol,String label,Runnable action){IconButton b=new IconButton(this,symbol,buttonFill(),buttonInk(),android.graphics.Color.TRANSPARENT);b.setContentDescription(tr(label));b.setTooltipText(tr(label));b.setOnClickListener(v->action.run());return b;}
 int accent(){return dark?0xff8ab4f8:0xff1a73e8;}
 void resolveTheme(){String mode=prefs.getString("theme_mode",prefs.contains("dark")?(prefs.getBoolean("dark",false)?"dark":"light"):"system");dark="dark".equals(mode)||("system".equals(mode)&&(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES);}
 void applyColors(){resolveTheme();setTheme(dark?R.style.KosherDark:R.style.KosherLight);getWindow().setStatusBarColor(dark?0xff202124:0xffffffff);getWindow().setNavigationBarColor(dark?0xff202124:0xffffffff);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);if(runtime!=null)runtime.getSettings().setPreferredColorScheme(dark?GeckoRuntimeSettings.COLOR_SCHEME_DARK:GeckoRuntimeSettings.COLOR_SCHEME_LIGHT);}
 String tr(String value){return BrowserLanguages.text(this,value);}
 void layout(){if(chrome==null)chrome=new BrowserUi(this);chrome.layout();BrowserSettings.applyWebPreferences(this);}
 void rebuildChrome(){if(web!=null&&web.getSession()!=null)web.releaseSession();layout();if(!tabs.isEmpty())switchTo(selected);if(features.fullscreen)features.applyFullscreen();}

 void updateNavigation(){Tab t=current();boolean back=t!=null&&t.back,forward=t!=null&&t.forward;backButton.setEnabled(back);backButton.setAlpha(1f);backButton.setIconColor(back?buttonInk():disabledInk());forwardButton.setEnabled(forward);forwardButton.setAlpha(1f);forwardButton.setIconColor(forward?buttonInk():disabledInk());updateBackHandling();}
 void updateBackHandling(){if(backHandler==null)backHandler=new BackHandler(getWindow(),this::handleBrowserBack);Tab t=current();backHandler.enable(!destroying&&!isFinishing()&&!isDestroyed()&&(features.fullscreen||(t!=null&&t.back)));}
 void renderTabs(){tabStrip.removeAllViews();for(int i=0;i<tabs.size();i++){final Tab t=tabs.get(i);LinearLayout chip=new LinearLayout(this);chip.setGravity(Gravity.CENTER_VERTICAL);chip.setBackground(rounded(i==selected?(dark?0xff394457:0xff8ab4f8):Color.TRANSPARENT,12));TextView title=new TextView(this);title.setText(t.title);title.setTextColor(ink());title.setTextSize(13);title.setSingleLine();title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(dp(12),0,dp(4),0);title.setTypeface(null,i==selected?Typeface.BOLD:Typeface.NORMAL);title.setContentDescription((i==selected?"Selected tab: ":"Tab: ")+t.title);title.setOnClickListener(v->{int index=tabs.indexOf(t);if(index>=0)switchTo(index);});chip.addView(title,new LinearLayout.LayoutParams(dp(132),dp(40)));chip.addView(icon(R.drawable.ic_close,"Close "+t.title,()->closeTab(t)),new LinearLayout.LayoutParams(dp(32),dp(40)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(164),dp(40));p.setMarginEnd(dp(4));tabStrip.addView(chip,p);}count.setCount(tabs.size());count.setContentDescription(tr("Tabs")+": "+tabs.size());}
 void closeTab(Tab t){features.remember(t);int index=tabs.indexOf(t);if(index<0)return;Tab active=current();if(web.getSession()!=null)web.releaseSession();tabs.remove(index);selected=tabs.isEmpty()?0:(tabs.contains(active)?tabs.indexOf(active):Math.min(index,tabs.size()-1));if(!DownloadService.isPinned(t.session))t.session.close();else DownloadService.detach(t.session);if(tabs.isEmpty())add("kosher:home",false);switchTo(selected);}
 Tab current(){return tabs.isEmpty()?null:tabs.get(selected);}
 void restore(){JSONArray records=features.array("session_tabs");if(records.length()>0){for(int i=0;i<Math.min(100,records.length());i++)features.restoreRecord(records.optJSONObject(i),false);}else try{JSONArray old=new JSONArray(prefs.getString("tabs","[]"));for(int i=0;i<Math.min(100,old.length());i++)add(old.getString(i),false);}catch(Exception ignored){}if(tabs.isEmpty())add("kosher:home",false);switchTo(Math.max(0,Math.min(prefs.getInt("selected",0),tabs.size()-1)));if(getIntent().getData()!=null)navigate(getIntent().getData().toString());if(getIntent().getBooleanExtra("show_downloads",false)){getIntent().removeExtra("show_downloads");downloads.showTransfers();}}

 Tab createTab(String url){if(!ready)return null;Tab t=new Tab(url==null?"about:blank":url);t.session=new GeckoSession();
  t.session.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
   @Override public void onLocationChange(GeckoSession s,String uri,List<GeckoSession.PermissionDelegate.ContentPermission> permissions,Boolean gesture){if(t.home||("about:blank".equals(uri)&&t.pendingUrl!=null))return;t.pendingUrl=null;t.url=uri;if(current()==t){address.setText(uri);updateBookmark();}save();}
   @Override public void onCanGoBack(GeckoSession s,boolean value){t.back=value;if(current()==t)updateNavigation();}
   @Override public void onCanGoForward(GeckoSession s,boolean value){t.forward=value;if(current()==t)updateNavigation();}
   @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession s,LoadRequest request){Uri u=Uri.parse(request.uri);boolean blank="about:blank".equals(request.uri)||request.uri.startsWith("about:blank#");return GeckoResult.fromValue((blank||"https".equals(u.getScheme())||"http".equals(u.getScheme())||("moz-extension".equals(u.getScheme())&&"/blocked.html".equals(u.getPath())))?AllowOrDeny.ALLOW:AllowOrDeny.DENY);}
   @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s,String uri){if(tabs.size()>=100)return GeckoResult.fromValue(null);Tab child=createTab("about:blank");if(child==null)return GeckoResult.fromValue(null);child.popupPending=true;tabs.add(child);renderTabs();root.post(()->activatePopup(child));return GeckoResult.fromValue(child.session);}
   @Override public GeckoResult<String> onLoadError(GeckoSession s,String uri,WebRequestError error){return GeckoResult.fromValue("data:text/html;charset=utf-8,"+Uri.encode("<meta name='viewport' content='width=device-width'><body style='font:18px sans-serif;background:#202124;color:#e8eaed;padding:32px'><h1>Page unavailable</h1><p>The site could not be loaded. Check your connection or try again. Some restricted requests may also be stopped by the browser.</p>"));}
  });
  t.session.setContentDelegate(new GeckoSession.ContentDelegate(){@Override public void onTitleChange(GeckoSession s,String title){if(t.home)return;t.title=title==null||title.isEmpty()?"New tab":title;renderTabs();} @Override public void onExternalResponse(GeckoSession session,WebResponse response){downloads.onExternalResponse(session,response);}@Override public void onContextMenu(GeckoSession s,int x,int y,ContextElement e){features.link(e);}@Override public void onFullScreen(GeckoSession s,boolean full){features.fullscreen=full;features.applyFullscreen();}@Override public void onCrash(GeckoSession s){features.crash(t);}@Override public void onKill(GeckoSession s){features.crash(t);}@Override public void onCloseRequest(GeckoSession session){if(tabs.contains(t))closeTab(t);}});
  t.session.setProgressDelegate(new GeckoSession.ProgressDelegate(){@Override public void onSessionStateChange(GeckoSession s,GeckoSession.SessionState state){if(!t.home){String value=state.toString();t.state=value.length()<=262144?value:null;save();}}@Override public void onPageStart(GeckoSession session,String url){t.pageLanguage=null;t.translating=false;activatePopup(t);}@Override public void onProgressChange(GeckoSession s,int value){if(current()==t){progress.setProgress(value);progress.setVisibility(features.fullscreen?View.GONE:(value==100?View.INVISIBLE:View.VISIBLE));}} @Override public void onPageStop(GeckoSession s,boolean success){if(success&&!t.home){history(t);if(current()==t)features.capture(t);}}});
  features.bind(t);BrowserLanguages.bind(this,t);return t;
 }
 void add(String url,boolean select){add(url,select,false);}
 void add(String url,boolean select,boolean desktop){if(tabs.size()>=100){Toast.makeText(this,"Close a tab before opening more (100 maximum).",Toast.LENGTH_LONG).show();return;}Tab t=createTab(url);if(t==null)return;tabs.add(t);t.desktop=desktop;t.session.getSettings().setUserAgentMode(desktop?GeckoSessionSettings.USER_AGENT_MODE_DESKTOP:GeckoSessionSettings.USER_AGENT_MODE_MOBILE);t.session.getSettings().setViewportMode(desktop?GeckoSessionSettings.VIEWPORT_MODE_DESKTOP:GeckoSessionSettings.VIEWPORT_MODE_MOBILE);t.home="kosher:home".equals(url);if(t.home){t.title="Home";t.pendingUrl=null;}t.session.open(runtime);t.session.setActive(false);t.session.loadUri(t.home?"about:blank":url);if(select)switchTo(tabs.size()-1);else renderTabs();}
 void activatePopup(Tab t){if(t.popupPending&&t.session.isOpen()&&tabs.contains(t)&&!isFinishing()&&!isDestroyed()){t.popupPending=false;switchTo(tabs.indexOf(t));}}
 void openGmail(){for(int i=0;i<tabs.size();i++){Uri uri=Uri.parse(tabs.get(i).url);if(!"https".equals(uri.getScheme())&&!"http".equals(uri.getScheme()))continue;String host=uri.getHost(),next=uri.getQueryParameter("continue");if("mail.google.com".equals(host)||("accounts.google.com".equals(host)&&((next!=null&&next.contains("mail.google.com"))||"mail".equals(uri.getQueryParameter("service"))))){switchTo(i);return;}}add("https://mail.google.com/",true);}
 void switchTo(int index){if(index<0||index>=tabs.size()||!tabs.get(index).session.isOpen())return;features.capture(current());if(current()!=null){current().session.setActive(false);runtime.getWebExtensionController().setTabActive(current().session,false);}selected=index;if(web.getSession()!=null)web.releaseSession();web.setSession(current().session);current().session.setActive(true);runtime.getWebExtensionController().setTabActive(current().session,true);address.setText(current().home?"":current().url);features.syncHome();renderTabs();updateNavigation();updateBookmark();tabScroll.post(()->tabScroll.smoothScrollTo(dp(selected*168),0));save();}
 void navigate(String text){if(!ready||current()==null)return;text=text.trim();if(text.isEmpty())return;if(!text.matches("(?i)^https?://.*")){if(!text.contains(" ")&&text.contains("."))text="https://"+text;else text="https://duckduckgo.com/?q="+Uri.encode(text);}Tab t=current();t.home=false;features.syncHome();t.url=text;t.pendingUrl=text;address.setText(text);updateBookmark();t.session.loadUri(text);save();}
 void save(){if(!ready||tabs.isEmpty())return;JSONArray a=new JSONArray();for(Tab t:tabs)a.put(features.tabRecord(t));prefs.edit().putString("session_tabs",a.toString()).putInt("selected",selected).apply();}
 void history(Tab t){if(!t.url.startsWith("http"))return;try{JSONArray old=new JSONArray(prefs.getString("history","[]")),a=new JSONArray();JSONObject o=new JSONObject();o.put("url",t.url);o.put("title",t.title);o.put("time",System.currentTimeMillis());a.put(o);for(int i=0;i<Math.min(499,old.length());i++)if(!old.getJSONObject(i).getString("url").equals(t.url))a.put(old.get(i));prefs.edit().putString("history",a.toString()).apply();}catch(Exception ignored){}}
 void showTabs(){if(ready)chrome.tabs();}
 void menu(){chrome.menu();}

 JSONArray bookmarks(){try{return new JSONArray(prefs.getString("bookmarks","[]"));}catch(Exception ignored){return new JSONArray();}}
 int bookmarkIndex(JSONArray a,String url){for(int i=0;i<a.length();i++)if(url.equals(a.optJSONObject(i)==null?null:a.optJSONObject(i).optString("url")))return i;return -1;}
 boolean pageAvailable(){Tab t=current();return ready&&t!=null&&t.url!=null&&t.url.matches("(?i)^https?://.+");}
 void updateBookmark(){boolean usable=pageAvailable();boolean saved=usable&&bookmarkIndex(bookmarks(),current().url)>=0;bookmarkButton.setEnabled(usable);bookmarkButton.setAlpha(1f);bookmarkButton.setSymbol(saved?R.drawable.ic_bookmark_saved:R.drawable.ic_bookmark);bookmarkButton.setIconColor(saved?accent():(usable?buttonInk():disabledInk()));bookmarkButton.setContentDescription(saved?"Remove bookmark":"Bookmark this page");bookmarkButton.setTooltipText(saved?"Remove bookmark":"Bookmark this page");}
 void removeBookmark(String url){JSONArray old=bookmarks(),updated=new JSONArray();for(int i=0;i<old.length();i++)if(old.optJSONObject(i)!=null&&!url.equals(old.optJSONObject(i).optString("url")))updated.put(old.optJSONObject(i));prefs.edit().putString("bookmarks",updated.toString()).apply();updateBookmark();}
 void toggleBookmark(){if(!pageAvailable())return;Tab t=current();JSONArray a=bookmarks();if(bookmarkIndex(a,t.url)>=0){removeBookmark(t.url);Toast.makeText(this,"Bookmark removed",Toast.LENGTH_SHORT).show();return;}try{JSONObject item=new JSONObject();item.put("url",t.url);item.put("title",t.title);a.put(item);prefs.edit().putString("bookmarks",a.toString()).apply();updateBookmark();Toast.makeText(this,"Bookmark saved",Toast.LENGTH_SHORT).show();}catch(Exception ignored){Toast.makeText(this,"Could not save bookmark",Toast.LENGTH_SHORT).show();}}
 void showBookmarks(){features.bookmarks();}

 void bookmarkActions(String url,String title){new AlertDialog.Builder(this).setTitle(title).setItems(new String[]{"Open in new tab","Rename","Delete"},(d,w)->{if(w==0)add(url,true);if(w==2){removeBookmark(url);showBookmarks();}if(w==1){EditText name=new EditText(this);name.setSingleLine();name.setText(title);new AlertDialog.Builder(this).setTitle("Rename bookmark").setView(name).setPositiveButton("Save",(p,b)->{String value=name.getText().toString().trim();if(value.isEmpty())return;JSONArray a=bookmarks();int index=bookmarkIndex(a,url);if(index>=0)try{a.getJSONObject(index).put("title",value);prefs.edit().putString("bookmarks",a.toString()).apply();}catch(Exception ignored){}showBookmarks();}).setNegativeButton("Cancel",null).show();}}).show();}
 void sharePage(){if(!pageAvailable())return;Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_SUBJECT,current().title);send.putExtra(Intent.EXTRA_TEXT,current().url);try{startActivity(Intent.createChooser(send,"Share page"));}catch(ActivityNotFoundException error){Toast.makeText(this,"No sharing apps available",Toast.LENGTH_SHORT).show();}}
 void showHistory(){try{JSONArray a=new JSONArray(prefs.getString("history","[]"));String[] labels=new String[a.length()];for(int i=0;i<a.length();i++)labels[i]=a.getJSONObject(i).optString("title")+"\n"+a.getJSONObject(i).getString("url");new AlertDialog.Builder(this).setTitle("History").setItems(labels,(d,w)->{try{navigate(a.getJSONObject(w).getString("url"));}catch(Exception ignored){}}).setNegativeButton("Done",null).show();}catch(Exception ignored){}}
 // API33+ invokes handleBrowserBack through BackHandler; this override only supports legacy Android back dispatch.
 @android.annotation.SuppressLint("GestureBackNavigation")
 @Override public void onBackPressed(){handleBrowserBack();}
 void handleBrowserBack(){if(features.fullscreen){features.fullscreen=false;features.applyFullscreen();if(current()!=null)current().session.exitFullScreen();return;}if(current()!=null&&current().back){current().home=false;features.syncHome();current().session.goBack();}else if(android.os.Build.VERSION.SDK_INT>=33)finish();else super.onBackPressed();}
 @Override protected void onStart(){super.onStart();DownloadService.setVisible(true);}
 @Override protected void onStop(){DownloadService.setVisible(false);super.onStop();}
 @Override protected void onPause(){super.onPause();for(Tab t:tabs)if(!t.home&&t.session.isOpen())t.session.flushSessionState();save();}
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){if(features!=null)features.result(requestCode,resultCode,data);if(downloads!=null)downloads.onActivityResult(requestCode,resultCode,data);super.onActivityResult(requestCode,resultCode,data);}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(ready&&intent.getData()!=null)add(intent.getData().toString(),true);if(ready&&intent.getBooleanExtra("show_downloads",false)){intent.removeExtra("show_downloads");downloads.showTransfers();}}
 @Override protected void onDestroy(){destroying=true;if(backHandler!=null)backHandler.close();if(downloads!=null)downloads.close();if(web!=null&&web.getSession()!=null)web.releaseSession();for(Tab t:tabs){if(!DownloadService.isPinned(t.session))t.session.close();else DownloadService.detach(t.session);}super.onDestroy();}
}
