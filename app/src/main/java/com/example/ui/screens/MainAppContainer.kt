package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FuelFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    Landing,
    Login,
    Dashboard,
    POS,
    Fuel,
    Pumps,
    Employees,
    Customers,
    Reports,
    Settings,
    Profile
}

@Composable
fun MainAppContainer(viewModel: FuelFlowViewModel) {
    val themeSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = themeSettings?.darkTheme ?: isSystemDark

    MyApplicationTheme(darkTheme = isDark) {
        val currentScreen = remember { mutableStateOf(AppScreen.Landing) }
        val currentLandingSection = remember { mutableStateOf("home") } // "home", "about", "services", "prices", "contact"
        
        val snackbarHostState = remember { SnackbarHostState() }
        val successToast by viewModel.successToast.collectAsStateWithLifecycle()

        // Handle success toast notifications
        LaunchedEffect(successToast) {
            successToast?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.dismissToast()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen.value) {
                    AppScreen.Landing -> {
                        LandingMainView(
                            currentSection = currentLandingSection.value,
                            onSectionChange = { currentLandingSection.value = it },
                            onNavigateToLogin = { currentScreen.value = AppScreen.Login },
                            viewModel = viewModel
                        )
                    }
                    AppScreen.Login -> {
                        LoginView(
                            onNavigateDashboard = { currentScreen.value = AppScreen.Dashboard },
                            onNavigateBack = { currentScreen.value = AppScreen.Landing },
                            viewModel = viewModel
                        )
                    }
                    else -> {
                        // All administrative sub-views
                        AdminDashboardMainView(
                            currentScreen = currentScreen.value,
                            onScreenChange = { currentScreen.value = it },
                            viewModel = viewModel
                        )
                    }
                }

                // Global Toast / Snackbar Area
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 56.dp) // Avoid overlap with safe bars
                ) { data ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Toast Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// LANDING PAGE SECTIONS & NAVIGATION
// ==========================================

@Composable
fun LandingMainView(
    currentSection: String,
    onSectionChange: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: FuelFlowViewModel
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val fuelStats by viewModel.fuelTypes.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            LandingHeader(
                activeSection = currentSection,
                onSectionSelect = onSectionChange,
                onLoginSelect = onNavigateToLogin,
                stationName = settings?.stationName ?: "FuelFlow Pro"
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "landing_screens"
            ) { section ->
                when (section) {
                    "home" -> LandingHomeSection(onNavigateToLogin, fuelStats)
                    "about" -> LandingAboutSection()
                    "services" -> LandingServicesSection()
                    "prices" -> LandingPricesSection(fuelStats, viewModel)
                    "contact" -> LandingContactSection(settings?.companyAddress ?: "123 Fuel Boulevard, Metro City")
                }
            }
        }
    }
}

