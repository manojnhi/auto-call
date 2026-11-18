package com.gramcall.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gramcall.ai.features.ai.AiHubScreen
import com.gramcall.ai.features.ai.OrderAssistantScreen
import com.gramcall.ai.features.calling.CallHistoryScreen
import com.gramcall.ai.features.calling.CampaignScreen
import com.gramcall.ai.features.calling.SimCallingScreen
import com.gramcall.ai.features.calling.VillageCallingScreen
import com.gramcall.ai.features.cities.CitiesScreen
import com.gramcall.ai.features.customers.CustomerEditScreen
import com.gramcall.ai.features.customers.CustomerProfileScreen
import com.gramcall.ai.features.customers.CustomersScreen
import com.gramcall.ai.features.dashboard.DashboardScreen
import com.gramcall.ai.features.orders.OrderDetailScreen
import com.gramcall.ai.features.orders.OrderEditScreen
import com.gramcall.ai.features.orders.OrdersScreen
import com.gramcall.ai.features.products.ProductsScreen
import com.gramcall.ai.features.reports.ReportsScreen
import com.gramcall.ai.features.settings.BackupScreen
import com.gramcall.ai.features.settings.SettingsScreen
import com.gramcall.ai.features.villages.VillagesScreen
import kotlinx.coroutines.launch

data class MenuItem(val label: String, val icon: ImageVector, val route: String)

val menuItems = listOf(
    MenuItem("होम", Icons.Filled.Home, "home"),
    MenuItem("गाँव प्रबंधन", Icons.Filled.Place, "villages"),
    MenuItem("शहर प्रबंधन", Icons.Filled.LocationCity, "cities"),
    MenuItem("ग्राहक प्रबंधन", Icons.Filled.People, "customers"),
    MenuItem("SIM कॉलिंग", Icons.Filled.Call, "sim"),
    MenuItem("AI वॉइस एजेंट", Icons.Filled.Mic, "ai"),
    MenuItem("प्रोडक्ट प्रबंधन", Icons.Filled.Inventory2, "products"),
    MenuItem("ऑर्डर प्रबंधन", Icons.Filled.Receipt, "orders"),
    MenuItem("रिपोर्ट", Icons.Filled.Assessment, "reports"),
    MenuItem("सेटिंग्स", Icons.Filled.Settings, "settings")
)

private val topRoutes = menuItems.map { it.route }.toSet()

