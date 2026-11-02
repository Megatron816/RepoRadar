package com.example.githubprofileexplorer.data.auth

import com.example.githubprofileexplorer.data.model.OAuthTokenResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class DeviceFlowPollerTest {

    @Test
    fun pollsPendingThenSucceeds() = runBlocking {
        var calls = 0
        val waits = mutableListOf<Long>()
        val token = DeviceFlowPoller(
            poll = {
                calls++
                if (calls == 1) OAuthTokenResponse(null, "authorization_pending")
                else OAuthTokenResponse("token", null)
            },
            wait = { waits += it }
        ).awaitToken(5, 30)
        assertEquals("token", token)
        assertEquals(listOf(5L), waits)
    }

    @Test
    fun slowDownIncreasesInterval() = runBlocking {
        var calls = 0
        val waits = mutableListOf<Long>()
        DeviceFlowPoller(
            poll = {
                calls++
                if (calls == 1) OAuthTokenResponse(null, "slow_down")
                else OAuthTokenResponse("token", null)
            },
            wait = { waits += it }
        ).awaitToken(5, 60)
        assertEquals(listOf(10L), waits)
    }

    @Test
    fun accessDeniedFails() = runBlocking {
        try {
            DeviceFlowPoller(poll = { OAuthTokenResponse(null, "access_denied") }, wait = {})
                .awaitToken(5, 30)
            fail("expected DeviceFlowException")
        } catch (e: DeviceFlowException) {
            assertEquals("access_denied", e.message)
        }
    }

    @Test
    fun expiresWhenNeverAuthorised() = runBlocking {
        try {
            DeviceFlowPoller(poll = { OAuthTokenResponse(null, "authorization_pending") }, wait = {})
                .awaitToken(5, 15)
            fail("expected DeviceFlowException")
        } catch (e: DeviceFlowException) {
            assertEquals("expired_token", e.message)
        }
    }
}
