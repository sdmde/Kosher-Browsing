package org.shield.browser;

import android.app.Activity;
import android.app.DownloadManager;
import android.provider.DocumentsContract;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.webkit.URLUtil;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/** Saves the response Gecko already received. Never fetches a URL outside Gecko. */
public final class DownloadsController {
    private static final int CREATE_DOWNLOAD = 7301;
    private static final int BROWSE_DOWNLOADS = 7310;
    private static final String RECORDS = "downloads";
    private static final Object RECORD_LOCK = new Object();
    private final WeakReference<Activity> owner;
    private final Context context;
    private final SharedPreferences preferences;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Pending waiting; private GeckoSession sourceSession;

    private static final class Pending {
        final WebResponse response;
        final String name, mime;
        Pending(WebResponse value) {
            response = value;
            String type = header(value, "Content-Type").split(";", 2)[0].trim();
            mime = type.matches("[^\\s/]+/[^\\s/]+") ? type : "application/octet-stream";
            String guessed = URLUtil.guessFileName(value.uri, header(value, "Content-Disposition"), mime);
            guessed = guessed.replaceAll("[\\\\/\\p{Cntrl}:*?\"<>|]", "_").trim();
            name = guessed.isEmpty() || guessed.equals(".") || guessed.equals("..") ? "download" :
                (guessed.length() > 180 ? guessed.substring(0, 180) : guessed);
        }
    }

