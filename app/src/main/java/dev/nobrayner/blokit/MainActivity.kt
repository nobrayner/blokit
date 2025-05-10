package dev.nobrayner.blokit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nobrayner.blokit.ui.theme.BlokitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlokitTheme {
                val todoModel: TodoViewModel = viewModel()
                val blockModel: BlockViewModel = viewModel()

                Scaffold(
                    contentWindowInsets = WindowInsets(0),
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding(),
                    bottomBar = {
                        NewTodo(todoModel)
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        Blocks(blockModel)
                        TodoList(
                            todoModel,
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Blocks(blockModel: BlockViewModel) {
    val timerState by blockModel.timerState.collectAsState()
    var showStartDialog by remember {
        mutableStateOf(false)
    }
    var showCancelDialog by remember {
        mutableStateOf(false)
    }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            blockModel.onNotificationPermissionResult(isGranted, 25.minutes)
        }

    LaunchedEffect(Unit) {
        blockModel.permissionRequest.collect {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Before you begin...") },
            text = {
                    Text(
                        "Picture what the chosen task(s) " +
                                "look like once completed!"
                    )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStartDialog = false
                    blockModel.onStartTimerClicked()
                }) {
                    Text("Start")
                }
            }
        )
    }
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Are you sure?") },
            text = {
                Text(
                    "Cancelling the block means you'll have to start all over again!"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    blockModel.cancelBlock()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (timerState.isRunning) {
            Text(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                text =
                    timerState.duration.toComponents { _, minutes, seconds, _ ->
                        "%02d:%02d".format(minutes, seconds)
                    },
                modifier = Modifier
                    .clickable {
                        showCancelDialog = true
                    }
            )
        } else {
            Button(
                onClick = {
                    showStartDialog = true
                }
            ) {
                Text(
                    text = "Start Block",
                )
            }
        }
    }
    TodaysBlocks(blockModel)
}

@Composable
fun TodaysBlocks(blockModel: BlockViewModel) {
    val blocks by blockModel.todaysBlocks.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (blocks.isEmpty()) {
            Text(
                "No blocks today",
                color = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            Row(
                modifier = Modifier
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (block in blocks.take(11)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .size(32.dp),
                    )
                }
                if (blocks.size > 12) {
                    Box(
                        modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val hidden = blocks.size - 11
                        Text(
                            "+$hidden",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else if (blocks.size == 12) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun NewTodo(
    todoModel: TodoViewModel
) {
    var text by remember {
        mutableStateOf("")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        TextField(
            value = text,
            modifier = Modifier
                .fillMaxWidth(),
            onValueChange = {
                text = it
            },
            placeholder = {
                Text("New Todo...")
            },
            shape = RectangleShape,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    todoModel.newTodo(text)
                    text = ""
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        todoModel.newTodo(text)
                        text = ""
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                    enabled = text.trim() != "",
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

    }
}

@Composable
fun TodoList(
    todoModel: TodoViewModel,
    modifier: Modifier = Modifier,
) {
    val todos by todoModel.incompleteTodos.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var shouldScrollToBottom by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(todos.size) {
        if (shouldScrollToBottom) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    (todos.size - 1).coerceAtLeast(0)
                )
            }
        }

        if (todos.isNotEmpty()) {
            shouldScrollToBottom = true
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        items(items = todos, key = { it.id }) { todo ->
            SwipeToDeleteContainer(
                item = todo,
                onSwipeLeft = {
                    todoModel.completeTodo(it)
                },
                onSwipeRight = {
                    todoModel.toggleMarked(it)
                }
            ) {
                IncompleteTodo(todo)
            }
        }
    }
}

@Composable
fun IncompleteTodo(
    todo: Todo
) {
    val markedColor = MaterialTheme.colorScheme.primaryContainer
    val animatedMarkedColor = remember {
        Animatable(
            if (todo.marked) {
                markedColor
            } else {
                Color.Transparent
            }
        )
    }

    val animationDuration = 200.milliseconds

    LaunchedEffect(todo.marked) {
        if (todo.marked) {
            animatedMarkedColor.animateTo(markedColor, animationSpec = tween(durationMillis = animationDuration.toInt(DurationUnit.MILLISECONDS)))
        } else {
            animatedMarkedColor.animateTo(Color.Transparent, animationSpec = tween(durationMillis = animationDuration.toInt(DurationUnit.MILLISECONDS)))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(animatedMarkedColor.value)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = todo.content)
        }
    }
}

@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onSwipeLeft: (T) -> Unit,
    onSwipeRight: (T) -> Unit,
    animationDuration: Duration = 500.milliseconds,
    content: @Composable (T) -> Unit
) {
    var shouldActionLeftSwipe by remember {
        mutableStateOf(false)
    }
    var shouldActionRightSwipe by remember {
        mutableStateOf(false)
    }

    val state = rememberSwipeToDismissBoxState(
        // Use this if you don't want the confirmation step
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    shouldActionLeftSwipe = true
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    shouldActionRightSwipe = true
                    true
                }
                else -> false
            }
        },
        positionalThreshold = { width ->
            val threshold = (width * 0.3).dp.value
            Log.d("DEBUG", "positionalThreshold: ($width) -> $threshold")
            threshold
        }

    )

    LaunchedEffect(key1 = shouldActionLeftSwipe) {
        if(shouldActionLeftSwipe) {
            delay(animationDuration)
            onSwipeLeft(item)
            shouldActionLeftSwipe = false
            state.reset()
        }
    }

    LaunchedEffect(key1 = shouldActionRightSwipe) {
        if (shouldActionRightSwipe) {
            onSwipeRight(item)
            state.reset()
            shouldActionRightSwipe = false
        }
    }

    AnimatedVisibility(
        visible = !shouldActionLeftSwipe,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = animationDuration.toInt(DurationUnit.MILLISECONDS)),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = {
                SwipeBackground(swipeDismissState = state)
            },
            content = { content(item) },
            enableDismissFromEndToStart = true,
            enableDismissFromStartToEnd = true,
        )
    }
}

@Composable
fun SwipeBackground(
    swipeDismissState: SwipeToDismissBoxState,
) {
    val color = when (swipeDismissState.dismissDirection) {
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val alignment = when (swipeDismissState.dismissDirection) {
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        else -> Alignment.Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = alignment
    ) {
        when (swipeDismissState.dismissDirection) {
            SwipeToDismissBoxValue.EndToStart -> Row {
                Text(
                    "Complete",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(20.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            SwipeToDismissBoxValue.StartToEnd -> Row {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    "Mark",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            else -> Row {}
        }
    }
}