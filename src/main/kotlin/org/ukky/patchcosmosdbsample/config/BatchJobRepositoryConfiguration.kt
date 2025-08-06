package org.ukky.patchcosmosdbsample.config

import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean
import org.springframework.batch.support.DatabaseType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.init.DataSourceInitializer
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class BatchJobRepositoryConfiguration {

    @Bean
    @Primary
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun dataSourceInitializer(dataSource: DataSource): DataSourceInitializer {
        val dataSourceInitializer = DataSourceInitializer()
        dataSourceInitializer.setDataSource(dataSource)

        val databasePopulator = ResourceDatabasePopulator()
        databasePopulator.addScript(ClassPathResource("org/springframework/batch/core/schema-h2.sql"))
        databasePopulator.setContinueOnError(true)

        dataSourceInitializer.setDatabasePopulator(databasePopulator)
        dataSourceInitializer.setEnabled(true)

        return dataSourceInitializer
    }

    @Bean
    @Primary
    fun jobRepository(
        dataSource: DataSource,
        transactionManager: PlatformTransactionManager
    ): JobRepository {
        val factoryBean = JobRepositoryFactoryBean()
        factoryBean.setDataSource(dataSource)
        factoryBean.setTransactionManager(transactionManager)
        factoryBean.setDatabaseType(DatabaseType.H2.productName)
        factoryBean.setValidateTransactionState(false)
        factoryBean.afterPropertiesSet()
        return factoryBean.`object`
    }
}
