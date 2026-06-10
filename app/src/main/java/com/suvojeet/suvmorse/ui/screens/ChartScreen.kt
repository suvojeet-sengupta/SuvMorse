package com.suvojeet.suvmorse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.suvmorse.morse.MorseCode
import com.suvojeet.suvmorse.ui.ChartViewModel

/** Searchable, grouped reference chart. Tap any character to hear its Morse. */
@Composable
fun ChartScreen(
    modifier: Modifier = Modifier,
    vm: ChartViewModel = viewModel()
) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { MorseCode.charToMorse.entries.toList() }
    val filtered = remember(query) {
        if (query.isBlank()) all
        else all.filter {
            it.key.toString().contains(query.trim(), ignoreCase = true) ||
                it.value.contains(query.trim())
        }
    }
    val letters = filtered.filter { it.key.isLetter() }
    val numbers = filtered.filter { it.key.isDigit() }
    val symbols = filtered.filter { !it.key.isLetterOrDigit() }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true,
            placeholder = { Text("Search a letter, number or symbol") }
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            section("Letters", letters) { vm.play(it) }
            section("Numbers", numbers) { vm.play(it) }
            section("Symbols", symbols) { vm.play(it) }
        }
    }
}

private fun LazyGridScope.section(
    title: String,
    entries: List<Map.Entry<Char, String>>,
    onTap: (Char) -> Unit
) {
    if (entries.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    items(entries) { entry ->
        ChartCell(symbol = entry.key, morse = entry.value, onClick = { onTap(entry.key) })
    }
}

@Composable
private fun ChartCell(symbol: Char, morse: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.clickable(onClickLabel = "Play $symbol", onClick = onClick)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = symbol.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = morse,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
