package com.bernaferrari.quietguard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceKeysTest {
    @Test
    fun perAppRuleKeyMatchesPackageName() {
        assertTrue(PreferenceKeys.isPerAppRuleKey("wifi_com.example.app"))
        assertTrue(PreferenceKeys.isPerAppRuleKey("other_com.example.app"))
    }

    @Test
    fun globalKeysAreNotPerAppRuleKeys() {
        assertFalse(PreferenceKeys.isPerAppRuleKey(PreferenceKeys.ENABLED))
        assertFalse(PreferenceKeys.isPerAppRuleKey(PreferenceKeys.FILTER))
        assertFalse(PreferenceKeys.isPerAppRuleKey(PreferenceKeys.LOCKDOWN))
        assertFalse(PreferenceKeys.isPerAppRuleKey(PreferenceKeys.UPDATE_CHECK))
    }

    @Test
    fun updateCheckDefaultsOff() {
        assertFalse(PreferenceKeys.UPDATE_CHECK_DEFAULT)
        assertEquals("update_check", PreferenceKeys.UPDATE_CHECK)
    }

    @Test
    fun prefixedKeysWithoutPackageAreNotPerAppRuleKeys() {
        assertFalse(PreferenceKeys.isPerAppRuleKey("wifi_home"))
        assertFalse(PreferenceKeys.isPerAppRuleKey("enabled"))
    }
}