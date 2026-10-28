package org.shield.browser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.StorageController;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;

/** Native preferences adapted to the supplied Brave settings layout. */
final class BrowserSettings {
    private final MainActivity a;
    private Dialog dialog;
    private BackHandler backHandler;
    private LinearLayout page;
    private final ArrayDeque<String> stack = new ArrayDeque<>();

    BrowserSettings(MainActivity activity) { a = activity; }

    /** Apply on startup as well as after changing an accessibility preference. */
    static void applyWebPreferences(MainActivity a) {
        if (MainActivity.runtime == null) return;
        GeckoRuntimeSettings settings = MainActivity.runtime.getSettings();
        boolean automatic = a.prefs.getBoolean("font_auto", true);
        settings.setAutomaticFontSizeAdjustment(automatic);
        // Gecko rejects manual factors while its Android font-scale listener is enabled.
        if (!automatic) settings.setFontSizeFactor(Math.max(.5f, Math.min(2f, a.prefs.getInt("font_percent", 100) / 100f)));
        settings.setForceUserScalableEnabled(a.prefs.getBoolean("force_zoom", false));
        settings.setAllowInsecureConnections(a.prefs.getBoolean("https_only", false) ? GeckoRuntimeSettings.HTTPS_ONLY : GeckoRuntimeSettings.ALLOW_ALL);
        settings.setGlobalPrivacyControl(a.prefs.getBoolean("global_privacy_control", false));
        BrowserLanguages.apply(a);
    }

    void show() {
        if (dialog != null && dialog.isShowing()) return;
        dialog = new Dialog(a, a.dark ? R.style.KosherDark : R.style.KosherLight);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnKeyListener((d, key, event) -> {
            if (key == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) { back(); return true; }
            return key == KeyEvent.KEYCODE_BACK;
        });
        stack.clear();
        stack.push("Settings");
        render();
        dialog.show();
        backHandler=new BackHandler(dialog.getWindow(),this::back);backHandler.enable(true);
        dialog.setOnDismissListener(d->{if(backHandler!=null)backHandler.close();});
        styleWindow();
    }

    private int bg() { return a.dark ? 0xff17171f : 0xffffffff; }
    private int accent() { return a.dark ? 0xffa0a5eb : 0xff4c54d2; }
    private int outline() { return a.dark ? 0xff32323e : 0xffededf2; }
    private int secondary() { return a.dark ? 0xffc2c4cf : 0xff636577; }
    private int dp(int value) { return a.dp(value); }

