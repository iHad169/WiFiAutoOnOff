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

public class ExpectedWiFiOnOff{
    public boolean isUsing;
    public boolean toWiFiOnOff;
    public int minute;
    public int hour;
    public boolean sunday;
    public boolean monday;
    public boolean tuesday;
    public boolean wednesday;
    public boolean thursday;
    public boolean friday;
    public boolean saturday;

    public ExpectedWiFiOnOff(boolean isUsing, boolean toWiFiOnOff, int minute, int hour, boolean sunday,
                             boolean monday, boolean tuesday, boolean wednesday,
                             boolean thursday, boolean friday, boolean saturday){
        this.isUsing = isUsing;
        this.toWiFiOnOff = toWiFiOnOff;
        this.minute = minute;
        this.hour = hour;
        this.sunday = sunday;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
    }

    private static class WeekDay{
        public final static int sunday      = 0;
        public final static int monday      = 1;
        public final static int tuesday     = 2;
        public final static int wednesday   = 3;
        public final static int thursday    = 4;
        public final static int friday      = 5;
        public final static int saturday    = 6;
    }

    public boolean isSelectWeekDay(int weekDay){
        switch (weekDay){
            case WeekDay.sunday:
                return sunday;
            case WeekDay.monday:
                return monday;
            case WeekDay.tuesday:
                return tuesday;
            case WeekDay.wednesday:
                return wednesday;
            case WeekDay.thursday:
                return thursday;
            case WeekDay.friday:
                return friday;
            case WeekDay.saturday:
                return saturday;
            default:
                return false;
        }
    }
}
