package org.shield.browser;

import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import org.json.*;
import org.mozilla.geckoview.GeckoSessionSettings;

/** Original Gecko browser chrome modeled on the supplied Chrome toolbar, menu and tab grid. */
final class BrowserUi {
    final MainActivity a;
    final ArrayList<View> controls = new ArrayList<>();
    View tabRow, navigation;

    BrowserUi(MainActivity activity) { a=activity; }
    int background(){return a.dark?0xff202124:0xffffffff;}
    int surface(){return a.dark?0xff292a2d:0xfff1f3f4;}
    int addressFill(){return a.dark?0xff303134:0xffe8eaed;}
    int low(){return a.dark?0xff171717:0xfff8f9fa;}
    int muted(){return a.dark?0xffbdc1c6:0xff5f6368;}
    int outline(){return a.dark?0xff3c4043:0xffe8eaed;}

    TextView text(String value,int size,int color){
        TextView v=new TextView(a);v.setText(a.tr(value));v.setTextSize(size);v.setTextColor(color);
        v.setFontFeatureSettings("kern");return v;
    }
    LinearLayout column(){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);return v;}
    GradientDrawable rounded(int color,int radius){return a.rounded(color,radius);}
    android.graphics.drawable.Drawable ripple(int color,int radius){
        return new RippleDrawable(ColorStateList.valueOf(a.dark?0x308ab4f8:0x201a73e8),rounded(color,radius),rounded(Color.WHITE,radius));
    }
    void setAction(View v,String label,Runnable action){
        v.setContentDescription(a.tr(label));v.setOnClickListener(x->action.run());v.setFocusable(true);
        v.setBackground(ripple(Color.TRANSPARENT,12));
    }
    IconButton icon(int symbol,String label,Runnable action){return a.icon(symbol,label,action);}

    void layout(){
        a.applyColors();controls.clear();
        a.root=column();BrowserLanguages.direction(a,a.root);a.root.setBackgroundColor(background());a.setContentView(a.root);
        a.root.setOnApplyWindowInsetsListener((v,insets)->{
            a.root.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;
        });
        buildTabStrip();buildAddress();buildNavigation();
        a.progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);
        a.progress.setMax(100);a.progress.setProgressTintList(ColorStateList.valueOf(a.accent()));
        a.progress.setProgressBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        a.progress.setVisibility(View.INVISIBLE);controls.add(a.progress);
        boolean top="top".equals(a.prefs.getString("toolbar_position","top"));
        if(a.prefs.getBoolean("show_tab_strip",false))a.root.addView(tabRow);
        if(top){a.root.addView(a.bar);a.root.addView(navigation);a.root.addView(a.progress,new LinearLayout.LayoutParams(-1,a.dp(3)));}
        a.features.attach();
        if(!top){a.root.addView(a.progress,new LinearLayout.LayoutParams(-1,a.dp(3)));a.root.addView(a.bar);a.root.addView(navigation);}
        a.renderTabs();a.updateNavigation();a.updateBookmark();
    }

    void buildTabStrip(){
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(4));row.setBackgroundColor(surface());
        a.tabScroll=new HorizontalScrollView(a);a.tabScroll.setHorizontalScrollBarEnabled(false);
        a.tabStrip=new LinearLayout(a);a.tabStrip.setGravity(Gravity.CENTER_VERTICAL);a.tabScroll.addView(a.tabStrip);
        row.addView(a.tabScroll,new LinearLayout.LayoutParams(0,a.dp(44),1));
        row.addView(icon(R.drawable.ic_add,"New tab",()->a.add("kosher:home",true)),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        tabRow=row;controls.add(row);
    }

    void buildAddress(){
        a.bar=new LinearLayout(a);a.bar.setGravity(Gravity.CENTER_VERTICAL);a.bar.setPadding(a.dp(4),a.dp(4),a.dp(4),a.dp(4));
        a.bar.setBackgroundColor(background());
        a.bar.addView(icon(R.drawable.ic_home,"Home",a.features::homepage),new LinearLayout.LayoutParams(a.dp(40),a.dp(48)));
        LinearLayout box=new LinearLayout(a);box.setGravity(Gravity.CENTER_VERTICAL);box.setBackground(rounded(addressFill(),24));
        box.addView(icon(IconButton.SHIELD,"Built-in protections active",a.features::dashboard),new LinearLayout.LayoutParams(a.dp(40),a.dp(48)));
        a.address=new EditText(a);a.address.setSingleLine();a.address.setTextSize(16);a.address.setTextColor(a.ink());
        a.address.setHintTextColor(muted());a.address.setHint(a.tr("Search or enter address"));a.address.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);a.address.setTextDirection(View.TEXT_DIRECTION_LTR);a.address.setContentDescription("Address and DuckDuckGo search");
        a.address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        a.address.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        a.address.setSelectAllOnFocus(true);a.address.setBackgroundColor(Color.TRANSPARENT);a.address.setPadding(0,0,0,0);
        a.address.setOnEditorActionListener((v,id,event)->{
            a.navigate(a.address.getText().toString());a.address.clearFocus();
            ((InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(a.address.getWindowToken(),0);return true;
        });
        box.addView(a.address,new LinearLayout.LayoutParams(0,a.dp(48),1));
        a.bookmarkButton=icon(R.drawable.ic_bookmark,"Bookmark this page",a::toggleBookmark);
        box.addView(a.bookmarkButton,new LinearLayout.LayoutParams(a.dp(40),a.dp(48)));
        a.bar.addView(box,new LinearLayout.LayoutParams(0,a.dp(48),1));
        a.count=icon(0,"Tabs",a::showTabs);a.count.setCount(1);
        a.bar.addView(a.count,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        a.bar.addView(icon(IconButton.MENU,"Browser menu",()->{if(a.ready)menu();}),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        controls.add(a.bar);
    }

    void buildNavigation(){
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setBackgroundColor(background());
        a.backButton=icon(R.drawable.ic_back,"Back",()->{if(a.current()!=null&&a.current().back){a.current().home=false;a.features.syncHome();a.current().session.goBack();}});
        a.forwardButton=icon(R.drawable.ic_forward,"Forward",()->{if(a.current()!=null&&a.current().forward){a.current().home=false;a.features.syncHome();a.current().session.goForward();}});
        row.addView(a.backButton,new LinearLayout.LayoutParams(0,a.dp(48),1));row.addView(a.forwardButton,new LinearLayout.LayoutParams(0,a.dp(48),1));
        row.addView(icon(R.drawable.ic_home,"Home",a.features::homepage),new LinearLayout.LayoutParams(0,a.dp(48),1));
        row.addView(icon(R.drawable.ic_reload,"Reload",()->{if(a.current()!=null){if(a.current().home)home();else a.current().session.reload();}}),new LinearLayout.LayoutParams(0,a.dp(48),1));
        row.addView(icon(R.drawable.ic_add,"New tab",()->a.add("kosher:home",true)),new LinearLayout.LayoutParams(0,a.dp(48),1));
        row.setVisibility(a.prefs.getBoolean("show_navigation",false)?View.VISIBLE:View.GONE);
        navigation=row;controls.add(row);
    }

    void fullscreen(boolean full){
        for(View v:controls)v.setVisibility(full?View.GONE:View.VISIBLE);
        if(!full){navigation.setVisibility(a.prefs.getBoolean("show_navigation",true)?View.VISIBLE:View.GONE);a.progress.setVisibility(View.INVISIBLE);}
    }

    void heading(LinearLayout parent,String value,Runnable action){
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,a.dp(12),0,a.dp(12));
        TextView title=text(value,18,a.ink());title.setTypeface(null,Typeface.BOLD);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        if(action!=null){TextView all=text("Show all",14,a.accent());all.setPadding(a.dp(12),a.dp(8),a.dp(4),a.dp(8));setAction(all,"Show all "+value,action);row.addView(all);}
        parent.addView(row);
    }
    String host(String url){try{String h=Uri.parse(url).getHost();return h==null?url:h.replaceFirst("^www\\.","");}catch(Exception e){return url;}}
    View shortcut(String title,String url,Runnable open){
        LinearLayout tile=column();tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(a.dp(6),a.dp(10),a.dp(6),a.dp(10));
        FrameLayout image=new FrameLayout(a);image.setBackground(rounded(surface(),16));
        TextView glyph=text(title.isEmpty()?"K":title.substring(0,1).toUpperCase(Locale.ROOT),24,a.accent());glyph.setGravity(Gravity.CENTER);glyph.setTypeface(null,Typeface.BOLD);
        image.addView(glyph,new FrameLayout.LayoutParams(-1,-1));tile.addView(image,new LinearLayout.LayoutParams(a.dp(56),a.dp(56)));
        TextView label=text(title,12,a.ink());label.setGravity(Gravity.CENTER);label.setSingleLine();label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=a.dp(8);tile.addView(label,lp);
        setAction(tile,title,open);return tile;
    }
    void home(){
        BrowserFeatures f=a.features;if(f.home!=null)f.content.removeView(f.home);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setBackgroundColor(background());
        LinearLayout page=column();page.setPadding(a.dp(20),a.dp(24),a.dp(20),a.dp(24));scroll.addView(page);
        LinearLayout brand=column();brand.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo=new ImageView(a);logo.setImageResource(R.drawable.kosher_logo);logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setBackground(rounded(surface(),16));logo.setClipToOutline(true);brand.addView(logo,new LinearLayout.LayoutParams(a.dp(72),a.dp(72)));
        LinearLayout name=column();name.setPadding(0,a.dp(12),0,0);name.setGravity(Gravity.CENTER_HORIZONTAL);TextView title=text("Kosher Browsing",24,a.ink());title.setTypeface(null,Typeface.BOLD);name.addView(title);brand.addView(name,new LinearLayout.LayoutParams(-1,-2));page.addView(brand);
        LinearLayout search=new LinearLayout(a);search.setGravity(Gravity.CENTER_VERTICAL);search.setPadding(a.dp(12),0,a.dp(12),0);search.setBackground(ripple(addressFill(),28));
        search.addView(icon(IconButton.SEARCH,"Search",()->{a.address.requestFocus();((InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).showSoftInput(a.address,InputMethodManager.SHOW_IMPLICIT);}),new LinearLayout.LayoutParams(a.dp(40),a.dp(56)));
        TextView hint=text("Search with DuckDuckGo",16,muted());search.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
        setAction(search,"Search with DuckDuckGo",()->{a.address.requestFocus();((InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).showSoftInput(a.address,InputMethodManager.SHOW_IMPLICIT);});search.setBackground(ripple(addressFill(),28));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,a.dp(56));sp.topMargin=a.dp(28);sp.bottomMargin=a.dp(12);page.addView(search,sp);
        
        LinearLayout tiles=new LinearLayout(a);tiles.addView(shortcut("Gmail","https://mail.google.com/",a::openGmail),new LinearLayout.LayoutParams(0,-2,1));
        tiles.addView(shortcut("DuckDuckGo","https://duckduckgo.com/",()->a.navigate("https://duckduckgo.com/")),new LinearLayout.LayoutParams(0,-2,1));
        tiles.addView(shortcut("Bookmarks","",a::showBookmarks),new LinearLayout.LayoutParams(0,-2,1));page.addView(tiles);
        if(a.prefs.getBoolean("home_bookmarks",false)){
            JSONArray saved=a.bookmarks();heading(page,"Bookmarks",a::showBookmarks);
            if(saved.length()==0){TextView empty=text("Save pages with the bookmark button in the address bar.",14,muted());empty.setPadding(0,0,0,a.dp(16));page.addView(empty);}
            for(int i=0;i<Math.min(4,saved.length());i++){JSONObject item=saved.optJSONObject(i);if(item!=null)page.addView(pageRow(R.drawable.ic_bookmark,item.optString("title"),host(item.optString("url")),()->a.navigate(item.optString("url"))));}
        }
        if(a.prefs.getBoolean("home_recent",false)){
            JSONArray history=f.array("history");heading(page,"Recently visited",a::showHistory);
            if(history.length()==0)page.addView(text("Your recent pages will appear here.",14,muted()));
            for(int i=0;i<Math.min(4,history.length());i++){JSONObject item=history.optJSONObject(i);if(item!=null)page.addView(pageRow(IconButton.HISTORY,item.optString("title"),host(item.optString("url")),()->a.navigate(item.optString("url"))));}
        }
        LinearLayout protectedRow=new LinearLayout(a);protectedRow.setGravity(Gravity.CENTER_VERTICAL);protectedRow.setPadding(a.dp(8),a.dp(12),a.dp(12),a.dp(12));
        protectedRow.addView(icon(IconButton.SHIELD,"Protection dashboard",f::dashboard),new LinearLayout.LayoutParams(a.dp(40),a.dp(40)));
        protectedRow.addView(text("Browsing protections are always on",13,muted()));setAction(protectedRow,"Protection dashboard",f::dashboard);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=a.dp(20);page.addView(protectedRow,pp);
        TextView customize=text("Customize homepage",14,a.accent());customize.setGravity(Gravity.CENTER);customize.setPadding(a.dp(12),a.dp(16),a.dp(12),a.dp(16));setAction(customize,"Customize homepage",()->new BrowserSettings(a).show());page.addView(customize);
        f.home=scroll;f.content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
    }

    View pageRow(int symbol,String title,String subtitle,Runnable action){
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(a.dp(4),a.dp(6),a.dp(12),a.dp(6));
        row.addView(icon(symbol,title,action),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        LinearLayout labels=column();TextView name=text(title,15,a.ink());name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(name);
        if(!subtitle.isEmpty()){TextView sub=text(subtitle,12,muted());sub.setSingleLine();sub.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(sub);}
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));setAction(row,title,action);return row;
    }

    Dialog sheet(){
        Dialog d=new Dialog(a);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.42f);}
        return d;
    }
    void showSheet(Dialog d,View view){d.setContentView(view);d.show();Window w=d.getWindow();if(w!=null){w.setLayout(Math.min(a.dp(304),a.getResources().getDisplayMetrics().widthPixels-a.dp(16)),-2);w.setGravity(Gravity.TOP|Gravity.END);WindowManager.LayoutParams lp=w.getAttributes();lp.x=a.dp(8);lp.y=a.dp(64);w.setAttributes(lp);BrowserLanguages.direction(a,view);}}
    void menuRow(LinearLayout list,Dialog d,int symbol,String title,Runnable action){
        View row=pageRow(symbol,title,"",()->{d.dismiss();action.run();});list.addView(row,new LinearLayout.LayoutParams(-1,a.dp(52)));
    }
    void divider(LinearLayout list){View line=new View(a);line.setBackgroundColor(outline());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,a.dp(1));p.topMargin=a.dp(8);p.bottomMargin=a.dp(8);list.addView(line,p);}
    IconButton quickAction(int symbol,String label,boolean enabled,Runnable action){IconButton b=icon(symbol,label,action);b.setEnabled(enabled);if(!enabled)b.setIconColor(a.disabledInk());return b;}
    void menu(){
        Dialog d=sheet();LinearLayout outer=column();outer.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(16));
        outer.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(16)+insets.getSystemWindowInsetBottom());return insets;});
        GradientDrawable shape=rounded(background(),24);shape.setCornerRadius(a.dp(12));outer.setBackground(shape);
        View handle=new View(a);handle.setBackground(rounded(outline(),2));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(a.dp(32),a.dp(4));hp.gravity=Gravity.CENTER;hp.bottomMargin=a.dp(8);
        LinearLayout quick=new LinearLayout(a);quick.setGravity(Gravity.CENTER_VERTICAL);
        quick.addView(quickAction(R.drawable.ic_back,"Back",a.current()!=null&&a.current().back,()->{d.dismiss();if(a.current()!=null&&a.current().back){a.current().home=false;a.features.syncHome();a.current().session.goBack();}}),new LinearLayout.LayoutParams(0,a.dp(48),1));
        quick.addView(quickAction(R.drawable.ic_forward,"Forward",a.current()!=null&&a.current().forward,()->{d.dismiss();if(a.current()!=null&&a.current().forward){a.current().home=false;a.features.syncHome();a.current().session.goForward();}}),new LinearLayout.LayoutParams(0,a.dp(48),1));
        quick.addView(quickAction(R.drawable.ic_reload,"Reload",a.current()!=null,()->{d.dismiss();if(a.current()!=null){if(a.current().home)home();else a.current().session.reload();}}),new LinearLayout.LayoutParams(0,a.dp(48),1));
        quick.addView(quickAction(R.drawable.ic_bookmark,"Bookmark page",a.pageAvailable(),()->{d.dismiss();a.toggleBookmark();}),new LinearLayout.LayoutParams(0,a.dp(48),1));
        quick.addView(quickAction(IconButton.SHARE,"Share page",a.pageAvailable(),()->{d.dismiss();a.sharePage();}),new LinearLayout.LayoutParams(0,a.dp(48),1));outer.addView(quick);
        ScrollView scroll=new ScrollView(a);LinearLayout list=column();scroll.addView(list);
        int max=a.getResources().getDisplayMetrics().heightPixels-a.dp(230);outer.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(a.dp(580),Math.max(a.dp(220),max))));
        menuRow(list,d,R.drawable.ic_add,"New tab",()->a.add("kosher:home",true));divider(list);
        menuRow(list,d,IconButton.HISTORY,"History",a::showHistory);menuRow(list,d,R.drawable.ic_bookmark,"Bookmarks",a::showBookmarks);
        menuRow(list,d,IconButton.DOWNLOAD,"Downloads",a.downloads::showDownloads);menuRow(list,d,IconButton.DOWNLOAD,"Download transfers",a.downloads::showTransfers);menuRow(list,d,IconButton.SHARE,"Share page",a::sharePage);menuRow(list,d,IconButton.PAGE,"Passwords",a.features::autofill);divider(list);
        if(a.pageAvailable()){
            menuRow(list,d,IconButton.SEARCH,"Find in page",a.features::find);
            menuRow(list,d,IconButton.DESKTOP,"Desktop site"+(a.current().desktop?"  ✓":""),this::desktop);
            menuRow(list,d,IconButton.PAGE,"Translate",()->BrowserLanguages.translate(a,a.current()));
            menuRow(list,d,IconButton.PAGE,"Show original",()->a.current().session.getSessionTranslation().restoreOriginalPage());
            menuRow(list,d,IconButton.PAGE,"Reader view",()->{a.features.reader();a.features.command("font",a.prefs.getInt("reader_font",19));});
        }
        menuRow(list,d,IconButton.FULLSCREEN,"Full screen",a.features::toggleFullscreen);
        menuRow(list,d,IconButton.SHIELD,"Protections",a.features::dashboard);divider(list);
        menuRow(list,d,R.drawable.ic_settings,"Settings",()->new BrowserSettings(a).show());
        a.address.clearFocus();((InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(a.address.getWindowToken(),0);
        showSheet(d,outer);outer.requestApplyInsets();
    }
    void desktop(){
        MainActivity.Tab t=a.current();if(t==null||t.home)return;t.desktop=!t.desktop;
        t.session.getSettings().setUserAgentMode(t.desktop?GeckoSessionSettings.USER_AGENT_MODE_DESKTOP:GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        t.session.getSettings().setViewportMode(t.desktop?GeckoSessionSettings.VIEWPORT_MODE_DESKTOP:GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        t.session.reload();a.save();
    }

    void tabs(){
        a.features.capture(a.current());Dialog d=new Dialog(a);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout page=column();page.setBackgroundColor(surface());BrowserLanguages.direction(a,page);
        page.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(a.dp(8),a.dp(8),a.dp(8),a.dp(8));
        header.addView(icon(R.drawable.ic_add,"New tab",()->{d.dismiss();a.add("kosher:home",true);}),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        TextView title=text("Tabs",20,a.ink());title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(icon(IconButton.MENU,"Tab options",()->new AlertDialog.Builder(a).setItems(new String[]{"Recently closed tabs","Close all tabs","Settings"},(x,which)->{d.dismiss();if(which==0)a.features.closed();if(which==1)closeAll();if(which==2)new BrowserSettings(a).show();}).show()),new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));page.addView(header);
        TextView count=text(a.tabs.size()+" open tabs",14,a.accent());count.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(16));page.addView(count);
        ScrollView scroll=new ScrollView(a);LinearLayout cards=column();cards.setPadding(a.dp(12),0,a.dp(12),a.dp(12));scroll.addView(cards);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        boolean grid=a.prefs.getBoolean("tab_grid",true);int columns=grid?2:1;
        ArrayList<MainActivity.Tab> snapshot=new ArrayList<>(a.tabs);
        for(int i=0;i<snapshot.size();i+=columns){LinearLayout row=new LinearLayout(a);row.setBaselineAligned(false);
            for(int j=0;j<columns;j++){
                int index=i+j;if(index>=snapshot.size()){row.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));continue;}
                MainActivity.Tab t=snapshot.get(index);LinearLayout card=column();GradientDrawable bg=rounded(surface(),12);
                if(a.current()==t)bg.setStroke(a.dp(2),a.accent());card.setBackground(bg);card.setPadding(a.dp(2),a.dp(2),a.dp(2),a.dp(8));
                LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);TextView name=text(t.title,14,a.ink());name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(a.dp(10),0,0,0);top.addView(name,new LinearLayout.LayoutParams(0,a.dp(44),1));
                top.addView(icon(R.drawable.ic_close,"Close "+t.title,()->{d.dismiss();a.closeTab(t);tabs();}),new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));card.addView(top);
                FrameLayout preview=new FrameLayout(a);preview.setBackground(rounded(low(),8));
                if(t.preview!=null){ImageView image=new ImageView(a);image.setImageBitmap(t.preview);image.setScaleType(ImageView.ScaleType.CENTER_CROP);preview.addView(image,new FrameLayout.LayoutParams(-1,-1));}
                else {TextView placeholder=text(t.home?"Kosher\nBrowsing":host(t.url),18,muted());placeholder.setGravity(Gravity.CENTER);preview.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));}
                setAction(preview,"Open tab "+t.title,()->{d.dismiss();a.switchTo(a.tabs.indexOf(t));});card.addView(preview,new LinearLayout.LayoutParams(-1,a.dp(grid?164:120)));
                TextView site=text(t.home?"Home":host(t.url),12,muted());site.setSingleLine();site.setEllipsize(android.text.TextUtils.TruncateAt.END);site.setPadding(a.dp(10),a.dp(8),a.dp(10),0);card.addView(site);
                setAction(name,"Open tab "+t.title,()->{d.dismiss();a.switchTo(a.tabs.indexOf(t));});
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(a.dp(4),a.dp(4),a.dp(4),a.dp(8));row.addView(card,cp);
            }cards.addView(row);
        }
        TextView add=text("New tab",16,a.dark?0xff202124:Color.WHITE);add.setTypeface(null,Typeface.BOLD);add.setGravity(Gravity.CENTER);add.setBackground(ripple(a.accent(),24));add.setOnClickListener(v->{d.dismiss();a.add("kosher:home",true);});add.setContentDescription("New tab");
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,a.dp(48));np.setMargins(a.dp(20),a.dp(12),a.dp(20),a.dp(16));page.addView(add,np);
        d.setContentView(page);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,-1);w.setStatusBarColor(background());w.setNavigationBarColor(background());w.getDecorView().setSystemUiVisibility(a.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}page.requestApplyInsets();
    }
    void closeAll(){new AlertDialog.Builder(a).setTitle("Close all tabs?").setMessage("You can restore recently closed pages from the tab tray.").setPositiveButton("Close all",(d,w)->{
        a.web.releaseSession();for(MainActivity.Tab t:new ArrayList<>(a.tabs)){a.features.remember(t);if(!DownloadService.isPinned(t.session))t.session.close();else DownloadService.detach(t.session);}a.tabs.clear();a.selected=0;a.add("kosher:home",true);
    }).setNegativeButton("Cancel",null).show();}
}
