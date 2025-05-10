package dev.nobrayner.blokit

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

enum class TodoView {
    Incomplete,
    Marked,
    All,
}

class IncompleteTodoViewModel(application: Application) : AndroidViewModel(application) {
    private val todoDao = AppDatabase.getInstance(application).todoDao()

    val incompleteTodos: StateFlow<List<Todo>> = todoDao
        .getIncompleteTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    private val resetSignals = mutableStateMapOf<Int, Long>()

    fun resetSignalFor(todo: Todo): Long? {
        return resetSignals[todo.id]
    }

    var view = mutableStateOf(TodoView.Incomplete)

    fun newTodo(text: String) {
        val content = text.trim()
        if (content != "") {
            val todo = Todo(content = content)
            viewModelScope.launch {
                todoDao.insert(todo)
            }
        }

    }

    fun completeTodo(todo: Todo) {
        viewModelScope.launch {
            val completedTodo = todo.copy(completed = true, completedAt = Instant.now())

            todoDao.update(completedTodo);
        }
    }

    fun toggleMarked(todo: Todo) {
        viewModelScope.launch {
            val markedTodo = todo.copy(
                marked = !todo.marked,
                markedAt = if (!todo.marked) {
                    Instant.now()
                } else {
                    null
                },
            )

            todoDao.update(markedTodo)
        }
    }

    fun undoCompleteTodo(todo: Todo) {
        resetSignals[todo.id] = System.currentTimeMillis()
        viewModelScope.launch {
            val incompleteTodo = todo.copy(completed = false, completedAt = null)

            todoDao.update(incompleteTodo)
        }
    }
}

data class TimerUiState(
    val workId: UUID? = null,
    val duration: Duration = Duration.ZERO,
    val isRunning: Boolean = false
)

class BlockViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)

    private val _timerState = MutableStateFlow(TimerUiState())
    private val _permissionRequest = MutableSharedFlow<Unit>()
    private val blockDao = AppDatabase.getInstance(application).blockDao()

    val permissionRequest = _permissionRequest.asSharedFlow()
    val timerState: StateFlow<TimerUiState> = _timerState

    val todaysBlocks: StateFlow<List<Block>> = blockDao.getTodaysBlocks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        workManager.getWorkInfosByTagLiveData("countdown")
            .asFlow()
            .map { workInfos ->
                val info = workInfos.firstOrNull()
                TimerUiState(
                    workId = _timerState.value.workId,
                    duration = (info?.progress?.getLong(CountdownWorker.KEY_PROGRESS, 0L) ?: 0L).seconds,
                    isRunning = info?.state == WorkInfo.State.RUNNING
                )
            }
            .onEach { _timerState.value = it }
            .launchIn(viewModelScope)
    }


    fun onStartTimerClicked() {
        viewModelScope.launch {
            _permissionRequest.emit(Unit)
        }
    }

    fun onNotificationPermissionResult(granted: Boolean, duration: Duration) {
        if (granted) {
            startCountdown(duration)
        }
    }

    fun cancelBlock() {
        val workId = _timerState.value.workId

        if (workId != null) {
            workManager.cancelWorkById(workId)
        }
    }

    private fun startCountdown(duration: Duration) {
        val request = OneTimeWorkRequestBuilder<CountdownWorker>()
            .setInputData(workDataOf(CountdownWorker.KEY_DURATION to duration.toLong(DurationUnit.SECONDS)))
            .addTag("countdown")
            .build()

        _timerState.value = TimerUiState(
            request.id,
            duration,
            isRunning = false,
        )

        workManager.enqueueUniqueWork(
            "countdown_timer",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}