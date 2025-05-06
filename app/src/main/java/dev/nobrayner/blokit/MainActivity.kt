package dev.nobrayner.blokit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nobrayner.blokit.ui.theme.BlokitTheme
import androidx.compose.foundation.lazy.items

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlokitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Todolist(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Todolist(todoViewModel: TodoViewModel = viewModel(), modifier: Modifier) {
    val todos by todoViewModel.incompleteTodos.collectAsState()

    LazyColumn(
        modifier = modifier
    ) {
        items(todos) { todo ->
                Text(
                    text = todo.content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                )
        }
    }
}