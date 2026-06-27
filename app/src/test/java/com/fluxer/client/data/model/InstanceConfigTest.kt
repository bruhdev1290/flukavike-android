package com.fluxer.client.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceConfigTest {
    @Test
    fun `media endpoint is preferred over static asset endpoint for uploaded images`() {
        val config = InstanceConfig(
            cdn = "https://legacy-web.example",
            endpoints = InstanceConfig.EndpointsConfig(
                media = "https://media.example",
                staticCdn = "https://static.example"
            )
        )

        assertEquals("https://media.example", config.resolvedCdn())
    }
}
