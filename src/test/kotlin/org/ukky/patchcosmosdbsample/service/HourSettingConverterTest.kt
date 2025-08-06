package org.ukky.patchcosmosdbsample.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HourSettingConverterTest {

    private val converter = HourSettingConverter()

    @Test
    fun `convertHourSetting should convert 2-hour units to 1-hour units correctly`() {
        // Given
        val originalHourSetting = listOf(1, 2, 3, 4, 7, 9, 12)

        // When
        val result = converter.convertHourSetting(originalHourSetting)

        // Then
        val expected = listOf(1, 2, 3, 4, 5, 6, 7, 8, 13, 14, 17, 18, 23, 24)
        assertEquals(expected, result)
    }

    @Test
    fun `convertHourSetting should handle single values correctly`() {
        // Given
        val originalHourSetting = listOf(6)

        // When
        val result = converter.convertHourSetting(originalHourSetting)

        // Then
        val expected = listOf(11, 12)
        assertEquals(expected, result)
    }

    @Test
    fun `needsConversion should return true when version is null`() {
        // When
        val result = converter.needsConversion(null)

        // Then
        assertTrue(result)
    }

    @Test
    fun `needsConversion should return true when version is less than 1`() {
        // When
        val result = converter.needsConversion(0)

        // Then
        assertTrue(result)
    }

    @Test
    fun `needsConversion should return false when version is 1 or greater`() {
        // When
        val result1 = converter.needsConversion(1)
        val result2 = converter.needsConversion(2)

        // Then
        assertFalse(result1)
        assertFalse(result2)
    }
}
