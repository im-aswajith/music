package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalPreferencesRepository
import com.example.data.model.YouTubeVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TubeSearch", appName)
  }

  @Test
  fun `test local repository search history and bookmarks`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LocalPreferencesRepository(context)

    // Test search history
    repo.addSearchQuery("lofi hip hop live")
    val history = repo.getSearchHistory()
    assertTrue(history.contains("lofi hip hop live"))

    // Test bookmarking video
    val testVideo = YouTubeVideo(
      id = "test_vid_123",
      title = "Test Live Stream",
      channelTitle = "Test Channel",
      thumbnailUrl = "https://example.com/thumb.jpg",
      isLive = true
    )
    val saved = repo.toggleSavedVideo(testVideo)
    assertTrue(saved)
    assertTrue(repo.isVideoSaved("test_vid_123"))

    // Toggle unsave
    val unsaved = repo.toggleSavedVideo(testVideo)
    assertEquals(false, unsaved)
    assertEquals(false, repo.isVideoSaved("test_vid_123"))
  }
}

