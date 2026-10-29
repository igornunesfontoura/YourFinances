package io.github.igornunesfontoura.yourfinances.ui.lock

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppLockTest {
    private var now = 0L
    private val lock = AppLock(now = { now })

    @Test
    fun `starts locked and unlocks`() {
        lock.onForeground(lockAvailable = true)
        assertTrue(lock.locked.value)
        lock.unlock()
        assertFalse(lock.locked.value)
    }

    @Test
    fun `a quick switch does not lock, a minute away does`() {
        lock.unlock()
        lock.onBackground()
        now += 59_999
        lock.onForeground(lockAvailable = true)
        assertFalse(lock.locked.value)

        lock.onBackground()
        now += 60_000
        lock.onForeground(lockAvailable = true)
        assertTrue(lock.locked.value)
    }

    @Test
    fun `no lock when it is off or the device has no screen lock`() {
        lock.onForeground(lockAvailable = false)
        assertFalse(lock.locked.value)
        lock.onBackground()
        now += 600_000
        lock.onForeground(lockAvailable = false)
        assertFalse(lock.locked.value)
    }

    @Test
    fun `coming back without having left does not lock`() {
        lock.unlock()
        now += 600_000
        lock.onForeground(lockAvailable = true)
        assertFalse(lock.locked.value)
    }

    @Test
    fun `the prompt opens on its own once per lock`() {
        assertTrue(lock.consumeAutoPrompt())
        assertFalse(lock.consumeAutoPrompt())
        lock.unlock()
        lock.onBackground()
        now += 60_000
        lock.onForeground(lockAvailable = true)
        assertTrue(lock.consumeAutoPrompt())
        assertFalse(lock.consumeAutoPrompt())
    }
}
