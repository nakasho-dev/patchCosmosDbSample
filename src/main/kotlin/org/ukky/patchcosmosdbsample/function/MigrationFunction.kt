package org.ukky.patchcosmosdbsample.function

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.TimerTrigger
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class MigrationFunction {

    private val logger = LoggerFactory.getLogger(MigrationFunction::class.java)

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var hourSettingMigrationJob: Job

    @FunctionName("hourSettingMigration")
    fun run(
        @TimerTrigger(name = "timerInfo", schedule = "0 0 2 * * *") // 毎日午前2時に実行
        timerInfo: String,
        context: ExecutionContext
    ): String {
        logger.info("Hour setting migration function started at: ${LocalDateTime.now()}")

        return try {
            val jobParameters: JobParameters = JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters()

            val jobExecution = jobLauncher.run(hourSettingMigrationJob, jobParameters)

            val result = when (jobExecution.status) {
                org.springframework.batch.core.BatchStatus.COMPLETED -> {
                    "Migration job completed successfully"
                }
                org.springframework.batch.core.BatchStatus.FAILED -> {
                    "Migration job failed: ${jobExecution.allFailureExceptions.joinToString { it.message ?: "Unknown error" }}"
                }
                else -> {
                    "Migration job finished with status: ${jobExecution.status}"
                }
            }

            logger.info("Migration job result: $result")
            result

        } catch (e: Exception) {
            val errorMessage = "Failed to execute migration job: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @FunctionName("hourSettingMigrationManual")
    fun runManual(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext
    ): HttpResponseMessage {
        logger.info("Manual hour setting migration function triggered")

        return try {
            val jobParameters: JobParameters = JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("trigger", "manual")
                .toJobParameters()

            val jobExecution = jobLauncher.run(hourSettingMigrationJob, jobParameters)

            val message = when (jobExecution.status) {
                org.springframework.batch.core.BatchStatus.COMPLETED -> {
                    "Migration job completed successfully"
                }
                org.springframework.batch.core.BatchStatus.FAILED -> {
                    "Migration job failed: ${jobExecution.allFailureExceptions.joinToString { it.message ?: "Unknown error" }}"
                }
                else -> {
                    "Migration job finished with status: ${jobExecution.status}"
                }
            }

            request.createResponseBuilder(HttpStatus.OK)
                .body(message)
                .build()

        } catch (e: Exception) {
            val errorMessage = "Failed to execute migration job: ${e.message}"
            logger.error(errorMessage, e)

            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMessage)
                .build()
        }
    }
}
