package org.ukky.patchcosmosdbsample

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableBatchProcessing
class PatchCosmosDbSampleApplication

fun main(args: Array<String>) {
    runApplication<PatchCosmosDbSampleApplication>(*args)
}
