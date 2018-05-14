package com.ryanharter.android.gl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GLStateTest {

  @Test fun givenResetListener_onReset_callsListener() {
    var called = false
    val listener = { called = true }
    GLState.addResetListener(listener)
    GLState.reset()
    assertThat(called).isTrue()
    GLState.removeResetListener(listener)
  }
}