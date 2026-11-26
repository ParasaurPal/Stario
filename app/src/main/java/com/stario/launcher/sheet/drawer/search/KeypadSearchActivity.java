package com.stario.launcher.sheet.drawer.search;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.stario.launcher.R;
import com.stario.launcher.themes.ThemedActivity;
import java.util.*;

public class KeypadSearchActivity extends ThemedActivity {
    private static final int CONTACTS_REQUEST = 4812;
    private static final String[] DIGITS = {"0","1","ABC","DEF","GHI","JKL","MNO","PQRS","TUV","WXYZ"};
    private EditText query;
    private LinearLayout results;
    private final List<Result> all = new ArrayList<>();
    private final List<Result> shortcuts = new ArrayList<>();
    private static class Result {
        String title, subtitle, digits;
        Runnable action;
        Result(String t, String s, Runnable a) {
            title=t; subtitle=s; action=a; digits=toDigits(t+" "+s);
        }
    }
    private static String toDigits(String value) {
        StringBuilder out=new StringBuilder();
        for(char c:value.toUpperCase(Locale.ROOT).toCharArray()) {
            if(c>='A'&&c<='C') out.append('2');
            else if(c>='D'&&c<='F') out.append('3');
            else if(c>='G'&&c<='I') out.append('4');
            else if(c>='J'&&c<='L') out.append('5');
            else if(c>='M'&&c<='O') out.append('6');
            else if(c>='P'&&c<='S') out.append('7');
            else if(c>='T'&&c<='V') out.append('8');
            else if(c>='W'&&c<='Z') out.append('9');
            else if(Character.isDigit(c)) out.append(c);
        }
        return out.toString();
    }
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(12));
        root.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface));
        TextView heading=new TextView(this); heading.setText("Keypad search"); heading.setTextSize(25);
        heading.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        root.addView(heading);
        query=new EditText(this); query.setSingleLine(true); query.setTextSize(25);
        query.setHint("Enter a name or number"); query.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        query.setShowSoftInputOnFocus(false); query.setPadding(dp(12),dp(12),dp(12),dp(12));
        root.addView(query,new LinearLayout.LayoutParams(-1,dp(58)));
        ScrollView scroll=new ScrollView(this); results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        GridLayout pad=new GridLayout(this); pad.setColumnCount(3);
        for(int i=1;i<=9;i++) addKey(pad,i);
        addKey(pad,0); addKey(pad,-1); addKey(pad,-2);
        root.addView(pad,new LinearLayout.LayoutParams(-1,dp(228)));
        setContentView(root);
        query.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){filter(s.toString());}
            public void afterTextChanged(android.text.Editable e){}
        });
        loadApps(); loadSettings(); loadShortcuts();
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED) loadContacts();
        else ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS},CONTACTS_REQUEST);
        filter("");
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private int resolveColor(int attr){android.util.TypedValue v=new android.util.TypedValue();getTheme().resolveAttribute(attr,v,true);return v.data;}
    private void addKey(GridLayout grid,int value){
        LinearLayout cell=new LinearLayout(this);cell.setGravity(Gravity.CENTER);cell.setOrientation(LinearLayout.VERTICAL);
        TextView number=new TextView(this); number.setText(value==-1?"⌫":value==-2?"×":String.valueOf(value)); number.setTextSize(27);
        number.setGravity(Gravity.CENTER); number.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        cell.addView(number);
        if(value>=2){TextView letters=new TextView(this);letters.setText(DIGITS[value]);letters.setTextSize(10);letters.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));cell.addView(letters);}
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(56);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(cell,p);
        cell.setOnClickListener(v->{if(value==-1){int n=query.length();if(n>0)query.getText().delete(n-1,n);}else if(value==-2)query.setText("");else query.append(String.valueOf(value));});
    }
    private void loadApps(){
        PackageManager pm=getPackageManager();Intent main=new Intent(Intent.ACTION_MAIN,null);main.addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo ri:pm.queryIntentActivities(main,0)){
            String label=ri.loadLabel(pm).toString();Intent launch=pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
            if(launch!=null) all.add(new Result(label,"App",()->startActivity(launch)));
        }
    }
    private void loadSettings(){
        addSetting("Wi-Fi","Wireless and network settings",Settings.ACTION_WIFI_SETTINGS);
        addSetting("Bluetooth","Connected devices",Settings.ACTION_BLUETOOTH_SETTINGS);
        addSetting("Display","Brightness, screen and display",Settings.ACTION_DISPLAY_SETTINGS);
        addSetting("Sound","Volume and sound",Settings.ACTION_SOUND_SETTINGS);
        addSetting("Battery","Battery and power",Intent.ACTION_POWER_USAGE_SUMMARY);
        addSetting("Storage","Storage space",Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        addSetting("Apps","Installed applications",Settings.ACTION_APPLICATION_SETTINGS);
        addSetting("Notifications","Notification settings",Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        addSetting("Location","Location services",Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        addSetting("Security","Security and privacy",Settings.ACTION_SECURITY_SETTINGS);
        addSetting("Accessibility","Accessibility settings",Settings.ACTION_ACCESSIBILITY_SETTINGS);
        addSetting("Language","Language and input",Settings.ACTION_INPUT_METHOD_SETTINGS);
        addSetting("Date and time","Clock and date settings",Settings.ACTION_DATE_SETTINGS);
        addSetting("Wallpaper","Wallpaper settings",Intent.ACTION_SET_WALLPAPER);
        addSetting("Default apps","Default application settings",Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
        addSetting("System","System settings",Settings.ACTION_SETTINGS);
    }
    private void addSetting(String name,String sub,String action){
        all.add(new Result(name,"Setting · "+sub,()->{try{startActivity(new Intent(action));}catch(Exception ignored){startActivity(new Intent(Settings.ACTION_SETTINGS));}}));
    }
    private void loadContacts(){
        try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},
                null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE")){
            if(c!=null)while(c.moveToNext()){
                String name=c.getString(0),number=c.getString(1);
                if(name!=null){Intent i=new Intent(Intent.ACTION_VIEW,Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                    getContactId(name)));all.add(new Result(name,number,()->{Intent call=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(number)));startActivity(call);}));}
            }
        }catch(Exception ignored){}
    }
    private String getContactId(String name){
        try(Cursor c=getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,new String[]{ContactsContract.Contacts._ID},
            ContactsContract.Contacts.DISPLAY_NAME+"=?",new String[]{name},null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}
        return "0";
    }
    private void loadShortcuts(){
        if(Build.VERSION.SDK_INT<25)return;
        try{
            LauncherApps launcher=(LauncherApps)getSystemService(LAUNCHER_APPS_SERVICE);
            LauncherApps.ShortcutQuery q=new LauncherApps.ShortcutQuery().setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC|LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST|LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<android.content.pm.ShortcutInfo> list=launcher.getShortcuts(q,android.os.Process.myUserHandle());
            if(list!=null)for(android.content.pm.ShortcutInfo si:list){
                String label=si.getShortLabel()==null?si.getId():si.getShortLabel().toString();
                shortcuts.add(new Result(label,"Shortcut · "+si.getPackage(),()->launcher.startShortcut(si.getPackage(),si.getId(),null,null,Process.myUserHandle())));
            }
            all.addAll(shortcuts);
        }catch(Exception ignored){}
    }
    private void filter(String input){
        results.removeAllViews();String q=input.trim();String digits=toDigits(q);
        if(q.isEmpty()){TextView hint=new TextView(this);hint.setText("Search apps, contacts, settings and shortcuts");hint.setPadding(8,dp(18),8,dp(18));results.addView(hint);return;}
        int count=0;
        for(Result r:all){
            boolean match=r.title.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))||
                (!digits.isEmpty()&&r.digits.startsWith(digits))||
                r.subtitle.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));
            if(match){addResult(r);if(++count>=50)break;}
        }
        if(count==0){TextView empty=new TextView(this);empty.setText("No matches");empty.setPadding(8,dp(20),8,dp(20));results.addView(empty);}
    }
    private void addResult(Result r){
        TextView row=new TextView(this);row.setText(r.title+"\n"+r.subtitle);row.setTextSize(16);
        row.setPadding(dp(12),dp(12),dp(12),dp(12));row.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        results.addView(row,new LinearLayout.LayoutParams(-1,-2));View line=new View(this);line.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorOutlineVariant));results.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        row.setOnClickListener(v->{try{r.action.run();}catch(Exception ignored){}});
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==CONTACTS_REQUEST&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED){loadContacts();filter(query.getText().toString());}
    }
}