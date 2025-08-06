package org.ukky.patchcosmosdbsample.controller

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.ukky.patchcosmosdbsample.service.CosmosDbService
import org.ukky.patchcosmosdbsample.util.TestDataGenerator
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/batch")
class BatchController(
    private val jobLauncher: JobLauncher,
    private val hourSettingMigrationJob: Job,
    private val jobExplorer: JobExplorer,
    private val cosmosDbService: CosmosDbService
) {

    private val logger = LoggerFactory.getLogger(BatchController::class.java)

    @PostMapping("/migrate-hour-settings")
    fun startHourSettingMigration(): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("Starting hour setting migration job...")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addLong("startTime", System.currentTimeMillis())
                .toJobParameters()

            val jobExecution = jobLauncher.run(hourSettingMigrationJob, jobParameters)

            val response: Map<String, Any> = mapOf(
                "jobId" to jobExecution.id,
                "status" to jobExecution.status.toString(),
                "startTime" to (jobExecution.startTime?.toString() ?: ""),
                "message" to "Migration job started successfully"
            )

            logger.info("Job started with ID: ${jobExecution.id}")
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Failed to start migration job", e)
            val errorResponse: Map<String, Any> = mapOf(
                "error" to "Failed to start migration job",
                "message" to (e.message ?: "Unknown error"),
                "timestamp" to LocalDateTime.now().toString()
            )
            ResponseEntity.internalServerError().body(errorResponse)
        }
    }

    @GetMapping("/status/{jobId}")
    fun getJobStatus(@PathVariable jobId: Long): ResponseEntity<Map<String, Any>> {
        return try {
            val jobExecution = jobExplorer.getJobExecution(jobId)

            if (jobExecution === null) {
                return ResponseEntity.notFound().build()
            }

            val stepExecutions = jobExecution.stepExecutions.map { stepExecution ->
                mapOf<String, Any>(
                    "stepName" to stepExecution.stepName,
                    "status" to stepExecution.status.toString(),
                    "readCount" to stepExecution.readCount,
                    "writeCount" to stepExecution.writeCount,
                    "commitCount" to stepExecution.commitCount,
                    "rollbackCount" to stepExecution.rollbackCount,
                    "skipCount" to stepExecution.skipCount,
                    "startTime" to (stepExecution.startTime?.toString() ?: ""),
                    "endTime" to (stepExecution.endTime?.toString() ?: "")
                )
            }

            val response: Map<String, Any> = mapOf(
                "jobId" to jobExecution.id,
                "jobName" to jobExecution.jobInstance.jobName,
                "status" to jobExecution.status.toString(),
                "exitStatus" to jobExecution.exitStatus.exitCode,
                "startTime" to (jobExecution.startTime?.toString() ?: ""),
                "endTime" to (jobExecution.endTime?.toString() ?: ""),
                "stepExecutions" to stepExecutions,
                "allFailureExceptions" to jobExecution.allFailureExceptions.map { it.message ?: "" }
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Failed to get job status for ID: $jobId", e)
            val errorResponse: Map<String, Any> = mapOf(
                "error" to "Failed to get job status",
                "jobId" to jobId,
                "message" to (e.message ?: "Unknown error")
            )
            ResponseEntity.internalServerError().body(errorResponse)
        }
    }

    @GetMapping("/jobs")
    fun getAllJobs(): ResponseEntity<Map<String, Any>> {
        return try {
            val jobNames = jobExplorer.jobNames
            val jobInstances = jobNames.flatMap { jobName ->
                jobExplorer.getJobInstances(jobName, 0, 10)
            }

            val jobs = jobInstances.map { jobInstance ->
                val executions = jobExplorer.getJobExecutions(jobInstance)
                    .take(5) // 最新5件
                    .map { execution ->
                        mapOf<String, Any>(
                            "executionId" to execution.id,
                            "status" to execution.status.toString(),
                            "startTime" to (execution.startTime?.toString() ?: ""),
                            "endTime" to (execution.endTime?.toString() ?: "")
                        )
                    }

                mapOf<String, Any>(
                    "jobName" to jobInstance.jobName,
                    "instanceId" to jobInstance.instanceId,
                    "executions" to executions
                )
            }

            val response: Map<String, Any> = mapOf(
                "jobs" to jobs,
                "totalJobNames" to jobNames.size
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Failed to get all jobs", e)
            val errorResponse: Map<String, Any> = mapOf(
                "error" to "Failed to get jobs",
                "message" to (e.message ?: "Unknown error")
            )
            ResponseEntity.internalServerError().body(errorResponse)
        }
    }

    @PostMapping("/create-test-data")
    fun createTestData(@RequestParam(defaultValue = "1000") count: Int): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("Creating $count test data items...")

            val testDataGenerator = TestDataGenerator(cosmosDbService)
            val testItems = testDataGenerator.generateTestUserItems(count)

            testDataGenerator.saveTestData(testItems).block()

            val response: Map<String, Any> = mapOf(
                "message" to "Test data created successfully",
                "itemCount" to count,
                "timestamp" to LocalDateTime.now().toString()
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Failed to create test data", e)
            val errorResponse: Map<String, Any> = mapOf(
                "error" to "Failed to create test data",
                "message" to (e.message ?: "Unknown error")
            )
            ResponseEntity.internalServerError().body(errorResponse)
        }
    }

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return try {
            val response: Map<String, Any> = mapOf(
                "status" to "UP",
                "timestamp" to LocalDateTime.now().toString(),
                "cosmosDb" to "Connected"
            )
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            val errorResponse: Map<String, Any> = mapOf(
                "status" to "DOWN",
                "timestamp" to LocalDateTime.now().toString(),
                "error" to (e.message ?: "Connection failed")
            )
            ResponseEntity.status(503).body(errorResponse)
        }
    }
}
