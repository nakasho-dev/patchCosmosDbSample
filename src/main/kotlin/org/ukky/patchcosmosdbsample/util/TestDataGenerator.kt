package org.ukky.patchcosmosdbsample.util

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.ukky.patchcosmosdbsample.model.AppSetting
import org.ukky.patchcosmosdbsample.model.UserItem
import org.ukky.patchcosmosdbsample.service.CosmosDbService
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Component
class TestDataGenerator(
    private val cosmosDbService: CosmosDbService
) {

    private val logger = LoggerFactory.getLogger(TestDataGenerator::class.java)

    /**
     * テスト用のユーザーデータを生成する
     */
    fun generateTestUserItems(count: Int): List<UserItem> {
        logger.info("Generating $count test user items...")

        return (1..count).map { index ->
            val userId = "user_${String.format("%06d", index)}"
            val hourSettings = generateRandomHourSettings()

            UserItem(
                id = UUID.randomUUID().toString(),
                userId = userId,
                appSetting = AppSetting(
                    hourSetting = hourSettings,
                    hourSettingVersion = null // 変換前なのでnull
                )
            )
        }
    }

    /**
     * ランダムなhour_settingを生成（1-12の範囲で1-7個）
     */
    private fun generateRandomHourSettings(): List<Int> {
        val random = Random()
        val count = random.nextInt(7) + 1 // 1-7個
        val settings = mutableSetOf<Int>()

        while (settings.size < count) {
            settings.add(random.nextInt(12) + 1) // 1-12
        }

        return settings.sorted()
    }

    /**
     * テストデータをCosmosDBに保存する
     */
    fun saveTestData(userItems: List<UserItem>): Mono<Void> {
        logger.info("Saving ${userItems.size} test items to CosmosDB...")

        return Flux.fromIterable(userItems)
            .concatMap { userItem ->
                cosmosDbService.saveUserItem(userItem)
                    .doOnSuccess {
                        logger.debug("Saved test item: ${userItem.id}")
                    }
                    .doOnError { error ->
                        logger.error("Failed to save test item: ${userItem.id}", error)
                    }
            }
            .then()
            .doOnSuccess {
                logger.info("Successfully saved all test data")
            }
    }

    /**
     * 変換済みテストデータも生成（テスト用）
     */
    fun generateConvertedTestUserItems(count: Int): List<UserItem> {
        logger.info("Generating $count converted test user items...")

        return (1..count).map { index ->
            val userId = "converted_user_${String.format("%06d", index)}"
            val originalHourSettings = generateRandomHourSettings()
            val convertedHourSettings = originalHourSettings.flatMap { twoHourUnit ->
                val startHour = (twoHourUnit - 1) * 2 + 1
                val endHour = startHour + 1
                listOf(startHour, endHour)
            }.sorted()

            UserItem(
                id = UUID.randomUUID().toString(),
                userId = userId,
                appSetting = AppSetting(
                    hourSetting = convertedHourSettings,
                    hourSettingVersion = 1 // 変換済み
                )
            )
        }
    }
}
