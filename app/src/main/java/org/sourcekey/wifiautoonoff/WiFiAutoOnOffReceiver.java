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


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.text.format.Time;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.*;

public class WiFiAutoOnOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            //檢查呢次廣播係米boot機時發出
            String action = intent.getAction();
            if(action == "android.intent.action.BOOT_COMPLETED") {
                readExpectedWiFiOnOffListInStartUp(context);
            } else {
                wiFiOnOff(context, intent);
            }
        }
    }

    /**
     * 當開機時讀取己儲存WiFi自動開關設定程序
     */
    private void readExpectedWiFiOnOffListInStartUp(Context context){
        try{
            //讀取己儲存WiFi自動開關設定
            SharedPreferences saver = context.getSharedPreferences("WiFiAutoOnOff", 0);
            ArrayList<ExpectedWiFiOnOff> expectedWiFiOnOffList = new Gson().fromJson(
                    saver.getString("ExpectedWiFiOnOffList", ""),
                    new TypeToken<List<ExpectedWiFiOnOff>>() {
                    }.getType()
            );
            boolean test = expectedWiFiOnOffList.get(0).isUsing;//Saver有可能出問題, 所以要去攞個value試下

            //設置己儲存WiFi自動開關設定
            int alarmManagerCount = 0;
            for (ExpectedWiFiOnOff expectedWiFiOnOff : expectedWiFiOnOffList) {
                for (int weekDay = 0; weekDay <= 6; weekDay++) {
                    if (expectedWiFiOnOff.isSelectWeekDay(weekDay)) {
                        setWiFiOnOffBroadcast(context, expectedWiFiOnOff, alarmManagerCount,
                                weekDay, expectedWiFiOnOff.hour, expectedWiFiOnOff.minute, 0,
                                expectedWiFiOnOff.toWiFiOnOff
                        );
                        alarmManagerCount++;
                    }
                }
            }
        }catch (Exception e){ }
    }

    /**
     * WiFi開關
     */
    private void wiFiOnOff(Context context, Intent intent){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(intent.getBooleanExtra("setWiFiOnOff", true));
        //ExpectedWiFiOnOff expectedWiFiOnOff = new Gson().fromJson(intent.getStringExtra("setWiFiOnOff"), ExpectedWiFiOnOff.class);
        //Toast.makeText(context, "123:" + expectedWiFiOnOff.hour + " " + expectedWiFiOnOff.minute + " " + expectedWiFiOnOff.toWiFiOnOff, Toast.LENGTH_SHORT).show();
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
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmManagerCount, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
    }

}