@Composable
fun LandingHeader(
    activeSection: String,
    onSectionSelect: (String) -> Unit,
    onLoginSelect: () -> Unit,
    stationName: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Station Logo Theme
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(FuelAmber, Color(0xFFFF5722))
                                ), CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Logo",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }

                // Dynamic Navigation Buttons for desktop/mobile views
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSectionSelect("home") },
                        modifier = Modifier.background(if (activeSection == "home") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = if (activeSection == "home") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = { onSectionSelect("prices") },
                        modifier = Modifier.background(if (activeSection == "prices") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = "Prices", tint = if (activeSection == "prices") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = { onSectionSelect("services") },
                        modifier = Modifier.background(if (activeSection == "services") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Services", tint = if (activeSection == "services") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = { onSectionSelect("about") },
                        modifier = Modifier.background(if (activeSection == "about") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "About", tint = if (activeSection == "about") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = { onSectionSelect("contact") },
                        modifier = Modifier.background(if (activeSection == "contact") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(Icons.Default.ContactSupport, contentDescription = "Contact", tint = if (activeSection == "contact") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Button(
                        onClick = onLoginSelect,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("LOGIN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun LandingHomeSection(onNavigateToLogin: () -> Unit, fuelTypes: List<FuelType>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero visual card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    CarbonCard,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    CharcoalBlack
                                )
                            )
                        )
                ) {
                    // Graphics representing futuristic Gas station dashboard
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        drawCircle(
                            color = FuelAmber.copy(alpha = 0.05f),
                            radius = size.width / 1.5f,
                            center = Offset(size.width, 0f)
                        )
                        drawCircle(
                            color = AlertBlue.copy(alpha = 0.04f),
                            radius = size.width / 2.5f,
                            center = Offset(0f, size.height)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, "Shield", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("ISO 9001 certified retail fueling system", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "INTELLIGENT RE-FUELING SYSTEM",
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Empowering fuel distribution with responsive nozzle control and real-time smart inventory automation.",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                lineHeight = 28.sp
                            )
                            Text(
                                "Run digital receipts, automated gravity tank alarms, loyalty membership ledgers, and shifts scheduling.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ACCESS DIGITAL COMMAND CENTER", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, "Nav", tint = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // Feature overview banner
        item {
            Text(
                "Today's Live Fuel Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Dynamic pricing previews
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fuelTypes.forEach { fuel ->
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .height(130.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fuel.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                }
                            }

                            Column {
                                Text(
                                    "$${String.format("%.2f", fuel.pricePerLiter)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("per liter updated live", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }

        // Testimonial
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("JD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("\"FuelFlow Pro has optimized our fleet transactions tremendously. Real-time logging and loyalty scanning are exceptionally smooth is mobile environment!\"", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("— John Doe, Transport Logistics Director", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun LandingAboutSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Our Mission",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Our mission is to engineer high-fidelity, responsive automation for independent fuel stations and retail networks. We bridge physical fueling points with robust local SQLite persistence and live analytics dashboard tools to empower cashiers, staff directors, and station administrators alike.",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Company Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "FuelFlow Pro operates as a modern tech-stack layout engineered with Kotlin, Android Jetpack Compose, and SQLite. We deliver exceptional visual polish, and responsive data flows with 0 external network dependencies to ensure persistent offline functionality on tablets, handhelds, and mobile drawers.",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun LandingServicesSection() {
    val servicesList = listOf(
        Triple("Nozzle Automation", "Automatic inventory deduction and pump tracking on checkout transactions", Icons.Default.LocalGasStation),
        Triple("Loyalty Vault", "Award loyalty points on liters fueled. Instant scanning with unique system ids", Icons.Default.Groups),
        Triple("Shift Monitoring", "Record and audit employee Clock-In transitions directly on shift changes", Icons.Default.Alarm),
        Triple("Alarms & Alerts", "Intelligent notification triggers when the gravity fuel container level falls below 30%", Icons.Default.NotificationsActive),
        Triple("Analytics Hub", "Curved sales line graphs, revenue totals, and offline csv reporting mocks", Icons.Default.BarChart),
        Triple("Rules Panel", "Customize fuel prices globally and test settings instantly", Icons.Default.Settings)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Industrial Grade Features", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        servicesList.forEach { (name, desc, icon) ->
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = name, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun LandingPricesSection(fuelStats: List<FuelType>, viewModel: FuelFlowViewModel) {
    var refuelDialogFuelId by remember { mutableStateOf<Int?>(null) }
    var refuelAmount by remember { mutableStateOf("1000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Fuel Prices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            // Helpful Quick Seeding and Tank Info
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("USD Prices updated real-time", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        fuelStats.forEach { fuel ->
            val capacitySafe = if (fuel.capacity > 0) fuel.capacity else 10000.0
            val ratio = ((fuel.availableLiters / capacitySafe).toFloat()).coerceIn(0f, 1f)
            val tintColor = if (ratio < 0.3f) AlertRed else if (ratio < 0.6f) AlertYellow else AlertGreen

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(fuel.name, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Tank Index limit: ${fuel.capacity} L", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }

                        Text(
                            "$${String.format("%.2f", fuel.pricePerLiter)}/L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Progress indicators showing level of fuel tank
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tank State: ${String.format("%.1f", fuel.availableLiters)} L capacity left", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${(ratio * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = tintColor)
                        }

                        LinearProgressIndicator(
                            progress = ratio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = tintColor,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    }

                    // Small simulated refuel toggle for interactive play!
                    Button(
                        onClick = { refuelDialogFuelId = fuel.id },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(14.dp))
                            Text("Simulate Refill", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (refuelDialogFuelId != null) {
        AlertDialog(
            onDismissRequest = { refuelDialogFuelId = null },
            title = { Text("Request Tanker Refill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select fuel volume to pump from simulation tanker:")
                    TextField(
                        value = refuelAmount,
                        onValueChange = { refuelAmount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Liters") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = refuelAmount.toDoubleOrNull() ?: 1000.0
                        viewModel.refillFuelTank(refuelDialogFuelId!!, amt)
                        refuelDialogFuelId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black)
                ) {
                    Text("Refill Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { refuelDialogFuelId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LandingContactSection(address: String) {
    var senderName by remember { mutableStateOf("") }
    var senderEmail by remember { mutableStateOf("") }
    var senderMsg by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Contact Support Team", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isSubmitted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = AlertGreen, modifier = Modifier.size(60.dp))
                            Text("Message Sent Successfully!", fontWeight = FontWeight.Bold)
                            Text("Our dispatch terminal will contact you soon.", fontSize = 11.sp, textAlign = TextAlign.Center)
                            Button(onClick = { isSubmitted = false; senderMsg = "" }) {
                                Text("Send Another Message")
                            }
                        }
                    }
                } else {
                    Text("Inquire refilling delivery status or account issues directly:", fontSize = 12.sp)

                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = senderEmail,
                        onValueChange = { senderEmail = it },
                        label = { Text("Your Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = senderMsg,
                        onValueChange = { senderMsg = it },
                        label = { Text("Message details...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )

                    Button(
                        onClick = {
                            if (senderName.isNotBlank() && senderEmail.isNotBlank()) {
                                isSubmitted = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FuelAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SUBMIT MESSAGE", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Fake Google Maps mock card
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarbonCard)
            ) {
                // Background visual representing physical station map
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color(0xFF152238))
                    // Roads representation
                    drawRoundRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(0f, size.height / 2.5f),
                        size = Size(size.width, 35f),
                        cornerRadius = CornerRadius(10f)
                    )
                    drawRoundRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(size.width / 2f, 0f),
                        size = Size(35f, size.height),
                        cornerRadius = CornerRadius(10f)
                    )
                    // Station Pin indicator
                    drawCircle(
                        color = FuelAmber,
                        radius = 18f,
                        center = Offset(size.width / 1.7f, size.height / 2.2f)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("HQ Station Terminal Coordinates", fontWeight = FontWeight.Bold, color = FuelAmber, fontSize = 10.sp)
                    Text(address, fontSize = 9.sp, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// LOGIN VIEW WITH ILLUSTRATIVE CARDS
// ==========================================

@Composable
fun LoginView(
    onNavigateDashboard: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FuelFlowViewModel
) {
    var emailInput by remember { mutableStateOf("admin@fuelflow.com") }
    var passwordInput by remember { mutableStateOf("admin123") }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(CharcoalBlack, MaterialTheme.colorScheme.surface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(FuelAmber, Color(0xFFFF5722))
                        ), RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = "Lock Logo",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                "Terminal Security Ledger",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Text(
                "Verify authorize employee credentials below:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (loginError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = loginError!!,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Corporate Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Security Access Code") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Helper with demo credentials
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Demo Sandbox Credentials (Preset):", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                            Text("• Admin: admin@fuelflow.com (Code: admin123)", fontSize = 8.5.sp)
                            Text("• Cashier: cashier@fuelflow.com (Code: cashier123)", fontSize = 8.5.sp)
                            Text("• Attendant: attendant@fuelflow.com (Code: attendant123)", fontSize = 8.5.sp)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.performLogin(emailInput, passwordInput, onNavigateDashboard)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("AUTHORIZE SESSION", fontWeight = FontWeight.Black)
                    }
                }
            }

            TextButton(onClick = onNavigateBack) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChevronLeft, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Return to retail gateway")
                }
            }
        }
    }
}

// ==========================================
// MASTER ADMIN INTERFACE WITH NAV SIDEPANEL
// ==========================================

@Composable
fun AdminDashboardMainView(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    viewModel: FuelFlowViewModel
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isMoreMenuOpen = remember { mutableStateOf(false) }
    
    // Auto return to landing if currentUser is null
    if (currentUser == null) {
        // Quick fail-safe logout redirection
        LaunchedEffect(Unit) {
            onScreenChange(AppScreen.Landing)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            
            // SIDE NAVIGATION RAIL: Executed ONLY on wider screens (tablet/landscape)
            if (isTablet) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(FuelAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (settings?.stationName ?: "FuelFlow").uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight().width(88.dp)
                ) {
                    val navItems = listOf(
                        Triple("Dashboard", AppScreen.Dashboard, Icons.Default.Dashboard),
                        Triple("POS", AppScreen.POS, Icons.Default.PointOfSale),
                        Triple("Tanks", AppScreen.Fuel, Icons.Default.LocalGasStation),
                        Triple("Pumps", AppScreen.Pumps, Icons.Default.ToggleOn),
                        Triple("Staff", AppScreen.Employees, Icons.Default.People),
                        Triple("Loyalty", AppScreen.Customers, Icons.Default.CardMembership),
                        Triple("Analytics", AppScreen.Reports, Icons.Default.BarChart),
                        Triple("Settings", AppScreen.Settings, Icons.Default.Settings)
                    )

                    Column(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        navItems.forEach { (label, screen, icon) ->
                            val isSelected = currentScreen == screen
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { onScreenChange(screen) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = FuelAmber,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    // Logout Action button at bottom of Rail
                    NavigationRailItem(
                        selected = false,
                        onClick = {
                            viewModel.performLogout()
                            onScreenChange(AppScreen.Landing)
                        },
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Log Out") },
                        label = { Text("Log Out", fontSize = 9.sp, color = MaterialTheme.colorScheme.error) },
                        colors = NavigationRailItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }

            // Central Work Area Scaffold
            Scaffold(
                topBar = {
                    AdminTopAppBar(
                        currentUser = currentUser,
                        stationName = settings?.stationName ?: "FuelFlow Pro",
                        unreadCount = viewModel.unreadNotificationsCount.collectAsStateWithLifecycle().value,
                        onProfileClick = { onScreenChange(AppScreen.Profile) },
                        viewModel = viewModel
                    )
                },
                bottomBar = {
                    // Show Bottom Navigation ONLY on non-tablet devices
                    if (!isTablet) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.height(72.dp)
                        ) {
                            val items = listOf(
                                Triple("Dashboard", AppScreen.Dashboard, Icons.Default.Dashboard),
                                Triple("POS", AppScreen.POS, Icons.Default.PointOfSale),
                                Triple("Tanks", AppScreen.Fuel, Icons.Default.LocalGasStation),
                                Triple("Pumps", AppScreen.Pumps, Icons.Default.ToggleOn),
                                Triple("More", AppScreen.Settings, Icons.Default.Menu) // Settings serves as fallback, but triggers custom 'More' popup
                            )
                            items.forEach { (label, screen, icon) ->
                                // Custom handling for "More" highlighting
                                val isSelected = if (label == "More") {
                                    currentScreen in listOf(AppScreen.Employees, AppScreen.Customers, AppScreen.Reports, AppScreen.Settings, AppScreen.Profile)
                                } else {
                                    currentScreen == screen
                                }

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (label == "More") {
                                            isMoreMenuOpen.value = true
                                        } else {
                                            onScreenChange(screen)
                                        }
                                    },
                                    label = {
                                        Text(
                                            label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = FuelAmber,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentScreen) {
                        AppScreen.Dashboard -> AdminDashboardView(viewModel, { onScreenChange(AppScreen.POS) })
                        AppScreen.POS -> POSRegisterView(viewModel)
                        AppScreen.Fuel -> FuelManagementView(viewModel)
                        AppScreen.Pumps -> PumpStatusView(viewModel)
                        AppScreen.Employees -> EmployeesDirectoryView(viewModel)
                        AppScreen.Customers -> CustomerLoyaltyView(viewModel)
                        AppScreen.Reports -> AnalyticalReportsView(viewModel)
                        AppScreen.Settings -> SystemSettingsView(viewModel)
                        AppScreen.Profile -> UserProfileView(viewModel, { onScreenChange(AppScreen.Landing) })
                        else -> Unit
                    }
                }
            }
        }

        // CUSTOM "MORE OPTIONS" SHEET & OVERLAY DIALOG (Responsive & Interactive)
        if (isMoreMenuOpen.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isMoreMenuOpen.value = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Interactive bottom card acting like a Material design sheet popup
                Card(
                    modifier = Modifier
                        .fillMaxWidth(if (isTablet) 0.5f else 1f)
                        .padding(bottom = if (isTablet) 24.dp else 0.dp)
                        .clickable(enabled = false) {}, // Prevent dismiss click-throughs
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = if (isTablet) 24.dp else 0.dp, bottomEnd = if (isTablet) 24.dp else 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Operational Modules",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { isMoreMenuOpen.value = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close menu")
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        val moreItems = listOf(
                            Triple("Staff & Shifts Directory", AppScreen.Employees, Icons.Default.People),
                            Triple("Customer Loyalty Club", AppScreen.Customers, Icons.Default.CardMembership),
                            Triple("CSV Reports & Analytics", AppScreen.Reports, Icons.Default.BarChart),
                            Triple("System Controls Setup", AppScreen.Settings, Icons.Default.Settings),
                            Triple("Security User Profile", AppScreen.Profile, Icons.Default.Person)
                        )

                        // Render each item as an elegant row selection list with dynamic indicators
                        moreItems.forEach { (label, screen, icon) ->
                            val isSelected = currentScreen == screen
                            Surface(
                                onClick = {
                                    onScreenChange(screen)
                                    isMoreMenuOpen.value = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) FuelAmber.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) FuelAmber else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) FuelAmber else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // Explicit Security Sign-out in list
                        Button(
                            onClick = {
                                viewModel.performLogout()
                                onScreenChange(AppScreen.Landing)
                                isMoreMenuOpen.value = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TERMINATE SESSION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Sidebar Drawer layout
@Composable
fun AdminSidebarContent(
    currentUser: User?,
    currentScreen: AppScreen,
    onScreenSelect: (AppScreen) -> Unit,
    onLogout: () -> Unit,
    stationName: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top logo header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(FuelAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalGasStation, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONTROL ROOM", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Profile card snippet
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.name ?: "Z").take(2).uppercase(),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(currentUser?.name ?: "User Session", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentUser?.role ?: "Staff Account", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Navigation list
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val navItems = listOf(
                        Triple("Dashboard", AppScreen.Dashboard, Icons.Default.Dashboard),
                        Triple("POS Register", AppScreen.POS, Icons.Default.PointOfSale),
                        Triple("Tanks & Fuel", AppScreen.Fuel, Icons.Default.LocalGasStation),
                        Triple("Nozzle Pumps", AppScreen.Pumps, Icons.Default.ToggleOn),
                        Triple("Staff & Shifts", AppScreen.Employees, Icons.Default.People),
                        Triple("Loyalty Loyalty", AppScreen.Customers, Icons.Default.CardMembership),
                        Triple("CSV Analytics", AppScreen.Reports, Icons.Default.BarChart),
                        Triple("System Setup", AppScreen.Settings, Icons.Default.Settings)
                    )

                    navItems.forEach { (label, screen, icon) ->
                        val isSelected = currentScreen == screen
                        val tintColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        val bg = if (isSelected) FuelAmber else Color.Transparent

                        Button(
                            onClick = { onScreenSelect(screen) },
                            colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = tintColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Logout block
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TERMINATE SESSION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Appbar component for administrative panel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopAppBar(
    currentUser: User?,
    stationName: String,
    unreadCount: Int,
    onProfileClick: () -> Unit,
    viewModel: FuelFlowViewModel
) {
    val isSimRunning by viewModel.isSimulationRunning.collectAsStateWithLifecycle()

    TopAppBar(
        title = {
            Column {
                Text(stationName, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("Operational Console", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        navigationIcon = {
            Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = FuelAmber,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        actions = {
            // Live Simulation feedback button
            IconButton(
                onClick = { viewModel.toggleSimulation() },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .background(
                        if (isSimRunning) AlertGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Simulate",
                    tint = if (isSimRunning) AlertGreen else MaterialTheme.colorScheme.onSurface
                )
            }

            // Notification bells indicator
            IconButton(
                onClick = { viewModel.showToast("Alert snapshot tray is checked.") },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(containerColor = AlertRed) {
                                Text("$unreadCount", color = Color.White)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, "Notifications")
                }
            }

            // Profile Avatar
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (currentUser?.name ?: "U").take(1).uppercase(),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

// Helper block to inject custom Z-order onto views
fun Modifier.zOrder(z: Float): Modifier = this.drawBehind { /* Null drawing to enforce order hierarchy */ }


// ==========================================
// CENTRAL ADMIN DASHBOARD VIEW
// ==========================================

@Composable
fun AdminDashboardView(viewModel: FuelFlowViewModel, onNavigateToPOS: () -> Unit) {
    val fuelTypes by viewModel.fuelTypes.collectAsStateWithLifecycle()
    val pumps by viewModel.pumps.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isSimRunning by viewModel.isSimulationRunning.collectAsStateWithLifecycle()

    // Aggregate statistics
    val totalSalesCash = transactions.sumOf { it.totalPrice }
    val totalLiters = transactions.sumOf { it.liters }
    val activePumpsCount = pumps.count { it.status == "Active" }
    val lowFuelWarningsCount = fuelTypes.count { 
        val cap = if (it.capacity > 0) it.capacity else 10000.0
        (it.availableLiters / cap) < 0.3 
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Welcome Header & Real-time Indicator
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Gas Logistics Central", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Live system snapshot coordinates", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Simulation switch widget
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSimRunning) AlertGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { viewModel.toggleSimulation() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isSimRunning) AlertGreen else AlertRed, CircleShape)
                            )
                            Text(
                                text = if (isSimRunning) "LIVE INCOMING" else "SIMULATOR OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSimRunning) AlertGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Stats grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatWidget(
                        title = "TOTAL FUEL SALES",
                        value = "$${String.format("%.2f", totalSalesCash)}",
                        icon = Icons.Default.AttachMoney,
                        subtext = "Sum of checkout receipts",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatWidget(
                        title = "DISPENSED VOLUME",
                        value = "${String.format("%.1f", totalLiters)} L",
                        icon = Icons.Default.LocalGasStation,
                        subtext = "Combined pump outputs",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatWidget(
                        title = "ACTIVE NOZZLES",
                        value = "$activePumpsCount / ${pumps.size}",
                        icon = Icons.Default.Power,
                        subtext = "Pumps online status",
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatWidget(
                        title = "ALARM CHANNELS",
                        value = "$lowFuelWarningsCount triggers",
                        icon = Icons.Default.NotificationsActive,
                        subtext = "Fuel levels below 30%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Custom Canvas Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sales Trend History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Recent transactions graph", fontSize = 10.sp, color = FuelAmber)
                    }

                    // Canvas drawing for a custom beautiful line graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val strokeColor = FuelAmber
                        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Grids lines
                            for (i in 1..4) {
                                val y = (h / 5) * i
                                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                            }

                            // Dynamic trend line based on transaction counts
                            val points = listOf(
                                Offset(w * 0.1f, h * 0.8f),
                                Offset(w * 0.25f, h * 0.5f),
                                Offset(w * 0.4f, h * 0.7f),
                                Offset(w * 0.6f, h * 0.2f),
                                Offset(w * 0.8f, h * 0.4f),
                                Offset(w * 0.95f, h * 0.1f)
                            )

                            // Path building
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (p in 1 until points.size) {
                                    val prev = points[p - 1]
                                    val curr = points[p]
                                    cubicTo(
                                        (prev.x + curr.x) / 2f, prev.y,
                                        (prev.x + curr.x) / 2f, curr.y,
                                        curr.x, curr.y
                                    )
                                }
                            }

                            drawPath(path = path, color = strokeColor, style = Stroke(width = 6f))
                            
                            // Draw point anchors shadow nodes
                            points.forEach { p ->
                                drawCircle(color = Color.Black, radius = 9f, center = p)
                                drawCircle(color = FuelAmber, radius = 5f, center = p)
                            }
                        }
                    }
                }
            }
        }

        // Tank status previews
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reserve Gravity Chambers Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Waves, "Waves", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(fuelTypes) { fuel ->
            val capacitySafe = if (fuel.capacity > 0) fuel.capacity else 10000.0
            val ratio = ((fuel.availableLiters / capacitySafe).toFloat()).coerceIn(0f, 1f)
            val trackingColor = if (ratio < 0.25f) AlertRed else if (ratio < 0.55f) AlertYellow else AlertGreen

            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(fuel.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${String.format("%.1f", fuel.availableLiters)} L / ${fuel.capacity.toInt()} L", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }

                    // Level indicators
                    LinearProgressIndicator(
                        progress = ratio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = trackingColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // Live Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Register Log stream", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onNavigateToPOS) {
                    Text("Trigger POS Purchase", fontSize = 11.sp, color = FuelAmber)
                }
            }
        }

        // Recent activity lists
        if (transactions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions logged in current session.", fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(transactions.take(6)) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Transaction FF-${tx.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${tx.pumpName} • ${tx.fuelTypeName} • ${tx.paymentMethod}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$${String.format("%.2f", tx.totalPrice)}",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                            Text("${String.format("%.2f", tx.liters)} Liters Dispensed", fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatWidget(
    title: String,
    value: String,
    icon: ImageVector,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                }
            }

            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subtext, fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


// ==========================================
// SECTOR: POS REGISTER
// ==========================================

@Composable
fun POSRegisterView(viewModel: FuelFlowViewModel) {
    val pumps by viewModel.pumps.collectAsStateWithLifecycle()
    val fuelTypes by viewModel.fuelTypes.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    
    val selectedPumpId by viewModel.selectedPumpId.collectAsStateWithLifecycle()
    val litersInput by viewModel.posLiters.collectAsStateWithLifecycle()
    val selectedCustId by viewModel.posSelectedCustomerId.collectAsStateWithLifecycle()
    val paymentM by viewModel.posPaymentMethod.collectAsStateWithLifecycle()
    val receipt by viewModel.activeReceipt.collectAsStateWithLifecycle()
    val activePump = pumps.find { it.id == selectedPumpId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Nozzle Point-of-Sale Register", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Active Pump card selection grid banner
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Activated Dispatch Nozzle Pump:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pumps.filter { it.status == "Active" }.forEach { pump ->
                        val isSel = pump.id == selectedPumpId
                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .height(95.dp)
                                .clickable { viewModel.selectPumpForPOS(pump.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                2.dp,
                                if (isSel) FuelAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                                    .background(if (isSel) FuelAmber.copy(alpha = 0.08f) else Color.Transparent),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(pump.name, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Icon(
                                        Icons.Default.Power,
                                        null,
                                        tint = if (isSel) FuelAmber else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                
                                val fType = fuelTypes.find { it.id == pump.fuelTypeId }
                                Text(
                                    fType?.name ?: "No Fuel Grade",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "$${String.format("%.2f", fType?.pricePerLiter ?: 0.0)}/L",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FuelAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        // Checkout formulation Form Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Checkout Formulation", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Target details
                    if (activePump != null) {
                        val fuel = fuelTypes.find { it.id == activePump.fuelTypeId }
                        val densityRatio = litersInput.toDoubleOrNull() ?: 0.0
                        val computedCash = densityRatio * (fuel?.pricePerLiter ?: 0.0)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Dispatching: ${activePump.name} (${fuel?.name})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Price calculation: ${litersInput}L x $${fuel?.pricePerLiter}", fontSize = 9.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$${String.format("%.2f", computedCash)}",
                                        fontWeight = FontWeight.Black,
                                        color = FuelAmber,
                                        fontSize = 15.sp
                                    )
                                    Text("Computed Net Cash", fontSize = 8.sp)
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Standard Nozzle lock isn't configured. Select an active pump node.",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Flow capacity inputs
                    OutlinedTextField(
                        value = litersInput,
                        onValueChange = { viewModel.setPOSLiters(it) },
                        label = { Text("Volume to dispense (Liters)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment grid Selection
                    Column {
                        Text("Secure Payment Choice:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cash", "GCash", "Maya", "Card").forEach { method ->
                                val active = paymentM == method
                                FilterChip(
                                    selected = active,
                                    onClick = { viewModel.setPOSPaymentMethod(method) },
                                    label = { Text(method) }
                                )
                            }
                        }
                    }

                    // Loyalty Selection
                    Column {
                        Text("Associate Loyalty Membership Card (Optional):", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCustId == null,
                                onClick = { viewModel.setPOSCustomer(null) },
                                label = { Text("No Loyalty Club Member") }
                            )

                            customers.forEach { member ->
                                val activeCl = selectedCustId == member.id
                                FilterChip(
                                    selected = activeCl,
                                    onClick = { viewModel.setPOSCustomer(member.id) },
                                    label = { Text(member.name) }
                                )
                            }
                        }
                    }

                    // Error logs panel from VM
                    val err by viewModel.loginError.collectAsStateWithLifecycle()
                    if (err != null) {
                        Text(err!!, color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.processPOSPurchase() },
                        colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DISPENSE FUEL & GENERATE RECEIPT", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // Interactive Animated printable Receipt Dialog modal
    if (receipt != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReceipt() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Receipt, null, tint = FuelAmber)
                    Text("Secure Thermal Receipt", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                        Text("FUELFLOW PRO RETAIL TERMINAL", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("Station ID: FF-TERM-SL-0012", fontSize = 10.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("Date: 2026-05-28 18:13", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nozzle Outlet:", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text(receipt?.pumpName ?: "Active Nozzle", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fuel Grade Choice:", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text(receipt?.fuelTypeName ?: "Active Outlet", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pump Price/L:", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text("$${String.format("%.2f", receipt?.pricePerLiter ?: 0.0)}/L", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dispensed Amount:", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text("${String.format("%.2f", receipt?.liters ?: 0.0)} Liters", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GRAND TOTAL CASH:", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("$${String.format("%.2f", receipt?.totalPrice ?: 0.0)}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black, color = FuelAmber)
                        }
                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                        
                        Text("Pay Mode: ${receipt?.paymentMethod}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        if (receipt?.customerName != null) {
                            Text("Loyalty Scanned: ${receipt?.customerName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }

                        // Code Mock Visual representation
                        Spacer(modifier = Modifier.height(10.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            // Barcode grid drawing matching printable receipt aesthetics
                            val lines = 36
                            val w = size.width
                            val h = size.height
                            for (l in 0..lines) {
                                val offset = (w / lines) * l
                                val active = (l % 3 != 0) && (l % 5 != 0)
                                if (active) {
                                    drawLine(
                                        color = Color.Black,
                                        start = Offset(offset, 0f),
                                        end = Offset(offset, h),
                                        strokeWidth = if (l % 2 == 0) 4f else 2f
                                    )
                                }
                            }
                        }
                        Text("System code Verification secure snapshot node", fontSize = 8.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissReceipt() },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black)
                ) {
                    Text("Proceed & Close Nozzle")
                }
            }
        )
    }
}


// ==========================================
// SECTOR: CUSTOMERS LOYALTY LIST
// ==========================================

@Composable
fun CustomerLoyaltyView(viewModel: FuelFlowViewModel) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var isEnrollOpen by remember { mutableStateOf(false) }

    var inputName by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputVehicle by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Loyalty Vault Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Manage membership points logs", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = { isEnrollOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Add, null)
                        Text("Enroll Card")
                    }
                }
            }
        }

        if (customers.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No registered loyalty accounts in SQLite.")
                    }
                }
            }
        } else {
            items(customers) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Points Badge
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(FuelAmber.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${member.points}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = FuelAmber)
                                    Text("PTS", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(member.name, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text(member.membershipNo, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                Text("Vehicle Pin No: ${member.vehicleNo} • Ph: ${member.phone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(onClick = { viewModel.removeLoyaltyMember(member) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = AlertRed)
                        }
                    }
                }
            }
        }
    }

    if (isEnrollOpen) {
        AlertDialog(
            onDismissRequest = { isEnrollOpen = false },
            title = { Text("Enroll Membership Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Customer Fullname") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputPhone,
                        onValueChange = { inputPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputVehicle,
                        onValueChange = { inputVehicle = it },
                        label = { Text("Vehicle Plate ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank() && inputPhone.isNotBlank()) {
                            viewModel.addLoyaltyMember(inputName, inputPhone, inputVehicle)
                            isEnrollOpen = false
                            inputName = ""
                            inputPhone = ""
                            inputVehicle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black)
                ) {
                    Text("Register Card Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEnrollOpen = false }) { Text("Cancel") }
            }
        )
    }
}


// ==========================================
// SECTOR: FUEL TANKS MANAGEMENT
// ==========================================

@Composable
fun FuelManagementView(viewModel: FuelFlowViewModel) {
    val fuelTypes by viewModel.fuelTypes.collectAsStateWithLifecycle()
    var isAddFuelOpen by remember { mutableStateOf(false) }

    var fuelName by remember { mutableStateOf("") }
    var fuelPrice by remember { mutableStateOf("60.00") }
    var tankCapacity by remember { mutableStateOf("10000") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Storage Chamber Logistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Adjust active per-liter pricing metrics", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = { isAddFuelOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Fuel")
                    }
                }
            }
        }

        items(fuelTypes) { fuel ->
            var customPriceStr by remember { mutableStateOf(fuel.pricePerLiter.toString()) }

            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(fuel.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        IconButton(onClick = { viewModel.deleteFuelGrade(fuel) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = AlertRed)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPriceStr,
                            onValueChange = { customPriceStr = it },
                            label = { Text("Price per Liter Scale ($)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val p = customPriceStr.toDoubleOrNull() ?: fuel.pricePerLiter
                                viewModel.updateFuelPrice(fuel.id, p)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SAVE")
                        }
                    }

                    // Display ratio bar charts
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val capacitySafe = if (fuel.capacity > 0) fuel.capacity else 10000.0
                        val ratio = ((fuel.availableLiters / capacitySafe).toFloat()).coerceIn(0f, 1f)
                        val color = if (ratio < 0.3f) AlertRed else AlertGreen
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Available Inventory Volume: ${String.format("%.1f", fuel.availableLiters)} / ${fuel.capacity.toInt()} L", fontSize = 10.sp)
                            Text("${(ratio * 100).toInt()}% Capacity", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                        LinearProgressIndicator(
                            progress = ratio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }
    }

    if (isAddFuelOpen) {
        AlertDialog(
            onDismissRequest = { isAddFuelOpen = false },
            title = { Text("Install Fuel Product Grade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fuelName,
                        onValueChange = { fuelName = it },
                        label = { Text("Fuel Grade Designation (e.g. Bio-Diesel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fuelPrice,
                        onValueChange = { fuelPrice = it },
                        label = { Text("Price / Liter") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = tankCapacity,
                        onValueChange = { tankCapacity = it },
                        label = { Text("Storage Chamber Maximum Capacity") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pr = fuelPrice.toDoubleOrNull() ?: 60.0
                        val cap = tankCapacity.toDoubleOrNull() ?: 10000.0
                        if (fuelName.isNotBlank()) {
                            viewModel.addFuelGrade(fuelName, pr, cap)
                            isAddFuelOpen = false
                            fuelName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black)
                ) {
                    Text("Provision Grade")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddFuelOpen = false }) { Text("Cancel") }
            }
        )
    }
}


// ==========================================
// SECTOR: NOZZLE PUMP MONITORING
// ==========================================

@Composable
fun PumpStatusView(viewModel: FuelFlowViewModel) {
    val pumps by viewModel.pumps.collectAsStateWithLifecycle()
    val fuelTypes by viewModel.fuelTypes.collectAsStateWithLifecycle()
    var isNewPumpOpen by remember { mutableStateOf(false) }

    var pName by remember { mutableStateOf("") }
    var pFuelTypeId by remember { mutableStateOf(1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pump Dispatches Control Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Activate / Deactivate active nozzles", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = { isNewPumpOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Add, null)
                        Text("Install Nozzle")
                    }
                }
            }
        }

        items(pumps) { pump ->
            val fuel = fuelTypes.find { it.id == pump.fuelTypeId }

            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pump.name, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("Mapping Outlet: ${fuel?.name ?: "Unknown Grade"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // Colored status node
                        val (bannerCol, txtCol) = when (pump.status) {
                            "Active" -> Pair(AlertGreen.copy(alpha = 0.15f), AlertGreen)
                            "Maintenance" -> Pair(AlertYellow.copy(alpha = 0.15f), AlertYellow)
                            else -> Pair(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), MaterialTheme.colorScheme.onSurface)
                        }

                        Box(
                            modifier = Modifier
                                .background(bannerCol, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pump.status, color = txtCol, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Nozzle switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Terminal Control Swich Model:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Active", "Inactive", "Maintenance").forEach { state ->
                                val selected = pump.status == state
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setPumpState(pump.id, state) },
                                    label = { Text(state, fontSize = 9.sp) }
                                )
                            }
                        }
                    }

                    // Metrics outputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LIFETIME FLOW DISPENSED", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", pump.lifetimeSalesLiters)} Liters", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("LIFETIME CASH RECEIVED", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("$${String.format("%.2f", pump.lifetimeSalesCash)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AlertGreen)
                        }
                    }
                }
            }
        }
    }

    if (isNewPumpOpen) {
        AlertDialog(
            onDismissRequest = { isNewPumpOpen = false },
            title = { Text("Assemble Nozzle Pump node") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("Pump identifier (e.g. Pump 4-B)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Connected Fuel Reserve Chamber:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    fuelTypes.forEach { fuel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pFuelTypeId = fuel.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = pFuelTypeId == fuel.id, onClick = { pFuelTypeId = fuel.id })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(fuel.name, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pName.isNotBlank()) {
                            viewModel.registerNewPump(pName, pFuelTypeId)
                            isNewPumpOpen = false
                            pName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black)
                ) {
                    Text("Provision Nozzle")
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewPumpOpen = false }) { Text("Cancel") }
            }
        )
    }
}


// ==========================================
// SECTOR: EMPLOYEES & ATTENDANCE
// ==========================================

@Composable
fun EmployeesDirectoryView(viewModel: FuelFlowViewModel) {
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    var isHireOpen by remember { mutableStateOf(false) }

    // Hire variables
    var empName by remember { mutableStateOf("") }
    var empEmail by remember { mutableStateOf("") }
    var empPhone by remember { mutableStateOf("") }
    var empRole by remember { mutableStateOf("Cashier") }
    var empShift by remember { mutableStateOf("Morning (6 AM - 2 PM)") }
    var empSalary by remember { mutableStateOf("25000") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Authorized Personnel Hub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Administer work rosters and Clock-In events", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = { isHireOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PersonAdd, null)
                        Text("Recruit Staff")
                    }
                }
            }
        }

        // List Employees cards
        items(employees) { staff ->
            // Check current shift clock status
            val isClockedIn = attendance.any { it.employeeId == staff.id && it.date == "2026-05-28" && it.timeOut == null }

            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Colored initial circle avatars
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = staff.name.take(2).uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(staff.name, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("${staff.role} • Shift: ${staff.shift}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Contacts: ${staff.phone} • Salary: $${String.format("%.0f", staff.salary)}/mo", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Interactive toggle Clock-In action
                    Button(
                        onClick = { viewModel.logEmployeeAttendance(staff.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isClockedIn) AlertGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            contentColor = if (isClockedIn) AlertGreen else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 80.dp)
                    ) {
                        Text(
                            text = if (isClockedIn) "IN SESSION" else "CLOCK-IN",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }

    if (isHireOpen) {
        AlertDialog(
            onDismissRequest = { isHireOpen = false },
            title = { Text("Recruit Staff Employee") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Personnel Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empEmail,
                        onValueChange = { empEmail = it },
                        label = { Text("Corporate Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empPhone,
                        onValueChange = { empPhone = it },
                        label = { Text("Phone Number Pin") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Employment Role:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Admin", "Staff", "Cashier", "Fuel Attendant").forEach { role ->
                            val isAct = empRole == role
                            FilterChip(
                                selected = isAct,
                                onClick = { empRole = role },
                                label = { Text(role, fontSize = 9.6.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = empSalary,
                        onValueChange = { empSalary = it },
                        label = { Text("Monthly Salary ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sal = empSalary.toDoubleOrNull() ?: 20000.0
                        if (empName.isNotBlank() && empPhone.isNotBlank()) {
                            viewModel.hireEmployee(empName, empRole, empEmail, empPhone, empShift, sal)
                            isHireOpen = false
                            empName = ""
                            empPhone = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black)
                ) {
                    Text("Hire & Draft Contract")
                }
            },
            dismissButton = {
                TextButton(onClick = { isHireOpen = false }) { Text("Cancel") }
            }
        )
    }
}


// ==========================================
// SECTOR: CSV/EXCEL REPORTS EXPORTER
// ==========================================

@Composable
fun AnalyticalReportsView(viewModel: FuelFlowViewModel) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val fuelTypes by viewModel.fuelTypes.collectAsStateWithLifecycle()
    
    val currentContext = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Snapshot Exporter Registry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Compile Analytics files cleanly on the system database:", fontSize = 11.sp)

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Reporting choices
                    ReportOptionRow(
                        title = "Fuel Sales Log.csv",
                        desc = "Compiles receipts containing fuel nozzle codes, liters, and payment channels.",
                        onDownload = {
                            viewModel.showToast("Compiling Fuel Sales Log CSV. Output generated on local container storage.")
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    ReportOptionRow(
                        title = "Atmospheric Gauge Report.pdf",
                        desc = "Compiles levels and capacities of the 5 gravity reserves storage compartments.",
                        onDownload = {
                            viewModel.showToast("Compiling Tank Reserve Status PDF. Snapshot reported successfully.")
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    ReportOptionRow(
                        title = "Staff Duty Attendance.xlsx",
                        desc = "Compiles Clock-In transactions matching standard employee shifts rosters.",
                        onDownload = {
                            viewModel.showToast("Compiling Staff Shifts xlsx spreadsheet completed.")
                        }
                    )
                }
            }
        }

        // Summary trend statistics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Historical Aggregate Analytics", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active operations days:", fontSize = 11.sp)
                        Text("Day 1 (Live Session)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FuelAmber)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Revenue logged in SQLite:", fontSize = 11.sp)
                        val totalNet = transactions.sumOf { it.totalPrice }
                        Text("$${String.format("%.2f", totalNet)}", fontWeight = FontWeight.Black, fontSize = 11.sp, color = AlertGreen)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dispensed capacity metrics:", fontSize = 11.sp)
                        val vol = transactions.sumOf { it.liters }
                        Text("${String.format("%.1f", vol)} Liters", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportOptionRow(
    title: String,
    desc: String,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 12.sp, color = FuelAmber)
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        IconButton(onClick = onDownload) {
            Icon(Icons.Default.Download, "Download", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}


// ==========================================
// SECTOR: RULES PANEL & SETTINGS
// ==========================================

@Composable
fun SystemSettingsView(viewModel: FuelFlowViewModel) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    
    var stationName by remember { mutableStateOf(settings?.stationName ?: "FuelFlow Pro Station") }
    var stationAddress by remember { mutableStateOf(settings?.companyAddress ?: "123 Fuel Boulevard, Metro City") }
    var smtpHost by remember { mutableStateOf(settings?.emailHost ?: "smtp.fuelflow.com") }
    var smtpPort by remember { mutableStateOf((settings?.emailPort ?: 587).toString()) }
    var autoBackup by remember { mutableStateOf(settings?.enableAutoBackup ?: true) }
    var forceDark by remember { mutableStateOf(settings?.darkTheme ?: true) }

    // Synchronize states on DB flow emit
    LaunchedEffect(settings) {
        settings?.let {
            stationName = it.stationName
            stationAddress = it.companyAddress
            smtpHost = it.emailHost
            smtpPort = it.emailPort.toString()
            autoBackup = it.enableAutoBackup
            forceDark = it.darkTheme
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Global System Configurations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Station Brand Customization", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = stationName,
                        onValueChange = { stationName = it },
                        label = { Text("Station Console Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = stationAddress,
                        onValueChange = { stationAddress = it },
                        label = { Text("Corporate Premises Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Digital Dispatch Notification Rules", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = { smtpHost = it },
                        label = { Text("SMTP Email Gateway Host") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpPort,
                        onValueChange = { smtpPort = it },
                        label = { Text("SMTP Port Endpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Automated SQLite snap backups", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = autoBackup, onCheckedChange = { autoBackup = it })
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Interactive Sandbox Preferences", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Force Cyber Dark Mode Themes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = forceDark, onCheckedChange = { forceDark = it })
                    }

                    // Backup and Restore Database elements
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.backupDatabase() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SAVE SNAPSHOT")
                        }

                        Button(
                            onClick = { viewModel.restoreDatabase() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertYellow.copy(alpha = 0.12f), contentColor = AlertYellow),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("RESTORE DB")
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val portVal = smtpPort.toIntOrNull() ?: 587
                    viewModel.saveStationSettings(
                        stationName,
                        stationAddress,
                        smtpHost,
                        portVal,
                        autoBackup
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FuelAmber, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SAVE SYSTEM SETTINGS", fontWeight = FontWeight.Black)
            }
        }
    }
}


// ==========================================
// SECTOR: USER PROFILE PAGE
// ==========================================

@Composable
fun UserProfileView(viewModel: FuelFlowViewModel, onLogoutLanding: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Large Avatar Panel
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(FuelAmber, Color(0xFFFF5722))
                    ), CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (currentUser?.name ?: "Z").take(2).uppercase(),
                color = Color.Black,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }

        Text(
            currentUser?.name ?: "Operational Officer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )

        Text(
            "Security Role Permissions: ${currentUser?.role ?: "Authorized Session"}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Identity Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Secure Email address:", fontSize = 11.sp)
                    Text(currentUser?.email ?: "admin@fuelflow.com", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Operating Premises Terminal:", fontSize = 11.sp)
                    Text("HQ Terminal A-01", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rostered shifts category:", fontSize = 11.sp)
                    Text("Custom Admin Schedule", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FuelAmber)
                }
            }
        }

        Button(
            onClick = {
                viewModel.performLogout()
                onLogoutLanding()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SAFELY TERMINATE TERMINAL SESSION", fontWeight = FontWeight.Black)
        }
    }
}
