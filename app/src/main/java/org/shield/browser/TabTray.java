package org.shield.browser;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import java.util.*;

/** Searchable tab and group overview, drawn with native controls. */
final class TabTray {
    private final MainActivity a;
    private final BrowserUi ui;
    private Dialog dialog;
    private BackHandler backHandler;
    private LinearLayout shell,cards,header;
    private EditText search;
    private boolean groupsOnly,privateMode,selecting;
    private String group="";
    private final HashSet<MainActivity.Tab> selected=new HashSet<>();
    TabTray(MainActivity activity){a=activity;ui=a.chrome;privateMode=a.current()!=null&&a.current().incognito;}
    private int dp(int n){return a.dp(n);}
    private TextView label(String s,int size,int color){return ui.text(s,size,color);}
    private IconButton icon(int glyph,String name,Runnable action){return ui.icon(glyph,name,action);}
    private int highlight(){return a.dark?0xffced0ff:0xffdfe0ff;}
    private int onHighlight(){return 0xff404776;}
    void show(){
        a.features.capture(a.current());
        dialog=new Dialog(a,a.dark?R.style.KosherDark:R.style.KosherLight);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        shell=ui.column();shell.setBackgroundColor(ui.background());BrowserLanguages.direction(a,shell);
        shell.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(0,i.getSystemWindowInsetTop(),0,i.getSystemWindowInsetBottom());return i;});
        header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(14),dp(10),dp(6),dp(10));shell.addView(header,new LinearLayout.LayoutParams(-1,dp(64)));
        search=new EditText(a);search.setSingleLine();search.setTextSize(16);search.setTextColor(a.ink());search.setHintTextColor(ui.muted());search.setHint(a.tr("Search your tabs"));search.setContentDescription(a.tr("Search your tabs"));search.setPadding(dp(16),0,dp(16),0);search.setBackground(ui.rounded(ui.surface(),24));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(40));sp.setMargins(dp(14),dp(4),dp(14),dp(14));shell.addView(search,sp);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int c){renderCards();}});
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);cards=ui.column();cards.setPadding(dp(10),0,dp(10),dp(20));scroll.addView(cards);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        dialog.setContentView(shell);dialog.setOnDismissListener(d->{if(backHandler!=null)backHandler.close();a.root.post(()->{if(a.tabs.isEmpty())a.add("kosher:home",true,false,false);});});
        dialog.show();Window w=dialog.getWindow();w.setLayout(-1,-1);w.setBackgroundDrawableResource(android.R.color.transparent);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);w.setStatusBarColor(ui.background());w.setNavigationBarColor(ui.background());w.getDecorView().setSystemUiVisibility(a.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        backHandler=new BackHandler(w,this::back);backHandler.enable(true);
        dialog.setOnKeyListener((d,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP)back();return true;});
        refresh();shell.requestApplyInsets();
    }
    private void back(){if(selecting){selecting=false;selected.clear();refresh();}else if(!group.isEmpty()){group="";refresh();}else dialog.dismiss();}
    private void refresh(){
        selected.retainAll(a.tabs);header.removeAllViews();
        IconButton add=icon(selecting?R.drawable.ic_close:(!group.isEmpty()?R.drawable.ic_back:R.drawable.ic_add),selecting?"Cancel selection":(!group.isEmpty()?"Back to tabs":"New tab"),()->{if(selecting){selecting=false;selected.clear();refresh();}else if(!group.isEmpty()){group="";refresh();}else{dialog.dismiss();a.add("kosher:home",true,false,privateMode);}});
        add.setBackground(ui.rounded(highlight(),12));add.setIconColor(onHighlight());header.addView(add,new LinearLayout.LayoutParams(dp(40),dp(40)));
        FrameLayout center=new FrameLayout(a);header.addView(center,new LinearLayout.LayoutParams(0,-1,1));
        if(selecting||!group.isEmpty()){
            TextView title=label(selecting?selected.size()+" "+a.tr("selected"):group,17,a.ink());title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.END);title.setGravity(Gravity.CENTER);center.addView(title,new FrameLayout.LayoutParams(-1,-1));
        }else{
            LinearLayout segment=new LinearLayout(a);segment.setPadding(dp(4),dp(4),dp(4),dp(4));segment.setBackground(ui.rounded(ui.low(),20));FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(88),dp(44),Gravity.CENTER);center.addView(segment,cp);
            IconButton normal=icon(IconButton.TAB,"Tabs",()->{groupsOnly=false;refresh();});int count=0;for(MainActivity.Tab t:a.tabs)if(t.incognito==privateMode)count++;if(count>0)normal.setCount(count);
            normal.setBackground(ui.rounded(!groupsOnly?ui.surface():ui.low(),14));segment.addView(normal,new LinearLayout.LayoutParams(0,-1,1));
            IconButton grouped=icon(IconButton.GRID,"Tab groups",()->{groupsOnly=true;refresh();});grouped.setBackground(ui.rounded(groupsOnly?ui.surface():ui.low(),14));segment.addView(grouped,new LinearLayout.LayoutParams(0,-1,1));
        }
        header.addView(icon(IconButton.MENU,"Tab options",()->menu()),new LinearLayout.LayoutParams(dp(40),dp(44)));
        boolean secure=false;for(MainActivity.Tab t:a.tabs)if(t.incognito)secure=true;if(secure)dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);else dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        renderCards();
    }
    private ArrayList<MainActivity.Tab> visible(){ArrayList<MainActivity.Tab> list=new ArrayList<>();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(MainActivity.Tab t:a.tabs)if(t.incognito==privateMode&&(group.isEmpty()||group.equals(t.group))&&(t.title+" "+t.url+" "+t.group).toLowerCase(Locale.ROOT).contains(q))list.add(t);return list;}
    private void renderCards(){
        if(cards==null)return;cards.removeAllViews();ArrayList<MainActivity.Tab> list=visible();
        LinkedHashMap<String,ArrayList<MainActivity.Tab>> sets=new LinkedHashMap<>();
        for(MainActivity.Tab t:list){if(groupsOnly&&group.isEmpty()&&t.group.isEmpty())continue;String key=group.isEmpty()&&!t.group.isEmpty()?"g:"+t.group:"t:"+a.tabs.indexOf(t);ArrayList<MainActivity.Tab> entry=sets.get(key);if(entry==null){entry=new ArrayList<>();sets.put(key,entry);}entry.add(t);}
        if(sets.isEmpty()){
            LinearLayout empty=ui.column();empty.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,Math.max(dp(360),a.getResources().getDisplayMetrics().heightPixels-dp(190)));cards.addView(empty,ep);
            EmptyArt art=new EmptyArt();empty.addView(art,new LinearLayout.LayoutParams(dp(100),dp(110)));
            String title=!search.getText().toString().isEmpty()?"No matching tabs":(groupsOnly?"Your tab groups appear here":"You’ll find your tabs here");TextView heading=label(title,17,ui.muted());heading.setTypeface(null,Typeface.BOLD);heading.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(24);empty.addView(heading,hp);
            TextView hint=label(!search.getText().toString().isEmpty()?"Try a different search":(groupsOnly?"Create a group from Tab options":"Open tabs to visit different pages at the same time"),14,ui.muted());hint.setGravity(Gravity.CENTER);hint.setPadding(dp(16),dp(12),dp(16),0);empty.addView(hint);return;
        }
        LinearLayout row=null;int i=0;boolean grid=a.prefs.getBoolean("tab_grid",true);int columns=grid?2:1;
        for(Map.Entry<String,ArrayList<MainActivity.Tab>> entry:sets.entrySet()){
            if(i%columns==0){row=new LinearLayout(a);row.setBaselineAligned(false);cards.addView(row);}i++;
            boolean grouped=entry.getKey().startsWith("g:");View card=card(entry.getValue(),grouped,grid);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(dp(4),dp(2),dp(4),dp(10));row.addView(card,cp);
        }
        if(i%columns!=0)row.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));
    }
    private View card(ArrayList<MainActivity.Tab> tabs,boolean grouped,boolean grid){
        MainActivity.Tab t=tabs.get(0);boolean active=tabs.contains(a.current()),chosen=selected.containsAll(tabs);int fill=(active||chosen)?highlight():ui.surface(),ink=(active||chosen)?onHighlight():a.ink();
        LinearLayout card=ui.column();card.setPadding(dp(4),dp(2),dp(4),dp(4));card.setBackground(ui.rounded(fill,24));card.setClipToOutline(true);
        LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);
        IconButton marker=icon(selecting?(chosen?IconButton.SELECT:IconButton.TAB):IconButton.TAB,selecting?"Select "+t.title:"Open "+t.title,()->open(tabs,grouped));marker.setIconColor(ink);top.addView(marker,new LinearLayout.LayoutParams(dp(28),dp(36)));
        TextView title=label(grouped?t.group:t.title,13,ink);title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.END);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));title.setOnClickListener(v->open(tabs,grouped));
        IconButton more=icon(grouped?IconButton.MENU:R.drawable.ic_close,grouped?"Group options "+t.group:"Close "+t.title,()->{if(grouped)groupOptions(t.group,t.incognito);else close(tabs);});more.setIconColor(ink);top.addView(more,new LinearLayout.LayoutParams(dp(30),dp(36)));card.addView(top);
        if(grouped){
            LinearLayout previews=ui.column();for(int y=0;y<2;y++){LinearLayout line=new LinearLayout(a);for(int x=0;x<2;x++){int n=y*2+x;FrameLayout preview=preview(n<tabs.size()?tabs.get(n):null,(active||chosen)?0xffb6b9e9:ui.low());LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(grid?96:80),1);pp.setMargins(dp(1),dp(1),dp(1),dp(1));line.addView(preview,pp);preview.setOnClickListener(v->open(tabs,true));}previews.addView(line);}card.addView(previews);TextView count=label(tabs.size()+" "+a.tr(tabs.size()==1?"tab":"tabs"),12,ink);count.setPadding(dp(8),dp(4),dp(8),dp(4));card.addView(count);
        }else{FrameLayout preview=preview(t,ui.low());card.addView(preview,new LinearLayout.LayoutParams(-1,dp(grid?192:120)));preview.setOnClickListener(v->open(tabs,false));}
        card.setOnClickListener(v->open(tabs,grouped));card.setOnLongClickListener(v->{selecting=true;selected.addAll(tabs);refresh();return true;});card.setContentDescription((grouped?"Tab group "+t.group:"Tab "+t.title)+(chosen?", selected":""));return card;
    }
    private FrameLayout preview(MainActivity.Tab t,int color){FrameLayout frame=new FrameLayout(a);frame.setBackground(ui.rounded(color,8));frame.setClipToOutline(true);if(t==null)return frame;if(t.preview!=null&&!t.incognito){ImageView image=new ImageView(a);image.setImageBitmap(t.preview);image.setScaleType(ImageView.ScaleType.CENTER_CROP);frame.addView(image,new FrameLayout.LayoutParams(-1,-1));}else{TextView placeholder=label(t.incognito?"Incognito":t.home?"Kosher\nBrowsing":ui.host(t.url),15,color==0xffb6b9e9?onHighlight():ui.muted());placeholder.setGravity(Gravity.CENTER);placeholder.setPadding(dp(4),0,dp(4),0);frame.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));}return frame;}
    private void open(ArrayList<MainActivity.Tab> tabs,boolean grouped){if(selecting){if(selected.containsAll(tabs))selected.removeAll(tabs);else selected.addAll(tabs);refresh();return;}if(grouped){group=tabs.get(0).group;refresh();return;}MainActivity.Tab t=tabs.get(0);dialog.dismiss();a.switchTo(a.tabs.indexOf(t));}
    private void close(Collection<MainActivity.Tab> tabs){
        MainActivity.Tab current=a.current();int previousIndex=a.selected;boolean closedPrivate=false;
        if(a.web.getSession()!=null)a.web.releaseSession();
        for(MainActivity.Tab t:new ArrayList<>(tabs)){if(!a.tabs.remove(t))continue;closedPrivate|=t.incognito;a.features.remember(t);if(!DownloadService.isPinned(t.session))t.session.close();else DownloadService.detach(t.session);}
        selected.removeAll(tabs);
        if(a.finishClosingTabs(current,previousIndex,closedPrivate,true)){
            privateMode=false;groupsOnly=false;selecting=false;selected.clear();group="";dialog.dismiss();return;
        }
        if(!group.isEmpty()){boolean exists=false;for(MainActivity.Tab t:a.tabs)if(group.equals(t.group)&&t.incognito==privateMode)exists=true;if(!exists)group="";}
        refresh();
    }
    private void menu(){
        Dialog menu=ui.sheet();LinearLayout content=ui.column();content.setPadding(dp(8),dp(10),dp(8),dp(12));content.setBackground(ui.rounded(ui.menuFill(),24));
        if(selecting){entry(content,menu,IconButton.GROUP,"Group selected tabs",()->createGroup(new ArrayList<>(selected)));entry(content,menu,R.drawable.ic_close,"Close selected tabs",()->close(new ArrayList<>(selected)));entry(content,menu,IconButton.SELECT,"Select all",()->{selected.addAll(visible());refresh();});}
        else{entry(content,menu,IconButton.NEW_TAB,"New tab",()->{dialog.dismiss();a.add("kosher:home",true,false,privateMode);});entry(content,menu,IconButton.INCOGNITO,"New incognito tab",()->{dialog.dismiss();a.addIncognito();});entry(content,menu,IconButton.GROUP,"New tab group",()->createGroup(new ArrayList<>()));entry(content,menu,R.drawable.ic_close,"Close all tabs",()->new AlertDialog.Builder(a).setTitle(a.tr("Close all tabs?")).setPositiveButton(a.tr("Close all"),(d,w)->{ArrayList<MainActivity.Tab> all=new ArrayList<>();for(MainActivity.Tab t:a.tabs)if(t.incognito==privateMode)all.add(t);close(all);}).setNegativeButton(a.tr("Cancel"),null).show());entry(content,menu,IconButton.SELECT,"Select tabs",()->{selecting=true;refresh();});entry(content,menu,IconButton.INCOGNITO,privateMode?"Show regular tabs":"Show incognito tabs",()->{privateMode=!privateMode;group="";refresh();});entry(content,menu,IconButton.DELETE,"Delete browsing data",()->{dialog.dismiss();new BrowserSettings(a).showPage("Delete browsing data");});entry(content,menu,R.drawable.ic_settings,"Settings",()->{dialog.dismiss();new BrowserSettings(a).show();});}
        ui.showSheet(menu,content);
    }
    private void entry(LinearLayout list,Dialog menu,int glyph,String name,Runnable action){ui.menuRow(list,menu,glyph,name,action);}
    private void createGroup(ArrayList<MainActivity.Tab> members){EditText name=new EditText(a);name.setSingleLine();name.setHint(a.tr("Group name"));name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});new AlertDialog.Builder(a).setTitle(a.tr("New tab group")).setView(name).setPositiveButton(a.tr("Create"),(d,w)->{String title=name.getText().toString().trim();if(title.isEmpty())title=a.tr("Tab group");String candidate=title;int n=2;while(hasGroup(candidate,privateMode)){String suffix=" "+n++;candidate=title.substring(0,Math.min(title.length(),60-suffix.length()))+suffix;}if(members.isEmpty()){int before=a.tabs.size();a.add("kosher:home",false,false,privateMode);if(a.tabs.size()<=before)return;members.add(a.tabs.get(a.tabs.size()-1));}for(MainActivity.Tab t:members)t.group=candidate;a.save();group=candidate;selecting=false;selected.clear();refresh();}).setNegativeButton(a.tr("Cancel"),null).show();}
    private boolean hasGroup(String name,boolean incognito){for(MainActivity.Tab t:a.tabs)if(name.equals(t.group)&&t.incognito==incognito)return true;return false;}
    private void groupOptions(String name,boolean incognito){new AlertDialog.Builder(a).setTitle(name).setItems(new String[]{a.tr("Rename group"),a.tr("Ungroup tabs"),a.tr("Close group")},(d,which)->{ArrayList<MainActivity.Tab> members=new ArrayList<>();for(MainActivity.Tab t:a.tabs)if(name.equals(t.group)&&t.incognito==incognito)members.add(t);if(which==2){close(members);return;}if(which==1){for(MainActivity.Tab t:members)t.group="";group="";a.save();refresh();return;}EditText field=new EditText(a);field.setSingleLine();field.setText(name);field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});AlertDialog edit=new AlertDialog.Builder(a).setTitle(a.tr("Rename group")).setView(field).setPositiveButton(a.tr("Save"),null).setNegativeButton(a.tr("Cancel"),null).create();edit.setOnShowListener(v->edit.getButton(-1).setOnClickListener(x->{String next=field.getText().toString().trim();if(next.isEmpty()||(!next.equals(name)&&hasGroup(next,incognito))){field.setError(a.tr("Choose a unique group name"));return;}for(MainActivity.Tab t:members)t.group=next;if(group.equals(name))group=next;a.save();edit.dismiss();refresh();}));edit.show();}).show();}
    static void moveCurrent(MainActivity a){MainActivity.Tab t=a.current();if(t==null)return;ArrayList<String> groups=new ArrayList<>();groups.add(a.tr("New tab group"));for(MainActivity.Tab other:a.tabs)if(other.incognito==t.incognito&&!other.group.isEmpty()&&!groups.contains(other.group))groups.add(other.group);if(!t.group.isEmpty())groups.add(a.tr("Remove from group"));new AlertDialog.Builder(a).setTitle(a.tr("Move tab to group")).setItems(groups.toArray(new String[0]),(d,w)->{if(w==0){TabTray tray=new TabTray(a);tray.show();ArrayList<MainActivity.Tab> one=new ArrayList<>();one.add(t);tray.createGroup(one);}else{t.group=w==groups.size()-1&&!t.group.isEmpty()&&groups.get(w).equals(a.tr("Remove from group"))?"":groups.get(w);a.save();}}).setNegativeButton(a.tr("Cancel"),null).show();}
    private final class EmptyArt extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);EmptyArt(){super(a);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas c){c.save();c.scale(getWidth()/100f,getHeight()/110f);p.setStyle(Paint.Style.FILL);p.setColor(0xff3c477c);c.drawRoundRect(5,5,95,52,24,24,p);c.drawRoundRect(5,51,95,99,24,24,p);p.setColor(0xff929878);c.drawRoundRect(35,28,77,89,7,7,p);p.setColor(0xff8794cd);c.drawRoundRect(23,17,66,76,7,7,p);p.setColor(0xff3c477c);c.drawRoundRect(28,28,61,70,2,2,p);c.drawCircle(32,23,1.8f,p);c.drawRoundRect(36,22,48,24,1,1,p);c.restore();}
    }
}
