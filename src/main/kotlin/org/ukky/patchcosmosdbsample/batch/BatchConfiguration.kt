package org.ukky.patchcosmosdbsample.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.ukky.patchcosmosdbsample.model.UserItem
import org.ukky.patchcosmosdbsample.service.CosmosDbService
import org.ukky.patchcosmosdbsample.service.HourSettingConverter
import java.util.concurrent.atomic.AtomicInteger

@Configuration
class BatchConfiguration(
    private val cosmosDbService: CosmosDbService,
    private val hourSettingConverter: HourSettingConverter
) {

    private val logger = LoggerFactory.getLogger(BatchConfiguration::class.java)

    @Bean
    fun userItemReader(): ItemReader<UserItem> {
        return CosmosItemReader(cosmosDbService)
    }

    @Bean
    fun userItemProcessor(): ItemProcessor<UserItem, UserItem> {
        return ItemProcessor { item ->
            if (hourSettingConverter.needsConversion(item.appSetting.hourSettingVersion)) {
                logger.debug("Processing item: ${item.id}")
                item
            } else {
                logger.debug("Skipping already converted item: ${item.id}")
                null // スキップ
            }
        }
    }

    @Bean
    fun userItemWriter(): ItemWriter<UserItem> {
        return CosmosItemWriter(cosmosDbService)
    }

    @Bean
    fun migrationStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager
    ): Step {
        return StepBuilder("migrationStep", jobRepository)
            .chunk<UserItem, UserItem>(100, transactionManager)
            .reader(userItemReader())
            .processor(userItemProcessor())
            .writer(userItemWriter())
            .build()
    }

    @Bean
    fun hourSettingMigrationJob(
        jobRepository: JobRepository,
        migrationStep: Step
    ): Job {
        return JobBuilder("hourSettingMigrationJob", jobRepository)
            .start(migrationStep)
            .build()
    }
}

class CosmosItemReader(
    private val cosmosDbService: CosmosDbService
) : AbstractItemCountingItemStreamItemReader<UserItem>() {

    private val logger = LoggerFactory.getLogger(CosmosItemReader::class.java)
    private var items: Iterator<UserItem>? = null
    private var initialized = false

    init {
        // nameを設定してExecutionContextのキープレフィックスを定義
        setName("cosmosItemReader")
    }

    override fun doRead(): UserItem? {
        if (!initialized) {
            logger.info("Initializing CosmosItemReader...")
            val itemList = cosmosDbService.getItemsNeedingConversion()
                .collectList()
                .block()

            items = itemList?.iterator()
            initialized = true
            logger.info("Found ${itemList?.size ?: 0} items needing conversion")
        }

        return items?.takeIf { it.hasNext() }?.next()
    }

    override fun doOpen() {
        // 初期化処理
    }

    override fun doClose() {
        // クリーンアップ処理
    }
}

class CosmosItemWriter(
    private val cosmosDbService: CosmosDbService
) : ItemWriter<UserItem> {

    private val logger = LoggerFactory.getLogger(CosmosItemWriter::class.java)
    private val processedCount = AtomicInteger(0)

    override fun write(chunk: org.springframework.batch.item.Chunk<out UserItem>) {
        val items = chunk.items
        logger.info("Writing ${items.size} items...")

        items.forEach { item ->
            try {
                cosmosDbService.patchUserItem(item).block()
                val count = processedCount.incrementAndGet()
                if (count % 1000 == 0) {
                    logger.info("Processed $count items")
                }
            } catch (e: Exception) {
                logger.error("Failed to patch item: ${item.id}", e)
                throw e
            }
        }

        logger.info("Successfully wrote ${items.size} items. Total processed: ${processedCount.get()}")
    }
}
