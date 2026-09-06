package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DailyRitualPhase
import com.example.model.Priority
import com.example.model.TaskStatus
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    assertEquals("Ultimate Brain", appName)
  }

  @Test
  fun `verify initial tasks list contains hero task and overdue items`() {
    val viewModel = MyDayViewModel()
    val state = viewModel.uiState.value

    val heroTask = state.tasks.find { it.id == "tk-w-1" }
    assertNotNull(heroTask)
    assertEquals("Cut release branch and tag", heroTask?.name)
    assertEquals(TaskStatus.DOING, heroTask?.status)
    assertEquals(Priority.HIGH, heroTask?.priority)
    assertEquals(3, heroTask?.subTasks?.size)

    val overdueTask = state.tasks.find { it.id == "tk-w-overdue-1" }
    assertNotNull(overdueTask)
    assertTrue(overdueTask?.isOverdue == true)
  }

  @Test
  fun `navigation between screens works properly`() {
    val viewModel = MyDayViewModel()
    assertEquals(AppScreen.TODAY, viewModel.uiState.value.currentScreen)

    viewModel.navigateTo(AppScreen.TASKS)
    assertEquals(AppScreen.TASKS, viewModel.uiState.value.currentScreen)

    viewModel.openTaskDetail("tk-w-1")
    assertEquals(AppScreen.TASK_DETAIL, viewModel.uiState.value.currentScreen)
    assertEquals("tk-w-1", viewModel.uiState.value.selectedTaskId)
    assertEquals("Cut release branch and tag", viewModel.uiState.value.selectedTask?.name)
  }

  @Test
  fun `toggle subtask and acceptance criterion`() {
    val viewModel = MyDayViewModel()
    val initialChecked = viewModel.uiState.value.selectedTask?.subTasks?.first()?.isCompleted
    assertEquals(false, initialChecked)

    viewModel.toggleSubTask("tk-w-1", "st-1")
    val updatedChecked = viewModel.uiState.value.tasks.find { it.id == "tk-w-1" }?.subTasks?.find { it.id == "st-1" }?.isCompleted
    assertEquals(true, updatedChecked)

    // Add subtask
    viewModel.addSubTask("tk-w-1", "Verify release notes")
    val currentSubTasks = viewModel.uiState.value.tasks.find { it.id == "tk-w-1" }?.subTasks
    assertEquals(4, currentSubTasks?.size)
  }

  @Test
  fun `toggle task status and priority`() {
    val viewModel = MyDayViewModel()
    viewModel.updateTaskStatus("tk-w-1", TaskStatus.DONE)
    val taskDone = viewModel.uiState.value.tasks.find { it.id == "tk-w-1" }
    assertTrue(taskDone?.isDone == true)

    viewModel.updateTaskPriority("tk-w-1", Priority.LOW)
    val taskLow = viewModel.uiState.value.tasks.find { it.id == "tk-w-1" }
    assertEquals(Priority.LOW, taskLow?.priority)
  }
}