    private void styleWindow() {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setLayout(-1, -1);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(bg());
        window.setNavigationBarColor(bg());
        window.getDecorView().setSystemUiVisibility(a.dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void open(String title) { stack.push(title); render(); }
    private void back() { if (stack.size() <= 1) dialog.dismiss(); else { stack.pop(); render(); } }
    private void refreshBrowser() { a.rebuildChrome(); render(); }
    private void leave(Runnable action) { dialog.dismiss(); action.run(); }
    private void toast(String message) { Toast.makeText(a, message, Toast.LENGTH_LONG).show(); }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(a);
        view.setText(a.tr(value));
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void render() {
        LinearLayout shell = new LinearLayout(a);
        shell.setOrientation(LinearLayout.VERTICAL);BrowserLanguages.direction(a,shell);
        shell.setBackgroundColor(bg());
        shell.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout header = new LinearLayout(a);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, dp(16), 0);
        Glyph back = new Glyph(Glyph.BACK);
        back.setContentDescription(stack.size() > 1 ? "Back to settings" : "Close settings");
        back.setBackground(ripple());
        back.setOnClickListener(v -> back());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(56)));
        TextView title = text(stack.peek(), 20, a.ink());
        title.setTypeface(null, Typeface.NORMAL);
        title.setPadding(dp(12), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        title.setGravity(Gravity.CENTER_VERTICAL);
        shell.addView(header);
        View line = new View(a);
        line.setBackgroundColor(outline());
        shell.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(8), 0, dp(28));
        scroll.addView(page);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        dialog.setContentView(shell);
        String screen = stack.peek();
        switch (screen) {
            case "Settings": main(); break;
            case "Search": search(); break;
            case "Tabs": tabs(); break;
            case "Home page": home(); break;
            case "Customize": case "Appearance": customize(); break;
            case "Passwords and autofill": passwords(); break;
            case "Accessibility": accessibility(); break;
            case "Language": case "Languages": language(); break;
            case "Browsing protections": protections(); break;
            case "HTTPS-Only Mode": httpsOnly(); break;
            case "Site permissions": permissions(); break;
            case "Delete browsing data": deleteData(); break;
            case "Notifications": notifications(); break;
            case "Privacy and data": privacy(); break;
            case "Extensions": extensions(); break;
            case "Open links in apps": externalLinks(); break;
            case "About Kosher Browsing": about(); break;
            case "Licenses": licenses(); break;
            default: main(); break;
        }
        BrowserLanguages.localize(a,shell);styleWindow();
    }

    private RippleDrawable ripple() {
        return new RippleDrawable(ColorStateList.valueOf(a.dark ? 0x228ab4f8 : 0x221a73e8), null, null);
    }

    private void group(String name) {
        if (page.getChildCount() > 0) { View divider = new View(a); divider.setBackgroundColor(outline()); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1)); p.topMargin = dp(12); page.addView(divider, p); }
        TextView label = text(name, 13, accent());
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(dp(20), dp(20), dp(20), dp(10));
        page.addView(label);
    }

    private void note(String message) {
        TextView detail = text(message, 14, secondary());
        detail.setLineSpacing(dp(3), 1);
        detail.setPadding(dp(24), dp(10), dp(24), dp(14));
        page.addView(detail);
    }

    private LinearLayout row(String title, String summary, View end, Runnable action) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(summary == null ? 56 : 72));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        if ("Settings".equals(stack.peek())) { SettingsIcon icon = new SettingsIcon(title); LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(24), dp(24)); ip.setMarginEnd(dp(28)); row.addView(icon, ip); }
        if (action != null) {
            row.setBackground(ripple());
            row.setFocusable(true);
            row.setOnClickListener(v -> action.run());
        }
        LinearLayout labels = new LinearLayout(a);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(title, 16, a.ink());
        labels.addView(name);
        if (summary != null) {
            TextView description = text(summary, 13, secondary());
            description.setPadding(0, dp(4), 0, 0);
            description.setLineSpacing(dp(2), 1);
            labels.addView(description);
        }
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        if (end != null && !("Settings".equals(stack.peek()) && end instanceof Glyph && ((Glyph)end).kind == Glyph.NEXT)) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.setMarginStart(dp(16));
            row.addView(end, p);
        }
        page.addView(row);
        return row;
    }

    private void item(String title, String summary, Runnable action) {
        row(title, summary, new Glyph(Glyph.NEXT), action);
    }

    private void link(String title, String summary, Runnable action) {
        row(title, summary, new Glyph(Glyph.EXTERNAL), action);
    }

    private void locked(String title, String summary) {
        row(title, summary, new Glyph(Glyph.LOCK), null);
    }

    private ColorStateList controlColors() {
        return new ColorStateList(new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { accent(), secondary() });
    }

    private void toggle(String title, String summary, String pref, boolean fallback, boolean rebuild, Runnable extra) {
        Switch value = new Switch(a);
        value.setChecked(a.prefs.getBoolean(pref, fallback));
        value.setThumbTintList(controlColors());
        value.setTrackTintList(new ColorStateList(new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { a.dark ? 0xff53416f : 0xffd4c5ed, outline() }));
        value.setContentDescription(title);
        value.setOnCheckedChangeListener((button, checked) -> {
            a.prefs.edit().putBoolean(pref, checked).apply();
            if (extra != null) extra.run();
            if (rebuild) refreshBrowser();
        });
        row(title, summary, value, () -> value.setChecked(!value.isChecked()));
    }

    private void radio(String title, String summary, boolean checked, Runnable action) {
        RadioButton radio = new RadioButton(a);
        radio.setButtonTintList(controlColors());
        radio.setChecked(checked);
        radio.setClickable(false);
        radio.setFocusable(false);
        radio.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout line = row(title, summary, radio, action);
        line.setContentDescription(title + (checked ? ", selected" : ", not selected"));
    }

    private void main() {
        group("Features");
        item("Browsing protections", "Always on", () -> open("Browsing protections"));
        item("Privacy and security", "Site data and browsing protections", () -> open("Privacy and data"));
        item("Protection dashboard", null, a.features::dashboard);
        group("General");
        item("Search engines", "DuckDuckGo", () -> open("Search"));
        item("Passwords and autofill", null, () -> open("Passwords and autofill"));
        link("Default browser", null, a.features::defaultBrowser);
        item("Languages", BrowserLanguages.appName(a), () -> open("Languages"));
        item("Downloads", "Open Android file manager", () -> leave(a.downloads::showDownloads));
        item("Download transfers", "Pause or resume downloads", () -> leave(a.downloads::showTransfers));
        item("Open links in apps", "Web links stay in this browser", () -> open("Open links in apps"));
        group("Display");
        item("Appearance", null, () -> open("Appearance"));
        item("Home page", null, () -> open("Home page"));
        item("Tabs", null, () -> open("Tabs"));
        item("Accessibility", null, () -> open("Accessibility"));
        group("Privacy");
        item("Site settings", null, () -> open("Site permissions"));
        item("HTTPS-Only Mode", null, () -> open("HTTPS-Only Mode"));
        item("Delete browsing data", null, () -> open("Delete browsing data"));
        item("Notifications", null, () -> open("Notifications"));
        group("Your browsing");
        item("Bookmarks", null, () -> leave(a.features::bookmarks));
        item("History", null, () -> leave(a::showHistory));
        group("About");
        item("Extensions", "Built-in browsing protections", () -> open("Extensions"));
        item("About Kosher Browsing", "Version 2.10", () -> open("About Kosher Browsing"));
    }

    private void search() {
        group("Default search engine");
        locked("DuckDuckGo", "Used by the address bar and homepage search. This choice is fixed.");
        group("Search safety");
        locked("Strict Safe Search", "Always enforced on supported DuckDuckGo, Google, Bing, Yahoo and Yandex search routes.");
        locked("Adult search blocking", "Adult-related search terms are blocked by the built-in policy.");
        locked("Search shortcuts", "Changing search engines or disabling Safe Search is unavailable.");
        note("Search providers can change their behavior. Safe Search reduces explicit results; it does not guarantee every result is appropriate.");
    }

    private void tabs() {
        group("Tab layout");
        boolean grid = a.prefs.getBoolean("tab_grid", true);
        radio("Grid", "Show page previews in a grid", grid, () -> {
            a.prefs.edit().putBoolean("tab_grid", true).apply(); render();
        });
        radio("List", "Show open tabs in a vertical list", !grid, () -> {
            a.prefs.edit().putBoolean("tab_grid", false).apply(); render();
        });
        group("Tab bar");
        toggle("Show tab strip", "Keep open tabs visible beside each other", "show_tab_strip", false, true, null);
        group("Keep your place");
        row("Restore open tabs", "Tabs and available back/forward history are saved between launches.", new Glyph(Glyph.CHECK), null);
        item("Open tabs", a.tabs.size() + " tabs open", () -> leave(a::showTabs));
        item("Recently closed", "Reopen one of the last 20 closed tabs", () -> leave(a.features::closed));
        note("Tabs close manually. This version does not automatically close inactive tabs.");
    }

    private void home() {
        group("What to show");
        toggle("Bookmarks", "Show saved sites on the homepage", "home_bookmarks", true, true, null);
        toggle("Recently visited", "Show sites from your browsing history", "home_recent", true, true, null);
        group("Always available");
        row("DuckDuckGo search", "Search safely from a new tab", new Glyph(Glyph.CHECK), null);
        row("Gmail", "A shortcut to mail.google.com and Google sign-in", new Glyph(Glyph.CHECK), null);
        item("Open homepage", null, () -> leave(a.features::homepage));
        note("Open tabs are restored when you restart the app. The Home button and new tabs open this homepage.");
    }

    private void customize() {
        group("Browser theme");
        String mode = a.prefs.getString("theme_mode", a.prefs.contains("dark") ? (a.prefs.getBoolean("dark", false) ? "dark" : "light") : "system");
        theme("Follow device theme", "Use Android's current light or dark appearance", "system", mode);
        theme("Light", null, "light", mode);
        theme("Dark", null, "dark", mode);
        group("Toolbar");
        String position = a.prefs.getString("toolbar_position", "bottom");
        radio("Top", "Address bar above the page", "top".equals(position), () -> {
            a.prefs.edit().putString("toolbar_position", "top").apply(); refreshBrowser();
        });
        radio("Bottom", "Address bar within easy reach", !"top".equals(position), () -> {
            a.prefs.edit().putString("toolbar_position", "bottom").apply(); refreshBrowser();
        });
        group("Navigation");
        toggle("Navigation controls", "Show Home, Back, Forward and Reload controls", "show_navigation", true, true, null);
        toggle("Show tab strip", "Display a horizontal row of open tabs", "show_tab_strip", false, true, null);
        note("The address bar, bookmarks button, tab switcher and menu stay available in either toolbar position.");
    }

    private void theme(String title, String summary, String value, String selected) {
        radio(title, summary, value.equals(selected), () -> {
            a.prefs.edit().putString("theme_mode", value).apply(); refreshBrowser();
        });
    }

    private void passwords() {
        group("Android autofill");
        row("Password-manager support", "Autofill is enabled for web forms. Availability depends on Android, your provider and the website.", new Glyph(Glyph.CHECK), null);
        link("Manage passwords and autofill", "Choose your preferred Android password manager", a.features::autofill);
        item("Open Gmail", "Sign in with Google's own website", () -> leave(a::openGmail));
        group("Storage");
        note("Kosher Browsing does not keep its own password or payment-card database. Website sign-ins stay available through cookies in the persistent Gecko profile. Clearing cookies signs you out.");
        note("Firefox accounts, Firefox Sync and Firefox Relay are not configured in this app.");
    }

    private void accessibility() {
        group("Text size");
        boolean automatic = a.prefs.getBoolean("font_auto", true);
        toggle("Use automatic font sizing", "Allow Gecko to adjust web text using Android settings", "font_auto", true, false, () -> { applyWebPreferences(a); render(); });
        TextView amount = text("Manual web text size: " + a.prefs.getInt("font_percent", 100) + "%", 17, automatic ? secondary() : a.ink());
        amount.setPadding(dp(24), dp(16), dp(24), dp(4));
        page.addView(amount);
        SeekBar scale = new SeekBar(a);
        scale.setMax(150);
        scale.setProgress(Math.max(0, Math.min(150, a.prefs.getInt("font_percent", 100) - 50)));
        scale.setProgressTintList(ColorStateList.valueOf(accent()));
        scale.setThumbTintList(ColorStateList.valueOf(accent()));
        scale.setContentDescription("Web text size, 50 to 200 percent");
        scale.setEnabled(!automatic);
        if (automatic) scale.setAlpha(.45f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.setMargins(dp(20), 0, dp(20), 0);
        page.addView(scale, p);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar view) { }
            public void onStopTrackingTouch(SeekBar view) { applyWebPreferences(a); }
            public void onProgressChanged(SeekBar view, int value, boolean user) {
                int percent = value + 50;
                amount.setText("Manual web text size: " + percent + "%");
                if (user) a.prefs.edit().putInt("font_percent", percent).apply();
            }
        });
        TextView preview = text("The quick brown fox jumps over the lazy dog.", 19, a.ink());
        preview.setPadding(dp(24), dp(12), dp(24), dp(18));
        page.addView(preview);
        if (automatic) note("Turn off automatic font sizing to choose a manual text size.");
        group("Zoom");
        toggle("Always enable zoom", "Allow pinch zoom even on pages that try to prevent it", "force_zoom", false, false, () -> applyWebPreferences(a));
        group("Reader mode");
        item("Reader font size", "Adjust text in Reader mode separately", a.features::font);
        note("Web text changes apply through Gecko's accessibility settings. Some sites use fixed layouts and may require a reload.");
    }

    private void language() {
        group("App language");
        item("Kosher Browsing language", BrowserLanguages.appName(a), () -> leave(() -> BrowserLanguages.appPicker(a)));
        group("Preferred languages");
        note("Let websites know the languages you prefer. Websites choose which versions they provide. Reload open pages after changing this list.");
        java.util.ArrayList<String> selected=BrowserLanguages.preferred(a);
        for(int i=0;i<selected.size();i++){
            final int index=i;String code=selected.get(i);
            item(BrowserLanguages.label(code),i==0?"First preference":code,()->{
                String[] choices={a.tr("Move up"),a.tr("Move down"),a.tr("Remove")};
                new AlertDialog.Builder(a).setTitle(BrowserLanguages.label(code)).setItems(choices,(d,w)->{
                    java.util.ArrayList<String> list=BrowserLanguages.preferred(a);
                    if(index>=list.size())return;
                    if(w==0&&index>0)java.util.Collections.swap(list,index,index-1);
                    if(w==1&&index+1<list.size())java.util.Collections.swap(list,index,index+1);
                    if(w==2&&list.size()>1)list.remove(index);
                    BrowserLanguages.savePreferred(a,list);render();
                }).setNegativeButton(a.tr("Cancel"),null).show();
            });
        }
        item("Add language",null,()->BrowserLanguages.addPreferred(a,this::render));
        group("Translation settings");
        toggle("Offer to translate pages", "Use Gecko translation for supported languages", "offer_translate", true, false, () -> BrowserLanguages.apply(a));
        item("Translate into this language",BrowserLanguages.label(BrowserLanguages.target(a)),()->BrowserLanguages.translationPicker(a,false,code->{a.prefs.edit().putString("translate_target",code).apply();render();}));
        item("Automatically translate these languages",null,()->BrowserLanguages.translationPicker(a,true,code->BrowserLanguages.languageRule(a,code,"always",this::render)));
        item("Never translate these languages",null,()->BrowserLanguages.translationPicker(a,true,code->BrowserLanguages.languageRule(a,code,"never",this::render)));
        for(java.util.Map.Entry<String,?> pref:a.prefs.getAll().entrySet()){
            if(!pref.getKey().startsWith("translation_rule_"))continue;
            String code=pref.getKey().substring("translation_rule_".length()),rule=String.valueOf(pref.getValue());
            if("offer".equals(rule))continue;
            item(BrowserLanguages.label(code),"always".equals(rule)?"Automatically translate":"Never translate",()->BrowserLanguages.languageRule(a,code,"offer",this::render));
        }
        note("Translation runs on this device using Gecko. Language models may download from Mozilla services. Availability depends on the device and language pair. Tap a saved translation rule to reset it.");
    }

    private void protections() {
        group("Always active");
        locked("Image filtering", "Skin-range pixels are grayscaled and strongly blurred with a 15-pixel border. Failed image filtering returns an empty image.");
        locked("Adult content and searches", "Bundled adult-site rules and adult-related search terms are blocked.");
        locked("Social-site blocking", "Reddit, X/Twitter, Instagram and related domains are blocked.");
        locked("Strict Safe Search", "Safe Search is forced on supported search routes; YouTube requests use Strict restricted mode.");
        locked("YouTube Shorts", "Shorts routes, links and shelves are blocked or hidden.");
        locked("Ad blocking", "Bundled host/path rules and cosmetic ad hiding are active.");
        group("Activity");
        item("Protection dashboard", "View blocked requests and image-filter counts", a.features::dashboard);
        note("These protections have no off switch or site exceptions. Rule lists are bundled snapshots and do not update automatically. Image color detection is a heuristic; video, canvas and unknown sites are not fully covered.");
    }

    private void permissions() {
        group("Device permissions");
        link("Manage Android permissions", "Control permissions granted to Kosher Browsing", () -> launch(appSettings()));
        group("Website data");
        if (a.pageAvailable()) item("Clear this site's data", Uri.parse(a.current().url).getHost(), a.features::clearSite);
        else note("Open a website to clear its cookies and site data individually.");
        item("Delete browsing data", "Clear cookies or permissions for all sites", () -> open("Delete browsing data"));
        note("Android permissions and website permissions are separate. This build does not include Firefox's per-site permission editor.");
    }

    private void httpsOnly() {
        group("Secure connections");
        toggle("HTTPS-Only Mode", "Use encrypted HTTPS connections whenever you visit a website", "https_only", false, false, () -> applyWebPreferences(a));
        note("When enabled, Gecko upgrades HTTP addresses to HTTPS. Websites that do not support HTTPS may fail to load.");
        note("This setting controls connection security. Built-in image filtering, site blocking and Safe Search stay permanently active.");
    }

    private CheckBox check(String title, String detail, boolean checked) {
        CheckBox checkbox = new CheckBox(a);
        checkbox.setChecked(checked);
        checkbox.setButtonTintList(controlColors());
        checkbox.setContentDescription(title);
        row(title, detail, checkbox, () -> checkbox.setChecked(!checkbox.isChecked()));
        return checkbox;
    }

    private void deleteData() {
        note("Choose the data to remove. Your bookmarks, settings and permanent protections will be kept.");
        CheckBox tabs = check("Open tabs", "Close every tab and remove saved tab sessions", false);
        CheckBox history = check("Browsing history", "Remove visited-site lists and recently closed tabs", true);
        CheckBox cookies = check("Cookies and site data", "Signs you out of websites and removes offline site data", false);
        CheckBox cache = check("Cached images and files", "Websites may take longer to load next time", true);
        CheckBox permissions = check("Site permissions", "Forget stored website permission choices", false);
        CheckBox downloads = check("Completed download history", "Keep downloaded files and paused or active transfers", false);
        Button clear = new Button(a);
        clear.setText("Delete browsing data");
        clear.setTextColor(a.dark ? 0xff271345 : 0xffffffff);
        clear.setBackgroundTintList(ColorStateList.valueOf(accent()));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(dp(24), dp(24), dp(24), dp(8));
        page.addView(clear, p);
        clear.setOnClickListener(v -> {
            if (!(tabs.isChecked() || history.isChecked() || cookies.isChecked() || cache.isChecked() || permissions.isChecked() || downloads.isChecked())) {
                toast("Choose at least one data type."); return;
            }
            String message = "Remove the selected browsing data? This cannot be undone.";
            if (cookies.isChecked()) message += " You will be signed out of websites, including Gmail.";
            if (history.isChecked() && !tabs.isChecked()) message += " Open tabs keep their current back/forward history until closed.";
            new AlertDialog.Builder(a).setTitle("Delete browsing data?").setMessage(message)
                    .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, which) -> {
                        clear.setEnabled(false);
                        clearData(tabs.isChecked(), history.isChecked(), cookies.isChecked(), cache.isChecked(), permissions.isChecked(), downloads.isChecked(), clear);
                    }).show();
        });
    }

    private void clearData(boolean closeTabs, boolean history, boolean cookies, boolean cache, boolean permissions, boolean downloads, Button button) {
        if (closeTabs) {
            if (a.web.getSession() != null) a.web.releaseSession();
            for (MainActivity.Tab tab : new ArrayList<>(a.tabs)) {
                if (!DownloadService.isPinned(tab.session)) tab.session.close();
                else DownloadService.detach(tab.session);
            }
            a.tabs.clear();
            a.selected = 0;
            a.prefs.edit().remove("session_tabs").remove("tabs").remove("selected").apply();
            a.add("kosher:home", true);
        }
        SharedPreferences.Editor editor = a.prefs.edit();
        if (history) editor.remove("history").remove("closed_tabs");
        editor.apply();
        if (downloads) synchronized (DownloadService.LOCK) {
            JSONArray kept = new JSONArray(), old = DownloadService.records(a);
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item != null && !"Complete".equals(item.optString("status"))) kept.put(item);
            }
            a.prefs.edit().putString("downloads", kept.toString()).apply();
        }
        long flags = 0;
        if (cookies) flags |= StorageController.ClearFlags.COOKIES | StorageController.ClearFlags.DOM_STORAGES | StorageController.ClearFlags.AUTH_SESSIONS;
        if (cache) flags |= StorageController.ClearFlags.ALL_CACHES;
        if (permissions) flags |= StorageController.ClearFlags.PERMISSIONS;
        a.features.syncHome();
        if (flags == 0) { button.setEnabled(true); toast("Selected browsing data deleted."); return; }
        MainActivity.runtime.getStorageController().clearData(flags).accept(result -> a.runOnUiThread(()->{
            button.setEnabled(true);
            toast("Selected browsing data deleted.");
        }), error -> a.runOnUiThread(()->{
            button.setEnabled(true);
            toast("Local lists were cleared, but Gecko could not clear all selected website data. Try again.");
        }));
    }

    private void notifications() {
        group("Download notifications");
        note("Download notifications show progress and Pause/Resume actions. Android controls whether this app may display them.");
        link("Manage notifications", "Open Android notification settings", () -> launch(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName())));
        item("Open Downloads", "Manage transfers inside the browser", () -> leave(a.downloads::showDownloads));
    }

    private void privacy() {
        group("Website privacy signals");
        toggle("Global Privacy Control", "Send websites a signal requesting limits on the sale or sharing of your data", "global_privacy_control", false, false, () -> applyWebPreferences(a));
        note("Websites decide how they respond to this signal. It does not replace the browser's built-in blocking rules.");
        group("Stored on this device");
        note("Bookmarks, history, settings and tab sessions are stored in this app's private storage. Website cookies and sign-ins use a persistent Gecko profile. Tab preview images are kept in memory.");
        group("Network requests");
        note("Web browsing and download requests go through the protected Gecko runtime. Websites and search providers receive normal network requests. Sharing a page opens Android's share chooser.");
        group("Accounts and sync");
        note("This app has no configured Firefox account, Sync or third-party analytics integration. It does not provide cloud backups or an account dashboard.");
        item("Delete browsing data", null, () -> open("Delete browsing data"));
    }

    private void extensions() {
        group("Installed with the app");
        locked("Kosher Browsing protections", "Image filtering, ad rules, adult/social blocks and strict Safe Search are packaged into this app.");
        item("Protection details", null, () -> open("Browsing protections"));
        note("Installing, disabling or replacing extensions is unavailable. The packaged extension is required before browsing starts.");
    }

    private void externalLinks() {
        group("Web links");
        locked("Keep web links in Kosher Browsing", "HTTP and HTTPS links open in protected browser tabs. Unsupported external-app schemes are blocked.");
        note("This policy helps keep web navigation inside the browser's protections. Completed downloads and links that you explicitly share use Android's app chooser.");
        link("Android app settings", "Manage supported web links and defaults", () -> launch(appSettings()));
    }

    private void about() {
        ImageView logo = new ImageView(a);
        logo.setImageResource(R.drawable.kosher_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(132));
        p.setMargins(dp(24), dp(24), dp(24), dp(12));
        page.addView(logo, p);
        String version = "";
        try { version = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName; } catch (Exception ignored) { }
        row("Kosher Browsing", "Version " + version + "\nMozilla GeckoView 158", null, null);
        note("A Gecko-based browser with permanent browsing protections. The app keeps the name and logo you supplied.");
        item("Licenses", "GeckoView, bundled rules and interface provenance", () -> open("Licenses"));
        note("This is an independent application. Firefox account services and Firefox branding are not included.");
    }

    private void licenses() {
        group("Mozilla GeckoView");
        note("This app uses Mozilla GeckoView, which is licensed under the Mozilla Public License 2.0 and includes components with their own notices. Gecko's packaged license information remains in the engine distribution.");
        link("Mozilla Public License 2.0", "mozilla.org/MPL/2.0", () -> leave(() -> a.add("https://www.mozilla.org/MPL/2.0/", true)));
        group("Interface reference");
        note("The settings layout follows the supplied Brave Android APK: icon rows, category order, typography and purple color roles. Native Java controls and geometric icons are original. The browser uses GeckoView; Brave accounts, Rewards, Wallet and VPN services are not included. Earlier toolbar and language layouts use the supplied Chrome reference.");
        group("Bundled AdBlock rule data");
        item("GNU General Public License 3.0", "License notice included in this app", () -> showAsset("COPYING-GPL-3.0.txt", "GNU GPL 3.0"));
        note("The project's THIRD-PARTY.md lists bundled rule-list origins and attribution. Rule snapshots are distributed with the application source.");
    }

    private void showAsset(String path, String title) {
        try (InputStream input = a.getAssets().open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1 && out.size() < 200000) out.write(buffer, 0, count);
            TextView body = text(out.toString("UTF-8"), 13, a.ink());
            body.setTextIsSelectable(true);
            body.setPadding(dp(20), dp(16), dp(20), dp(16));
            ScrollView scroll = new ScrollView(a);
            scroll.addView(body);
            new AlertDialog.Builder(a).setTitle(title).setView(scroll).setPositiveButton("Done", null).show();
        } catch (Exception error) { toast("The license text could not be opened. It is also included in the source package."); }
    }

    private Intent appSettings() {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName()));
    }

    private void launch(Intent intent) {
        try { a.startActivity(intent); }
        catch (Exception error) {
            try { a.startActivity(appSettings()); }
            catch (Exception unavailable) { toast("These settings are not available on this Android device."); }
        }
    }

    /** Original geometric icons matching the reference preference categories. */
    private final class SettingsIcon extends View {
        final String label; final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        SettingsIcon(String title) { super(a); label=title; setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        protected void onDraw(Canvas c) {
            super.onDraw(c); c.save(); c.scale(getWidth()/24f,getHeight()/24f);
            p.setColor(secondary()); p.setStrokeWidth(1.8f); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            Path x=new Path();
            if(label.contains("protection") || label.contains("Privacy") || label.contains("HTTPS")) {
                x.moveTo(12,2);x.lineTo(21,6);x.lineTo(20,14);x.quadTo(18,20,12,23);x.quadTo(6,20,4,14);x.lineTo(3,6);x.close();c.drawPath(x,p);c.drawLine(8,12,11,15,p);c.drawLine(11,15,16,9,p);
            } else if(label.contains("Search")) {c.drawCircle(10,10,6,p);c.drawLine(15,15,21,21,p);
            } else if(label.contains("Password")) {c.drawCircle(7,10,4,p);c.drawLine(11,10,22,10,p);c.drawLine(18,10,18,14,p);c.drawLine(21,10,21,13,p);
            } else if(label.contains("Language")) {c.drawCircle(12,12,10,p);c.drawOval(7,2,17,22,p);c.drawLine(2,12,22,12,p);c.drawLine(4,7,20,7,p);c.drawLine(4,17,20,17,p);
            } else if(label.contains("Download")) {c.drawLine(12,3,12,16,p);x.moveTo(7,11);x.lineTo(12,16);x.lineTo(17,11);c.drawPath(x,p);x.reset();x.moveTo(4,17);x.lineTo(4,21);x.lineTo(20,21);x.lineTo(20,17);c.drawPath(x,p);
            } else if(label.equals("Appearance")) {c.drawOval(2,3,22,21,p);c.drawCircle(7,10,1,p);c.drawCircle(11,7,1,p);c.drawCircle(16,8,1,p);c.drawCircle(18,13,1,p);c.drawCircle(10,16,2,p);
            } else if(label.equals("Home page")) {x.moveTo(2,11);x.lineTo(12,3);x.lineTo(22,11);x.moveTo(5,10);x.lineTo(5,21);x.lineTo(10,21);x.lineTo(10,15);x.lineTo(14,15);x.lineTo(14,21);x.lineTo(19,21);x.lineTo(19,10);c.drawPath(x,p);
            } else if(label.equals("Tabs") || label.contains("browser")) {c.drawRoundRect(3,3,21,21,2,2,p);c.drawLine(3,8,21,8,p);
            } else if(label.contains("Accessibility")) {c.drawCircle(12,4,2,p);c.drawLine(3,9,21,9,p);c.drawLine(12,7,12,15,p);c.drawLine(12,15,7,22,p);c.drawLine(12,15,17,22,p);
            } else if(label.contains("Bookmark")) {x.moveTo(6,3);x.lineTo(18,3);x.lineTo(18,22);x.lineTo(12,17);x.lineTo(6,22);x.close();c.drawPath(x,p);
            } else if(label.equals("History")) {c.drawArc(3,3,21,21,-80,300,false,p);c.drawLine(12,6,12,12,p);c.drawLine(12,12,16,14,p);x.moveTo(2,4);x.lineTo(2,10);x.lineTo(8,10);c.drawPath(x,p);
            } else if(label.contains("Delete")) {c.drawLine(4,6,20,6,p);c.drawLine(9,3,15,3,p);x.moveTo(6,6);x.lineTo(7,21);x.lineTo(17,21);x.lineTo(18,6);c.drawPath(x,p);c.drawLine(10,10,10,17,p);c.drawLine(14,10,14,17,p);
            } else if(label.contains("Notifications")) {x.moveTo(4,18);x.lineTo(6,15);x.lineTo(6,9);x.cubicTo(6,1,18,1,18,9);x.lineTo(18,15);x.lineTo(20,18);x.close();c.drawPath(x,p);c.drawArc(10,18,14,23,0,180,false,p);
            } else if(label.contains("dashboard")) {c.drawLine(4,21,4,12,p);c.drawLine(12,21,12,3,p);c.drawLine(20,21,20,8,p);
            } else if(label.contains("Site")) {c.drawLine(3,6,21,6,p);c.drawLine(3,12,21,12,p);c.drawLine(3,18,21,18,p);c.drawCircle(8,6,2,p);c.drawCircle(16,12,2,p);c.drawCircle(10,18,2,p);
            } else if(label.contains("links")) {c.drawRoundRect(2,9,14,17,4,4,p);c.drawRoundRect(10,5,22,13,4,4,p);
            } else if(label.contains("Extensions")) {c.drawRoundRect(3,6,21,22,2,2,p);c.drawCircle(12,5,3,p);c.drawCircle(20,14,3,p);
            } else {c.drawCircle(12,12,10,p);c.drawLine(12,11,12,17,p);c.drawCircle(12,7,0.7f,p);}
            c.restore();
        }
    }

    /** Small geometric controls keep navigation shapes distinct from their backgrounds. */
    private final class Glyph extends View {
        static final int NEXT = 1, BACK = 2, LOCK = 3, EXTERNAL = 4, CHECK = 5;
        final int kind;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Glyph(int type) { super(a); kind = type; setImportantForAccessibility(type == BACK ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onMeasure(int width, int height) {
            setMeasuredDimension(resolveSize(dp(kind == BACK ? 48 : 24), width), resolveSize(dp(kind == BACK ? 48 : 24), height));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.translate((getWidth() - dp(24)) / 2f, (getHeight() - dp(24)) / 2f);
            canvas.scale(dp(24) / 24f, dp(24) / 24f);
            if(getLayoutDirection()==View.LAYOUT_DIRECTION_RTL&&(kind==BACK||kind==NEXT)){canvas.translate(24,0);canvas.scale(-1,1);}
            paint.setColor(kind == LOCK || kind == CHECK ? accent() : secondary());
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            Path path = new Path();
            if (kind == NEXT) { path.moveTo(9, 6); path.lineTo(15, 12); path.lineTo(9, 18); }
            else if (kind == BACK) { path.moveTo(15, 5); path.lineTo(8, 12); path.lineTo(15, 19); canvas.drawLine(8, 12, 21, 12, paint); }
            else if (kind == LOCK) {
                canvas.drawRoundRect(5, 10, 19, 21, 2, 2, paint);
                canvas.drawArc(8, 3, 16, 17, 180, 180, false, paint);
                canvas.drawLine(12, 14, 12, 17, paint);
            } else if (kind == EXTERNAL) {
                path.moveTo(13, 4); path.lineTo(20, 4); path.lineTo(20, 11);
                path.moveTo(20, 4); path.lineTo(10, 14);
                path.moveTo(9, 5); path.lineTo(5, 5); path.lineTo(5, 20); path.lineTo(19, 20); path.lineTo(19, 15);
            } else { path.moveTo(5, 12); path.lineTo(10, 17); path.lineTo(19, 7); }
            canvas.drawPath(path, paint);
            canvas.restore();
        }
    }
}
