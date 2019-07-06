/*
 * WiFiAutoOnOff is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WiFiAutoOnOff is distributed in the hope that it will be useful,
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WiFiAutoOnOff.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.sourcekey.wifiautoonoff;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences saver;
    private ArrayList<ExpectedWiFiOnOff> expectedWiFiOnOffList = new ArrayList<>();
    private BaseAdapter baseAdapter;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saver = getSharedPreferences("WiFiAutoOnOff", 0);
        //Toast.makeText(this, String.valueOf(checkAgreeUseConditions()), Toast.LENGTH_LONG);
        if(checkAgreeUseConditions()){
            Log.e("xx", "t");
            startUpApplication();
        }else{
            Log.e("xx", "f");
            showUseConditions();
        }
    }

    /**
     * 撿查使用者是否同意使用條款
     */
    private boolean checkAgreeUseConditions(){
        //讀取可能己同意使用條款值
        try{
            return saver.getBoolean("isAgreeUseConditions", false);
        }catch (Exception e){
            //如果讀取錯誤, 就重新設定為未同意使用條款
            saver.edit().putBoolean("isAgreeUseConditions", false);
            return false;
        }
    }

    /**
     * 顯示使用條款
     */
    private void showUseConditions(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View useConditionsDialogLayout = getLayoutInflater().inflate(R.layout.use_conditions_dialog_layout, null);
        WebView webView = useConditionsDialogLayout.findViewById(R.id.webView);
        webView.loadUrl("https://docs.google.com/document/d/1OASjyxZtXt73WxHQsInYTz7YnHDi6C08lwfqKAMcQ1s/edit#heading=h.c3gz7r4kfhee");
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
        builder.setPositiveButton("Agree", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                saver.edit().putBoolean("isAgreeUseConditions", true);
                startUpApplication();
            }
        }).setNeutralButton("Disagree", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                saver.edit().putBoolean("isAgreeUseConditions", false);
                finish();
                System.exit(0);
            }
        });
        builder.setView(useConditionsDialogLayout);
        builder.create().show();
    }

    /**
     * 啟動應用程式
     */
    private void startUpApplication(){
        readExpectedWiFiOnOffList();
        setViews();
    }

    /**
     * 讀取己儲存WiFi自動開關設定
     */
    private void readExpectedWiFiOnOffList(){
        try{
            //讀取己儲存WiFi自動開關設定
            expectedWiFiOnOffList = new Gson().fromJson(
                    saver.getString("ExpectedWiFiOnOffList", ""),
                    new TypeToken<List<ExpectedWiFiOnOff>>() {
                    }.getType()
            );
            //Saver有可能出問題, 所以要去攞個value試下
            if(0 < expectedWiFiOnOffList.size()){
                boolean test = expectedWiFiOnOffList.get(0).isUsing;
            }
        }catch (Exception e){
            //初始化
            expectedWiFiOnOffList = new ArrayList<>();
            Time timeNow = new Time(Time.getCurrentTimezone());
            timeNow.setToNow();
            expectedWiFiOnOffList.add(new ExpectedWiFiOnOff(
                    true,
                    (expectedWiFiOnOffList.size() > 0) ? !(expectedWiFiOnOffList.get(expectedWiFiOnOffList.size() - 1).toWiFiOnOff) : true,//同前個相反
                    timeNow.minute, timeNow.hour, timeNow.weekDay == 0,
                    timeNow.weekDay == 1, timeNow.weekDay == 2,
                    timeNow.weekDay == 3, timeNow.weekDay == 4,
                    timeNow.weekDay == 5, timeNow.weekDay == 6
            ));
            shortExpectedWiFiOnOffList();
            updateDistplay();
        }
    }

    /**
     * 設定介面物件
     */
    private void setViews(){
        ListView item_list = (ListView) findViewById(R.id.item_list);
        baseAdapter = new ListItemGroup(this);
        item_list.setAdapter(baseAdapter);
        FloatingActionButton addExpectedWiFiOnOffFloatingActionButton = (FloatingActionButton) findViewById(R.id.addExpectedWiFiOnOffFloatingActionButton);
        addExpectedWiFiOnOffFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Time timeNow = new Time(Time.getCurrentTimezone());
                timeNow.setToNow();
                expectedWiFiOnOffList.add(new ExpectedWiFiOnOff(
                        true,
                        (expectedWiFiOnOffList.size() > 0) ? !(expectedWiFiOnOffList.get(expectedWiFiOnOffList.size() - 1).toWiFiOnOff) : true,//同前個相反
                        timeNow.minute, timeNow.hour, timeNow.weekDay == 0,
                        timeNow.weekDay == 1, timeNow.weekDay == 2,
                        timeNow.weekDay == 3, timeNow.weekDay == 4,
                        timeNow.weekDay == 5, timeNow.weekDay == 6
                ));
                shortExpectedWiFiOnOffList();
                updateDistplay();
                baseAdapter.notifyDataSetInvalidated();
            }
        });
        setAds();
    }

    /**
     * 設定廣告
     */
    private void setAds(){
        MobileAds.initialize(this, "ca-app-pub-2319576034906153~1853366597");

        //橫幅廣告
        mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build());

        //插頁式廣告
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-2319576034906153/3504062444");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                //Toast.makeText(MainActivity.this, "onAdLoaded()", Toast.LENGTH_SHORT).show();
                if(mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                //Toast.makeText(MainActivity.this, "onAdFailedToLoad()", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 每一項有同樣嘅ViewGroup
     */
    private class ListItemGroup extends BaseAdapter {
        private Context context;
        private LayoutInflater layoutInflater;

        public ListItemGroup(Context context) {
            this.context = context;
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * 此適配器表示的數據集中有多少項。
         *
         * @return 項目計數。
         */
        @Override
        public int getCount() {
            return expectedWiFiOnOffList.size();//imageSrc.length;
        }

        /**
         * 獲取與數據集中指定位置相關聯的數據項。
         *
         * @param position 我們想要的數據在適配器的數據集中的位置。
         * @return 指定位置的數據。
         */
        @Override
        public Object getItem(int position) {
            return position;
        }

        /**
         * 獲取與列表中指定位置相關聯的行標識。
         *
         * @param position 項目在適配器的數據集中的位置，其行ID為我們想要的。
         * @return 在指定位置的項目的ID。
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * 獲取在數據集中指定位置顯示數據的視圖。 您可以手動創建視圖，也可以從XML佈局文件中對其進行擴充。
         * 當視圖膨脹時，父視圖（GridView，ListView ...）將應用默認佈局參數，除非您使用
         * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
         * 以指定根視圖並防止附加到根。
         *
         * @param position    項目在我們想要的視圖的項目的適配器的數據集內的位置。
         * @param convertView 舊視圖重用，如果可能的話。
         *                    注意：在使用之前，應該檢查此視圖是否為非空值並且是合適的類型。
         *                    如果無法將此視圖轉換為顯示正確的數據，則此方法可以創建新視圖。
         *                    異構列表可以指定其視圖類型的數量，以便此視圖始終是正確的類型
         *                    (see {@link #getViewTypeCount()} 和
         *                    {@link #getItemViewType(int)}).
         * @param parent      此視圖最終將附加到的父級
         * @return 與指定位置處的數據相對應的View.
         */
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            @SuppressLint("ViewHolder") ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(R.layout.list_item_group_layout, null);
            Switch isUsingSwitch = (Switch) viewGroup.findViewById(R.id.isUsingSwitch);
            ImageView deleteImageView = (ImageView) viewGroup.findViewById(R.id.deleteImageView);
            ToggleButton toggleButtonWiFiOnOff = (ToggleButton) viewGroup.findViewById(R.id.toggleButtonWiFiOnOff);
            final TextView textViewTine = (TextView) viewGroup.findViewById(R.id.textViewTine);
            CheckBox checkBoxSunday = (CheckBox) viewGroup.findViewById(R.id.checkBoxSunday);
            CheckBox checkBoxMonday = (CheckBox) viewGroup.findViewById(R.id.checkBoxMonday);
            CheckBox checkBoxTuesday = (CheckBox) viewGroup.findViewById(R.id.checkBoxTuesday);
            CheckBox checkBoxWednesday = (CheckBox) viewGroup.findViewById(R.id.checkBoxWednesday);
            CheckBox checkBoxThursday = (CheckBox) viewGroup.findViewById(R.id.checkBoxThursday);
            CheckBox checkBoxFriday = (CheckBox) viewGroup.findViewById(R.id.checkBoxFriday);
            CheckBox checkBoxSaturday = (CheckBox) viewGroup.findViewById(R.id.checkBoxSaturday);

            isUsingSwitch.setChecked(expectedWiFiOnOffList.get(position).isUsing);
            toggleButtonWiFiOnOff.setChecked(expectedWiFiOnOffList.get(position).toWiFiOnOff);
            textViewTine.setText(String.format("%02d", expectedWiFiOnOffList.get(position).hour) + ":" + String.format("%02d", expectedWiFiOnOffList.get(position).minute));
            checkBoxSunday.setChecked(expectedWiFiOnOffList.get(position).sunday);
            checkBoxMonday.setChecked(expectedWiFiOnOffList.get(position).monday);
            checkBoxTuesday.setChecked(expectedWiFiOnOffList.get(position).tuesday);
            checkBoxWednesday.setChecked(expectedWiFiOnOffList.get(position).wednesday);
            checkBoxThursday.setChecked(expectedWiFiOnOffList.get(position).thursday);
            checkBoxFriday.setChecked(expectedWiFiOnOffList.get(position).friday);
            checkBoxSaturday.setChecked(expectedWiFiOnOffList.get(position).saturday);

            viewGroup.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showDeleteExpectedWiFiOnOff(position);
                    return true;
                }
            });
            isUsingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).isUsing = isChecked;
                    updateDistplay();
                }
            });
            deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteExpectedWiFiOnOff(position);
                }
            });
            toggleButtonWiFiOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).toWiFiOnOff = isChecked;
                    updateDistplay();
                }
            });
            textViewTine.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    View timeSelectDialogLayout = getLayoutInflater().inflate(R.layout.time_select_dialog_layout, null);
                    final TimePicker timePicker = timeSelectDialogLayout.findViewById(R.id.timePicker);
                    timePicker.setCurrentHour(expectedWiFiOnOffList.get(position).hour);
                    timePicker.setCurrentMinute(expectedWiFiOnOffList.get(position).minute);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            textViewTine.setText(String.format("%02d", timePicker.getCurrentHour()) + ":" + String.format("%02d", timePicker.getCurrentMinute()));
                            ExpectedWiFiOnOff expectedWiFiOnOff = expectedWiFiOnOffList.get(position);
                            expectedWiFiOnOff.hour = timePicker.getCurrentHour();
                            expectedWiFiOnOff.minute = timePicker.getCurrentMinute();
                            shortExpectedWiFiOnOffList();
                            updateDistplay();
                            baseAdapter.notifyDataSetInvalidated();
                            mInterstitialAd.loadAd(new AdRequest.Builder().build());//顯示廣告
                        }
                    }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() { // define the 'Cancel' button
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {

                        }
                    });
                    builder.setView(timeSelectDialogLayout);
                    builder.create().show();
                }
            });
            checkBoxSunday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).sunday = isChecked;
                    updateDistplay();
                }
            });
            checkBoxMonday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).monday = isChecked;
                    updateDistplay();
                }
            });
            checkBoxTuesday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).tuesday = isChecked;
                    updateDistplay();
                }
            });
            checkBoxWednesday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).wednesday = isChecked;
                    updateDistplay();
                }
            });
            checkBoxThursday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).thursday = isChecked;
                    updateDistplay();
                }
            });
            checkBoxFriday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).friday = isChecked;
                    updateDistplay();
                }
            });
            checkBoxSaturday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    expectedWiFiOnOffList.get(position).saturday = isChecked;
                    updateDistplay();
                }
            });

            return viewGroup;
        }

        private void showDeleteExpectedWiFiOnOff(final int position){
            //顯示對話框界用戶確定刪除呢個ExpectedWiFiOnOff
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("You want to DELETE?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            expectedWiFiOnOffList.remove(position);
                            updateDistplay();
                            baseAdapter.notifyDataSetInvalidated();
                            mInterstitialAd.loadAd(new AdRequest.Builder().build());//顯示廣告
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            builder.create().show();
        }
    }

    /**
     *  更新顯示
     * */
    private void updateDistplay(){
        //儲存WiFi自動開關設定
        saver.edit().putString("ExpectedWiFiOnOffList", new Gson().toJson(expectedWiFiOnOffList)).commit();

        setAllWiFiOnOffBroadcast();
    }

    /**
     *  將ExpectedWiFiOnOffList按時間排序
     * */
    private void shortExpectedWiFiOnOffList(){
        ExpectedWiFiOnOff expectedWiFiOnOffTemporaryStorage;
        for(int i = 0; i<expectedWiFiOnOffList.size(); i++){
            for(int j = i+1; j<expectedWiFiOnOffList.size(); j++){
                if(isExpectedWiFiOnOff1Bigger(expectedWiFiOnOffList.get(i), expectedWiFiOnOffList.get(j))){
                    expectedWiFiOnOffTemporaryStorage = expectedWiFiOnOffList.get(i);
                    expectedWiFiOnOffList.set(i, expectedWiFiOnOffList.get(j));
                    expectedWiFiOnOffList.set(j, expectedWiFiOnOffTemporaryStorage);
                }
            }
        }
    }

    /**
     * 比較第一個ExpectedWiFiOnOff同第二個ExpectedWiFiOnOff
     * 如果第一個大過第二個就出 true 否則出 false
     * */
    private boolean isExpectedWiFiOnOff1Bigger(ExpectedWiFiOnOff expectedWiFiOnOff1, ExpectedWiFiOnOff expectedWiFiOnOff2){
        Calendar expectedWiFiOnOff1Calendar = Calendar.getInstance();
        expectedWiFiOnOff1Calendar.set(Calendar.HOUR_OF_DAY, expectedWiFiOnOff1.hour);
        expectedWiFiOnOff1Calendar.set(Calendar.MINUTE, expectedWiFiOnOff1.minute);
        Calendar expectedWiFiOnOff2Calendar = Calendar.getInstance();
        expectedWiFiOnOff2Calendar.set(Calendar.HOUR_OF_DAY, expectedWiFiOnOff2.hour);
        expectedWiFiOnOff2Calendar.set(Calendar.MINUTE, expectedWiFiOnOff2.minute);

        return expectedWiFiOnOff1Calendar.getTimeInMillis()>expectedWiFiOnOff2Calendar.getTimeInMillis();
    }

    /**
     * 設定所有開關WiFi廣播
     */
    private void setAllWiFiOnOffBroadcast() {
        //Time timeNow = new Time(Time.getCurrentTimezone());
        //timeNow.setToNow();
        removeAllWiFiOnOffBroadcast();
        int alarmManagerCount = 0;
        for (ExpectedWiFiOnOff expectedWiFiOnOff : expectedWiFiOnOffList) {
            for (int weekDay = 0; weekDay <= 6; weekDay++) {
                if (expectedWiFiOnOff.isSelectWeekDay(weekDay)) {
                    setWiFiOnOffBroadcast(this, expectedWiFiOnOff, alarmManagerCount,
                            weekDay, expectedWiFiOnOff.hour, expectedWiFiOnOff.minute, 0,
                            expectedWiFiOnOff.toWiFiOnOff
                    );
                    alarmManagerCount++;
                }
            }
        }
    }

    /**
     * 刪除所有開關WiFi廣播
     */
    private void removeAllWiFiOnOffBroadcast() {
        int alarmManagerCount = 0;
        for (ExpectedWiFiOnOff expectedWiFiOnOff : expectedWiFiOnOffList) {
            for (int weekDay = 0; weekDay <= 6; weekDay++) {
                if (expectedWiFiOnOff.isSelectWeekDay(weekDay)) {
                    Intent intent = new Intent(this, WiFiAutoOnOffReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmManagerCount, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    alarmManager.cancel(pendingIntent);
                    alarmManagerCount++;
                }
            }
        }
        //刪除己刪除既alarmManagerCount
        for (int i = 0; i < 14; i++) {
            Intent intent = new Intent(this, WiFiAutoOnOffReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmManagerCount, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            alarmManagerCount++;
        }
    }

    /**
     * 設定開關WiFi時間
     * <p>
     * 注意:如果設定已過去嘅時間會即時執行WiFiAutoOnOffReceiver
     * 已過去時間為 星期日凌晨00:00打後
     * 即如果依家為星期二13:00去設定下個星期一19:00
     * 就會即時執行WiFiAutoOnOffReceiver
     */
    private void setWiFiOnOffBroadcast(Context context, ExpectedWiFiOnOff expectedWiFiOnOff, int alarmManagerCount, int weekDay, int hour, int minute, int second, boolean toOnOff) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, weekDay + 1);//設定星期幾響
        calendar.set(Calendar.HOUR_OF_DAY, hour);//設定幾點響
        calendar.set(Calendar.MINUTE, minute);//設定幾多分鐘響
        calendar.set(Calendar.SECOND, second);//設定第幾秒響

        Intent intent = new Intent(context, WiFiAutoOnOffReceiver.class);
        intent.putExtra("setWiFiOnOff", toOnOff);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmManagerCount, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
    }

}
