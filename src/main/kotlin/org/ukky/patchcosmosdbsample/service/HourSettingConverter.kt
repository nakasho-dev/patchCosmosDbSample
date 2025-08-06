package org.ukky.patchcosmosdbsample.service

import org.springframework.stereotype.Service

@Service
class HourSettingConverter {

    /**
     * 2時間単位（1-12）から1時間単位（1-24）に変換する
     * 1 -> [1,2], 2 -> [3,4], 3 -> [5,6], ..., 12 -> [23,24]
     */
    fun convertHourSetting(originalHourSetting: List<Int>): List<Int> {
        return originalHourSetting.flatMap { twoHourUnit ->
            val startHour = (twoHourUnit - 1) * 2 + 1
            val endHour = startHour + 1
            listOf(startHour, endHour)
        }.sorted()
    }

    /**
     * 変換が必要かどうかを判定する
     * hour_setting_versionが存在しない、または1未満の場合は変換が必要
     */
    fun needsConversion(hourSettingVersion: Int?): Boolean {
        return hourSettingVersion == null || hourSettingVersion < 1
    }
}
