package org.shield.browser;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.mozilla.geckoview.*;
import java.io.*;
import java.util.*;

/** Independent interface, website-language and Gecko translation preferences. */
final class BrowserLanguages {
 static final String[] CODES={"system","en","he","ar","es","fr"};
 static final String[] NAMES={"System default","English","עברית","العربية","Español","Français"};
 static JSONObject dictionary;
 static Context wrap(Context base){String code=base.getSharedPreferences("browser",0).getString("app_language","system");if("system".equals(code))return base;Locale locale=Locale.forLanguageTag(code);Configuration config=new Configuration(base.getResources().getConfiguration());config.setLocales(new LocaleList(locale));config.setLayoutDirection(locale);return base.createConfigurationContext(config);}
 static String code(Context c){String code=c.getSharedPreferences("browser",0).getString("app_language","system");if("system".equals(code))code=c.getResources().getConfiguration().getLocales().get(0).getLanguage();return "iw".equals(code)?"he":code;}
 static String text(Context c,String value){if(value==null)return null;try{if(dictionary==null){try(InputStream in=c.getAssets().open("interface-languages.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);dictionary=new JSONObject(out.toString("UTF-8"));}}JSONObject language=dictionary.optJSONObject(code(c));return language==null?value:language.optString(value,value);}catch(Exception e){return value;}}
 static boolean rtl(Context c){return Arrays.asList("he","ar").contains(code(c));}
 static void direction(Context c,View v){v.setLayoutDirection(rtl(c)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);}
 static void localize(Context c,View v){if(v instanceof TextView&&!(v instanceof EditText)){TextView t=(TextView)v;t.setText(text(c,t.getText().toString()));}if(v.getContentDescription()!=null)v.setContentDescription(text(c,v.getContentDescription().toString()));if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)localize(c,g.getChildAt(i));}}
 static String appName(Context c){String code=c.getSharedPreferences("browser",0).getString("app_language","system");for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code))return NAMES[i];return NAMES[0];}
 static void appPicker(MainActivity a){String current=a.prefs.getString("app_language","system");int selected=Arrays.asList(CODES).indexOf(current);String[] names=NAMES.clone();names[0]=text(a,names[0]);new AlertDialog.Builder(a).setTitle(text(a,"App language")).setSingleChoiceItems(names,Math.max(0,selected),(dialog,which)->{a.prefs.edit().putString("app_language",CODES[which]).apply();a.save();dialog.dismiss();a.recreate();}).setNegativeButton(text(a,"Cancel"),null).show();}
 static ArrayList<String> preferred(Context c){ArrayList<String> result=new ArrayList<>();try{JSONArray list=new JSONArray(c.getSharedPreferences("browser",0).getString("website_languages","[]"));for(int i=0;i<Math.min(20,list.length());i++){String tag=Locale.forLanguageTag(list.optString(i)).toLanguageTag();if(!tag.equals("und")&&!result.contains(tag))result.add(tag);}}catch(Exception ignored){}if(result.isEmpty()){LocaleList locales=android.content.res.Resources.getSystem().getConfiguration().getLocales();for(int i=0;i<locales.size();i++){String tag=locales.get(i).toLanguageTag();if(!result.contains(tag))result.add(tag);}}if(result.isEmpty())result.add("en");return result;}
 static String label(String code){Locale l=Locale.forLanguageTag(code);return l.getDisplayName(l);}
 static void savePreferred(MainActivity a,ArrayList<String> list){a.prefs.edit().putString("website_languages",new JSONArray(list).toString()).apply();apply(a);}
 static void apply(MainActivity a){if(MainActivity.runtime!=null){MainActivity.runtime.getSettings().setLocales(preferred(a).toArray(new String[0]));MainActivity.runtime.getSettings().setTranslationsOfferPopup(a.prefs.getBoolean("offer_translate",true));}}
 interface Pick {void selected(String code);}
 static void picker(MainActivity a,String title,List<String> codes,Pick picked){String[] labels=new String[codes.size()];for(int i=0;i<labels.length;i++)labels[i]=label(codes.get(i));LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);EditText search=new EditText(a);search.setSingleLine();search.setHint(text(a,"Search languages"));search.setTextColor(a.ink());panel.addView(search);ListView list=new ListView(a);panel.addView(list,new LinearLayout.LayoutParams(-1,a.dp(360)));ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_list_item_1,new ArrayList<>(Arrays.asList(labels)));list.setAdapter(adapter);ArrayList<String> visible=new ArrayList<>(codes);search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(android.text.Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){String q=s.toString().toLowerCase(Locale.ROOT);visible.clear();adapter.clear();for(int i=0;i<codes.size();i++)if(labels[i].toLowerCase(Locale.ROOT).contains(q)||codes.get(i).toLowerCase(Locale.ROOT).contains(q)){visible.add(codes.get(i));adapter.add(labels[i]);}adapter.notifyDataSetChanged();}});AlertDialog dialog=new AlertDialog.Builder(a).setTitle(text(a,title)).setView(panel).setNegativeButton(text(a,"Cancel"),null).create();list.setOnItemClickListener((p,v,pos,id)->{String code=visible.get(pos);dialog.dismiss();picked.selected(code);});dialog.show();direction(a,panel);}
 static List<String> all(){TreeMap<String,String> sorted=new TreeMap<>();for(String language:Locale.getISOLanguages()){Locale l=Locale.forLanguageTag(language);sorted.put(l.getDisplayName(l)+language,language);}return new ArrayList<>(sorted.values());}
 static void addPreferred(MainActivity a,Runnable changed){ArrayList<String> current=preferred(a);ArrayList<String> available=new ArrayList<>(all());available.removeAll(current);picker(a,"Add language",available,code->{if(current.size()<20){current.add(code);savePreferred(a,current);}changed.run();});}
 static String target(MainActivity a){return a.prefs.getString("translate_target","en");}
 static void translationPicker(MainActivity a,boolean from,Pick pick){TranslationsController.RuntimeTranslation.listSupportedLanguages().accept(support->{ArrayList<String> codes=new ArrayList<>();for(TranslationsController.Language l:from?support.fromLanguages:support.toLanguages)codes.add(l.code);if(!a.isFinishing())picker(a,from?"Select language":"Translate into this language",codes,pick);},error->Toast.makeText(a,text(a,"Translation is unavailable on this device or connection."),Toast.LENGTH_LONG).show());}
 static void languageRule(MainActivity a,String code,String rule,Runnable updated){TranslationsController.RuntimeTranslation.setLanguageSettings(code,rule).accept(ignored->{a.prefs.edit().putString("translation_rule_"+code,rule).apply();updated.run();},error->Toast.makeText(a,text(a,"Could not save translation setting."),Toast.LENGTH_LONG).show());}
 static void bind(MainActivity a,MainActivity.Tab tab){tab.session.setTranslationsSessionDelegate(new TranslationsController.SessionTranslation.Delegate(){
  public void onTranslationStateChange(GeckoSession session,TranslationsController.SessionTranslation.TranslationState state){if(state.detectedLanguages!=null)tab.pageLanguage=state.detectedLanguages.docLangTag;if(state.error!=null&&!state.error.isEmpty())tab.translating=false;}
  public void onOfferTranslate(GeckoSession session){if(a.current()!=tab||!a.prefs.getBoolean("offer_translate",true)||tab.home||a.isFinishing())return;new AlertDialog.Builder(a).setTitle(text(a,"Translate page?")).setMessage(text(a,"Translation uses Gecko on this device. Language models may need to be downloaded.")).setPositiveButton(text(a,"Translate"),(d,w)->translate(a,tab)).setNegativeButton(text(a,"Not now"),null).show();}
  public void onExpectedTranslate(GeckoSession session){if(!tab.translating)translate(a,tab);}
 });}
 static void translate(MainActivity a,MainActivity.Tab tab){if(tab==null||tab.home)return;if(tab.pageLanguage==null||tab.pageLanguage.isEmpty()){Toast.makeText(a,text(a,"Wait for the page language to be detected."),Toast.LENGTH_LONG).show();return;}tab.translating=true;tab.session.getSessionTranslation().translate(tab.pageLanguage,target(a),new TranslationsController.SessionTranslation.TranslationOptions.Builder().downloadModel(true).build()).accept(v->{tab.translating=false;},error->{tab.translating=false;Toast.makeText(a,text(a,"Translation is unavailable on this device or connection."),Toast.LENGTH_LONG).show();});}
 private BrowserLanguages(){}
}
