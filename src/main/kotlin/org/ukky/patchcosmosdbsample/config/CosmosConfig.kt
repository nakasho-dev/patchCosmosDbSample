package org.ukky.patchcosmosdbsample.config

import com.azure.cosmos.CosmosAsyncClient
import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.CosmosClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CosmosConfig {

    @Value("\${azure.cosmos.uri}")
    private lateinit var cosmosDbUri: String

    @Value("\${azure.cosmos.key}")
    private lateinit var cosmosDbKey: String

    @Value("\${azure.cosmos.database}")
    private lateinit var databaseName: String

    @Value("\${azure.cosmos.container}")
    private lateinit var containerName: String

    @Bean
    fun cosmosAsyncClient(): CosmosAsyncClient {
        return CosmosClientBuilder()
            .endpoint(cosmosDbUri)
            .key(cosmosDbKey)
            .buildAsyncClient()
    }

    @Bean
    fun cosmosAsyncContainer(cosmosAsyncClient: CosmosAsyncClient): CosmosAsyncContainer {
        return cosmosAsyncClient.getDatabase(databaseName)
            .getContainer(containerName)
    }
}
