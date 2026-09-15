package com.interviewcoach.app.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MasteryColorTest {
    @Test
    fun `below 60 is low`() {
        assertThat(masteryColor(0.0)).isEqualTo(AppColors.MasteryLow)
        assertThat(masteryColor(59.9)).isEqualTo(AppColors.MasteryLow)
    }

    @Test
    fun `60 up to just under 80 is mid`() {
        assertThat(masteryColor(60.0)).isEqualTo(AppColors.MasteryMid)
        assertThat(masteryColor(79.9)).isEqualTo(AppColors.MasteryMid)
    }

    @Test
    fun `80 and above is high`() {
        assertThat(masteryColor(80.0)).isEqualTo(AppColors.MasteryHigh)
        assertThat(masteryColor(100.0)).isEqualTo(AppColors.MasteryHigh)
    }
}