private fun titleFor(route: String?): String = when {
    route == null -> ""
    route == "home" -> "GramCall AI"
    route == "villages" -> "गाँव प्रबंधन"
    route == "cities" -> "शहर प्रबंधन"
    route == "customers" -> "ग्राहक प्रबंधन"
    route == "customer/{id}" -> "ग्राहक प्रोफ़ाइल"
    route == "customer/edit/{id}" -> "ग्राहक"
    route == "sim" -> "SIM कॉलिंग"
    route == "calling/village" -> "गाँव / शहर कॉल अभियान"
    route == "calling/campaign" -> "कॉल अभियान"
    route == "calling/history" -> "कॉल हिस्ट्री"
    route == "ai" -> "AI वॉइस एजेंट"
    route.startsWith("assistant") -> "ऑर्डर सहायक"
    route == "products" -> "प्रोडक्ट प्रबंधन"
    route == "orders" -> "ऑर्डर प्रबंधन"
    route == "order/{id}" -> "ऑर्डर विवरण"
    route.startsWith("order/edit") -> "ऑर्डर"
    route == "reports" -> "रिपोर्ट"
    route == "settings" -> "सेटिंग्स"
    route == "backup" -> "बैकअप"
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    session: SessionViewModel,
    vm: AppViewModel = viewModel(),
    cvm: CallingViewModel = viewModel(),
    bvm: BizViewModel = viewModel(),
    bkvm: BackupViewModel = viewModel()
) {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
    LaunchedEffect(Unit) { cvm.messages.collect { snack.showSnackbar(it) } }
    LaunchedEffect(Unit) { bvm.messages.collect { if (it.isNotEmpty()) snack.showSnackbar(it) } }
    LaunchedEffect(Unit) { bkvm.messages.collect { snack.showSnackbar(it) } }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val isTop = route in topRoutes

    fun goTop(r: String) {
        nav.navigate(r) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val title = if (route == "customer/edit/{id}") {
        if ((entry?.arguments?.getLong("id") ?: 0L) == 0L) "नया ग्राहक" else "ग्राहक संपादित करें"
    } else if (route?.startsWith("order/edit") == true) {
        if ((entry?.arguments?.getLong("id") ?: 0L) == 0L) "नया ऑर्डर" else "ऑर्डर संपादित करें"
    } else titleFor(route)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTop,
        drawerContent = {
            ModalDrawerSheet {
                Text("GramCall AI", style = MaterialTheme.typography.headlineSmall, color = Green, modifier = Modifier.padding(24.dp))
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, null) },
                        selected = route == item.route,
                        onClick = { scope.launch { drawerState.close() }; goTop(item.route) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (isTop) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Filled.Menu, "मेनू") }
                        } else {
                            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "वापस") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Navy,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { pad ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
                composable("home") { DashboardScreen(vm, bvm) }
                composable("villages") {
                    VillagesScreen(vm, onShowCustomers = { id ->
                        vm.customerVillageFilter.value = id; vm.customerCityFilter.value = null; goTop("customers")
                    })
                }
                composable("cities") {
                    CitiesScreen(vm, onShowCustomers = { id ->
                        vm.customerCityFilter.value = id; vm.customerVillageFilter.value = null; goTop("customers")
                    })
                }
                composable("customers") {
                    CustomersScreen(vm, onOpen = { nav.navigate("customer/$it") }, onAdd = { nav.navigate("customer/edit/0") })
                }
                composable("customer/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    val id = e.arguments?.getLong("id") ?: 0L
                    CustomerProfileScreen(
                        vm, bvm, id,
                        onEdit = { nav.navigate("customer/edit/$id") },
                        onDeleted = { nav.popBackStack() },
                        onNewOrder = { nav.navigate("order/edit/0?customerId=$id") },
                        onOpenOrder = { nav.navigate("order/$it") }
                    )
                }
                composable("customer/edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    CustomerEditScreen(vm, e.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
                }

                // ---- calling ----
                composable("sim") {
                    SimCallingScreen(
                        cvm,
                        onVillageCalling = { nav.navigate("calling/village") },
                        onHistory = { nav.navigate("calling/history") },
                        onOpenCampaign = { nav.navigate("calling/campaign") }
                    )
                }
                composable("calling/village") {
                    VillageCallingScreen(
                        vm, cvm,
                        onStarted = { nav.navigate("calling/campaign") { popUpTo("sim") } },
                        onOpenActive = { nav.navigate("calling/campaign") { popUpTo("sim") } }
                    )
                }
                composable("calling/campaign") {
                    CampaignScreen(
                        cvm, onClose = { nav.popBackStack() },
                        onTakeOrder = { cust, camp -> nav.navigate("assistant?customerId=$cust&campaignId=$camp") }
                    )
                }
                composable("calling/history") { CallHistoryScreen(cvm) }

                // ---- AI / products / orders ----
                composable("ai") { AiHubScreen(bvm, onOpenAssistant = { nav.navigate("assistant") }) }
                composable(
                    "assistant?customerId={customerId}&campaignId={campaignId}",
                    arguments = listOf(
                        navArgument("customerId") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("campaignId") { type = NavType.LongType; defaultValue = 0L }
                    )
                ) { e ->
                    OrderAssistantScreen(
                        bvm, e.arguments?.getLong("customerId") ?: 0L, e.arguments?.getLong("campaignId") ?: 0L,
                        onDone = { nav.popBackStack() }
                    )
                }
                composable("products") { ProductsScreen(bvm) }
                composable("orders") {
                    OrdersScreen(bvm, onOpen = { nav.navigate("order/$it") }, onNew = { nav.navigate("order/edit/0") })
                }
                composable("order/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    val id = e.arguments?.getLong("id") ?: 0L
                    OrderDetailScreen(bvm, id, onEdit = { nav.navigate("order/edit/$id") })
                }
                composable(
                    "order/edit/{id}?customerId={customerId}&campaignId={campaignId}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("customerId") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("campaignId") { type = NavType.LongType; defaultValue = 0L }
                    )
                ) { e ->
                    OrderEditScreen(
                        bvm, e.arguments?.getLong("id") ?: 0L,
                        e.arguments?.getLong("customerId") ?: 0L, e.arguments?.getLong("campaignId") ?: 0L,
                        onDone = { id -> nav.navigate("order/$id") { popUpTo("orders") } }
                    )
                }

                composable("reports") { ReportsScreen(bvm, vm) }
                composable("settings") {
                    SettingsScreen(
                        bvm, vm, session,
                        onBackup = { nav.navigate("backup") }, onSim = { goTop("sim") }
                    )
                }
                composable("backup") { BackupScreen(bkvm) }
            }
        }
    }
}
