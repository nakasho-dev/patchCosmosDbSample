package org.ukky.patchcosmosdbsample.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UserItem(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("userId")
    val userId: String,

    @JsonProperty("app_setting")
    val appSetting: AppSetting
)

data class AppSetting(
    @JsonProperty("hour_setting")
    val hourSetting: List<Int>,

    @JsonProperty("hour_setting_version")
    val hourSettingVersion: Int? = null
)

data class PatchOperation(
    val op: String,
    val path: String,
    val value: Any
)
