package org.ukky.patchcosmosdbsample.service

import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.models.CosmosPatchOperations
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.ukky.patchcosmosdbsample.model.UserItem
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class CosmosDbService(
    private val cosmosAsyncContainer: CosmosAsyncContainer,
    private val objectMapper: ObjectMapper,
    private val hourSettingConverter: HourSettingConverter
) {

    private val logger = LoggerFactory.getLogger(CosmosDbService::class.java)

    /**
     * 変換が必要なユーザーアイテムを取得する
     */
    fun getItemsNeedingConversion(): Flux<UserItem> {
        val query = "SELECT * FROM c WHERE NOT IS_DEFINED(c.app_setting.hour_setting_version) OR c.app_setting.hour_setting_version < 1"
        val queryOptions = CosmosQueryRequestOptions()

        return cosmosAsyncContainer.queryItems(query, queryOptions, UserItem::class.java)
            .byPage()
            .flatMap { page -> Flux.fromIterable(page.results) }
    }

    /**
     * PATCH操作でアイテムを更新する
     */
    fun patchUserItem(userItem: UserItem): Mono<Void> {
        return try {
            val convertedHourSetting = hourSettingConverter.convertHourSetting(userItem.appSetting.hourSetting)

            val patchOperations = CosmosPatchOperations.create()
                .replace("/app_setting/hour_setting", convertedHourSetting)
                .add("/app_setting/hour_setting_version", 1)

            cosmosAsyncContainer.patchItem(
                userItem.id,
                PartitionKey(userItem.userId),
                patchOperations,
                UserItem::class.java
            ).then()
                .doOnSuccess {
                    logger.info("Successfully patched user item: ${userItem.id}")
                }
                .doOnError { error ->
                    logger.error("Failed to patch user item: ${userItem.id}", error)
                }
        } catch (e: Exception) {
            logger.error("Error creating patch operations for user item: ${userItem.id}", e)
            Mono.error(e)
        }
    }

    /**
     * ユーザーアイテムを保存する（テストデータ作成用）
     */
    fun saveUserItem(userItem: UserItem): Mono<UserItem> {
        return cosmosAsyncContainer.createItem(userItem)
            .map { response -> response.item }
            .doOnSuccess {
                logger.debug("Successfully saved user item: ${userItem.id}")
            }
            .doOnError { error ->
                logger.error("Failed to save user item: ${userItem.id}", error)
            }
    }
}
