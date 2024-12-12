@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Overall Stats Card
            StatCard(
                title = "Overall Statistics",
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Games Played", "142")
                        StatItem("Win Rate", "68%")
                        StatItem("High Score", "10,450")
                    }
                }
            )

            // Game-specific Stats
            GameBoard.entries.forEach { board ->
                StatCard(
                    title = board.modeName,
                    content = {
                        Column {
                            // Win rate chart
                            WinRateChart(
                                winRate = 0.75f, // Replace with actual data
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Detailed stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("Games", "45")
                                StatItem("Avg Score", "850")
                                StatItem("Best", "1,240")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun WinRateChart(
    winRate: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2 * 0.8f
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        
        // Background circle
        drawArc(
            color = Color.LightGray.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 25f),
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        
        // Progress arc
        drawArc(
            color = MaterialTheme.colorScheme.primary,
            startAngle = -90f,
            sweepAngle = 360f * winRate,
            useCenter = false,
            style = Stroke(width = 25f),
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        
        // Center text
        val textPaint = Paint().asFrameworkPaint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 50f
            color = MaterialTheme.colorScheme.onSurface.toArgb()
        }
        
        drawContext.canvas.nativeCanvas.drawText(
            "${(winRate * 100).toInt()}%",
            center.x,
            center.y + 15f,
            textPaint
        )
    }
} 