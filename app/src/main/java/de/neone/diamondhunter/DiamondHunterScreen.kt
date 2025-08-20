
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.neone.diamondhunter.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

@Composable
fun DiamondHunter(innerPadding: PaddingValues) {
    var gameSpeed by remember { mutableStateOf(300L) }
    val initialSpeed = 300L
    val speedIncreaseFactor = 0.98f
    val minSpeed = 50L
    val snake = remember { mutableStateListOf(Offset(5f, 5f)) }
    val foodList = remember { mutableStateListOf<Offset>() }
    var direction by remember { mutableStateOf(Offset(1f, 0f)) }
    var nextDirection by remember { mutableStateOf<Offset?>(null) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }
    var gridWidth by remember { mutableStateOf(0) }
    var gridHeight by remember { mutableStateOf(0) }
    var cellSizePx by remember { mutableStateOf(0f) }

    fun resetGame() {
        snake.clear()
        snake.add(Offset(5f, 5f))
        foodList.clear()
        direction = Offset(1f, 0f)
        nextDirection = null
        isGameOver = false
        showGameOverDialog = false
        gameSpeed = initialSpeed
        if (score > highScore) {
            highScore = score
        }
        score = 0
        gameStarted = true
    }

    val snakePath by remember(snake, cellSizePx) {
        derivedStateOf {
            Path().apply {
                snake.forEach { position ->
                    addOval(
                        Rect(
                            position.x * cellSizePx,
                            position.y * cellSizePx,
                            (position.x + 1) * cellSizePx,
                            (position.y + 1) * cellSizePx
                        )
                    )
                }
            }
        }
    }

    val foodPath by remember(foodList, cellSizePx) {
        derivedStateOf {
            Path().apply {
                foodList.forEach { food ->
                    val centerX = food.x * cellSizePx + cellSizePx / 2
                    val centerY = food.y * cellSizePx + cellSizePx / 2
                    val halfSize = cellSizePx / 2
                    moveTo(centerX, centerY - halfSize)
                    lineTo(centerX + halfSize, centerY)
                    lineTo(centerX, centerY + halfSize)
                    lineTo(centerX - halfSize, centerY)
                    close()
                }
            }
        }
    }

    LaunchedEffect(gameStarted, gridWidth, gridHeight) {
        if (gameStarted && gridWidth > 0 && gridHeight > 0) {
            foodList.clear()
            foodList.addAll(generateFoodList(gridWidth, gridHeight, snake, 3))
            val gameLoop = Channel<Unit>()
            launch {
                while (!isGameOver) {
                    delay(gameSpeed)
                    gameLoop.send(Unit)
                }
            }
            for (event in gameLoop) {
                nextDirection?.let {
                    direction = it
                    nextDirection = null
                }
                val newHead = Offset(snake.first().x + direction.x, snake.first().y + direction.y)
                if (newHead.x < 0 || newHead.x >= gridWidth || newHead.y < 0 || newHead.y >= gridHeight || snake.drop(1).any { it == newHead }) {
                    isGameOver = true
                    showGameOverDialog = true
                    gameStarted = false
                    break
                }
                var ateFood = false
                if (newHead in foodList) {
                    ateFood = true
                    foodList.remove(newHead)
                    if (foodList.size < 3) {
                        foodList.add(generateFood(gridWidth, gridHeight, snake + foodList))
                    }
                }
                if (ateFood) {
                    snake.add(0, newHead)
                    score += 10
                    gameSpeed = max(minSpeed, (gameSpeed * speedIncreaseFactor).toLong())
                } else {
                    snake.add(0, newHead)
                    snake.removeAt(snake.lastIndex)
                }
            }
        }
    }

    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game Over!") },
            text = { Text("Your score: $score\nHigh score: $highScore") },
            confirmButton = {
                Button(
                    onClick = { showGameOverDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            modifier = Modifier.background(Color.White)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF163772))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Diamond-Hunter by N.E.O.N.E",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "High-Score: $highScore",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Score: $score",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Button(
                    onClick = { resetGame() },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("PLAY", color = Color.White)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF225ABB))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (isGameOver) {
                                    resetGame()
                                }
                            }
                        }
                ) {
                    val desiredCellSize = 30.dp.toPx()
                    gridWidth = (size.width / desiredCellSize).toInt().coerceAtLeast(10)
                    gridHeight = (size.height / desiredCellSize).toInt().coerceAtLeast(10)
                    cellSizePx = size.width / gridWidth

                    // Zeichne Gitter
                    for (i in 0..gridWidth) {
                        drawLine(
                            color = Color(0xFF43B89D),
                            start = Offset(i * cellSizePx, 0f),
                            end = Offset(i * cellSizePx, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 0..gridHeight) {
                        drawLine(
                            color = Color(0xFF43B89D),
                            start = Offset(0f, i * cellSizePx),
                            end = Offset(size.width, i * cellSizePx),
                            strokeWidth = 1f
                        )
                    }

                    // Zeichne Schlange
                    drawPath(
                        path = snakePath,
                        color = Color(0xFF00FFD9)
                    )

                    // Zeichne Essen
                    drawPath(
                        path = foodPath,
                        color = Color(0xFFFFC800)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { nextDirection = Offset(0f, -1f) },
                        modifier = Modifier
                            .height(80.dp)
                            .width(80.dp)
                            .padding(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Image(
                            painterResource(id = R.drawable.baseline_arrow_circle_up_62),
                            contentDescription = "UP", modifier = Modifier.scale(1.5f)
                        )
                    }

                    Row {
                        Button(
                            onClick = { nextDirection = Offset(-1f, 0f) },
                            modifier = Modifier
                                .height(80.dp)
                                .width(80.dp)
                                .padding(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                        ) {
                            Image(
                                painterResource(id = R.drawable.outline_arrow_circle_left_62),
                                contentDescription = "LEFT", modifier = Modifier.scale(1.5f)
                            )
                        }
                        Spacer(modifier = Modifier.width(50.dp))

                        Button(
                            onClick = { nextDirection = Offset(1f, 0f) },
                            modifier = Modifier
                                .height(80.dp)
                                .width(80.dp)
                                .padding(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                        ) {
                            Image(
                                painterResource(id = R.drawable.outline_arrow_circle_right_62),
                                contentDescription = "RIGHT", modifier = Modifier.scale(1.5f)
                            )
                        }
                    }

                    Button(
                        onClick = { nextDirection = Offset(0f, 1f) },
                        modifier = Modifier
                            .height(80.dp)
                            .width(80.dp)
                            .padding(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Image(
                            painterResource(id = R.drawable.outline_arrow_circle_down_62),
                            contentDescription = "DOWN", modifier = Modifier.scale(1.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun generateFoodList(
    gridWidth: Int,
    gridHeight: Int,
    snake: List<Offset>,
    count: Int,
): List<Offset> {
    val foodList = mutableListOf<Offset>()
    repeat(count) {
        foodList.add(generateFood(gridWidth, gridHeight, snake + foodList))
    }
    return foodList
}

private fun generateFood(gridWidth: Int, gridHeight: Int, snake: List<Offset>): Offset {
    var x: Int
    var y: Int
    var newFood: Offset
    var attempts = 0
    do {
        x = Random.nextInt(0, gridWidth)
        y = Random.nextInt(0, gridHeight)
        newFood = Offset(x.toFloat(), y.toFloat())
        attempts++
        if (attempts > 100) break // Vermeide Endlosschleife
    } while (newFood in snake)
    return newFood
}

@Composable
fun DiamondHunterGameScreen(innerPadding: PaddingValues) {
    MaterialTheme {
        DiamondHunter(innerPadding = innerPadding)
    }
}