    public DownloadsController(Activity activity) {
        owner = new WeakReference<>(activity);
        context = activity.getApplicationContext();
        preferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE);
        DownloadService.reconcile(context);
    }

    /** Call from the session ContentDelegate's onExternalResponse callback. */
    public void onExternalResponse(GeckoSession session, WebResponse response) {
        Uri source = Uri.parse(response.uri);
        Activity activity = activity();
        if (activity == null || response.body == null ||
                !("https".equalsIgnoreCase(source.getScheme()) || "http".equalsIgnoreCase(source.getScheme()))) {
            closeStream(response.body);
            toast("This download is not supported.");
            return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
            closeStream(response.body);
            toast("The website could not provide this download. Please try again.");
            return;
        }
        if (waiting != null) {
            closeStream(response.body);
            toast("Finish choosing a location for the current download first.");
            return;
        }
        sourceSession=session; Pending pending = new Pending(response);
        if(DownloadPolicy.blocked(pending.name,pending.mime,response.uri,header(response,"Content-Disposition"))){closeStream(response.body);toast("APK downloads are blocked.");return;}
        waiting = pending;
        new AlertDialog.Builder(activity).setTitle("Download file?").setMessage(pending.name)
            .setPositiveButton("Save", (dialog, which) -> {
                if (waiting != pending) return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    waiting = null;
                    save(pending, null);
                } else {
                    Intent choose = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE).setType(pending.mime)
                        .putExtra(Intent.EXTRA_TITLE, pending.name)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    try { activity.startActivityForResult(choose, CREATE_DOWNLOAD); }
                    catch (ActivityNotFoundException e) { cancelWaiting(); toast("No file picker is available."); }
                }
            }).setNegativeButton("Cancel", (dialog, which) -> cancelWaiting())
            .setOnCancelListener(dialog -> cancelWaiting()).show();
    }

    /** Forward MainActivity.onActivityResult here before calling super. */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==BROWSE_DOWNLOADS){
            if(resultCode==Activity.RESULT_OK&&data!=null&&data.getData()!=null){
                Uri uri=data.getData();String mime=context.getContentResolver().getType(uri);
                if(DownloadPolicy.blocked(uri.toString(),mime)){toast("APK files are blocked.");return true;}
                Activity a=activity();if(a!=null)try{a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime==null?"application/octet-stream":mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(ActivityNotFoundException|SecurityException e){toast("No app can open this file.");}
            }
            return true;
        }
        if (requestCode != CREATE_DOWNLOAD) return false;
        Pending pending = waiting;
        waiting = null;
        if (pending == null) return true;
        Uri destination = data == null ? null : data.getData();
        if (resultCode != Activity.RESULT_OK || destination == null) {
            closeStream(pending.response.body);
            return true;
        }
        try {
            boolean read=(data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0;
            boolean write=(data.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION)!=0;
            if(read&&write)context.getContentResolver().takePersistableUriPermission(destination,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            else if(read)context.getContentResolver().takePersistableUriPermission(destination,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            else if(write)context.getContentResolver().takePersistableUriPermission(destination,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) { /* Some providers only offer temporary access. */ }
        save(pending, destination);
        return true;
    }

    private void save(Pending pending, Uri selectedDestination) {
        Activity a=activity();
        if(a!=null && Build.VERSION.SDK_INT>=33 && a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},7302);
        DownloadService.enqueue(context,sourceSession,pending.response,pending.name,pending.mime,selectedDestination);
        toast("Download started. Keep the browser open; use Download transfers to pause or resume.");
    }

    /** Open the device's file manager at Downloads without broad storage access. */
    public void showDownloads() {
        Activity a=activity();if(a==null)return;
        try{a.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));return;}catch(ActivityNotFoundException|SecurityException ignored){}
        Uri downloads=Uri.parse("content://com.android.providers.downloads.documents/root/downloads");
        try{a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(downloads,DocumentsContract.Document.MIME_TYPE_DIR).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));return;}catch(ActivityNotFoundException|SecurityException ignored){}
        try{Intent browse=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").putExtra(DocumentsContract.EXTRA_INITIAL_URI,downloads);a.startActivityForResult(browse,BROWSE_DOWNLOADS);}catch(ActivityNotFoundException|SecurityException e){toast("No file manager is available. Open Downloads from your device's Files app.");}
    }

    public void showTransfers() {
        Activity activity = activity();
        if (activity == null) return;
        ArrayList<JSONObject> items = new ArrayList<>();
        synchronized (RECORD_LOCK) {
            JSONArray saved = records();
            for (int i = 0; i < saved.length(); i++) if (saved.optJSONObject(i) != null) items.add(saved.optJSONObject(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle("Downloads")
            .setNegativeButton("Done", null);
        if (items.isEmpty()) {
            builder.setMessage("No downloads yet. Files you download from websites will appear here.");
        } else {
            String[] labels = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.get(i);
                labels[i] = item.optString("name", "Download") + "\n" + item.optString("status") + " · " +
                    Formatter.formatShortFileSize(context, item.optLong("bytes")) + "\n" +
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(item.optLong("time")));
            }
            builder.setItems(labels, (dialog, which) -> {
                JSONObject item = items.get(which);
                if (!"Complete".equals(item.optString("status"))) {
                    boolean saving="Saving".equals(item.optString("status"));
                    new AlertDialog.Builder(activity).setTitle(item.optString("name")).setMessage(item.optString("status")+"\nResume restarts from zero when the server does not support verified byte ranges.")
                        .setPositiveButton(saving?"Pause":"Resume",(d,w)->{DownloadService.action(context,saving?"pause":"resume",item.optString("id"));toast(saving?"Pausing download":"Resuming download");})
                        .setNegativeButton("Done",null).show();
                    return;
                }
                Uri uri = Uri.parse(item.optString("uri"));
                Intent open = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, item.optString("mime", "application/octet-stream"))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { activity.startActivity(Intent.createChooser(open, "Open download")); }
                catch (ActivityNotFoundException | SecurityException e) { toast("The file was moved, or no app can open this file type."); }
            });
        }
        builder.show();
    }

    /** Releases any stream waiting for a confirmation or system file picker. */
    public void close() { cancelWaiting(); owner.clear(); }

    private void cancelWaiting() {
        if (waiting != null) { closeStream(waiting.response.body); waiting = null; }
    }

    private JSONArray records() {
        try { return new JSONArray(preferences.getString(RECORDS, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private Activity activity() {
        Activity activity = owner.get();
        return activity == null || activity.isFinishing() || activity.isDestroyed() ? null : activity;
    }

    private void toast(String message) {
        main.post(() -> { Activity activity = activity(); if (activity != null) Toast.makeText(activity, message, Toast.LENGTH_LONG).show(); });
    }

    private static void closeStream(InputStream stream) {
        if (stream != null) try { stream.close(); } catch (IOException ignored) { }
    }

    private static String header(WebResponse response, String name) {
        for (Map.Entry<String, String> entry : response.headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) return entry.getValue() == null ? "" : entry.getValue();
        }
        return "";
    }
}
