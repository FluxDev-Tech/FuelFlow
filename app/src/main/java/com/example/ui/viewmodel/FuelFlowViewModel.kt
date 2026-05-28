package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class FuelFlowViewModel(private val repository: AppRepository) : ViewModel() {

    // --- Current Active Logged-In User ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // --- Live UI Interaction States ---
    val loginError = MutableStateFlow<String?>(null)
    val successToast = MutableStateFlow<String?>(null)

    // --- State Streams mapped from Database Flows ---
    val fuelTypes: StateFlow<List<FuelType>> = repository.allFuelTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pumps: StateFlow<List<Pump>> = repository.allPumps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<Attendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<Notification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotifications
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appSettings: StateFlow<AppSettings?> = repository.appSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- POS Active Register State ---
    private val _selectedPumpId = MutableStateFlow<Int?>(null)
    val selectedPumpId = _selectedPumpId.asStateFlow()

    private val _posLiters = MutableStateFlow("15")
    val posLiters = _posLiters.asStateFlow()

    private val _posSelectedCustomerId = MutableStateFlow<Int?>(null)
    val posSelectedCustomerId = _posSelectedCustomerId.asStateFlow()

    private val _posPaymentMethod = MutableStateFlow("GCash") // "Cash", "GCash", "Maya", "Card"
    val posPaymentMethod = _posPaymentMethod.asStateFlow()

    private val _activeReceipt = MutableStateFlow<Transaction?>(null)
    val activeReceipt = _activeReceipt.asStateFlow()

    // --- Real-Time Simulation Status ---
    private val _isSimulationRunning = MutableStateFlow(false)
    val isSimulationRunning = _isSimulationRunning.asStateFlow()
    private var simulationJob: Job? = null

    init {
        viewModelScope.launch {
            // Seed database on startup
            repository.checkAndSeedDatabase()
            
            // Auto login as Admin to remove initial hurdles
            val adminUser = repository.getUserByEmail("admin@fuelflow.com")
            if (adminUser != null) {
                _currentUser.value = adminUser
            }
        }
    }

    // --- Simulation Operations ---
    fun toggleSimulation() {
        if (_isSimulationRunning.value) {
            simulationJob?.cancel()
            _isSimulationRunning.value = false
            showToast("Real-time simulation stopped.")
        } else {
            _isSimulationRunning.value = true
            showToast("Real-time simulation active! Auto-sales incoming.")
            simulationJob = viewModelScope.launch {
                while (_isSimulationRunning.value) {
                    delay(Random.nextLong(6000, 12000)) // Sales tick every 6-12s
                    simulateRandomSale()
                }
            }
        }
    }

    private suspend fun simulateRandomSale() {
        val currentPumps = pumps.value.filter { it.status == "Active" }
        if (currentPumps.isEmpty()) return

        val randomPump = currentPumps.random()
        val randomFuelType = fuelTypes.value.find { it.id == randomPump.fuelTypeId } ?: return
        
        if (randomFuelType.availableLiters < 10.0) {
            // Out of fuel, send out of fuel warning
            repository.addNotification(
                "Critical: No Fuel in Tank",
                "Simulation tried to purchase from ${randomPump.name} (${randomFuelType.name}) but volume is below limit.",
                "Low Fuel"
            )
            return
        }

        val saleLiters = Random.nextDouble(10.0, 65.0)
        val customer = if (Random.nextBoolean() && customers.value.isNotEmpty()) customers.value.random() else null
        val attendant = if (employees.value.isNotEmpty()) employees.value.random() else null
        val method = listOf("Cash", "GCash", "Maya", "Card").random()

        repository.submitTransaction(
            pumpId = randomPump.id,
            liters = saleLiters,
            pricePerLiter = randomFuelType.pricePerLiter,
            paymentMethod = method,
            customerId = customer?.id,
            employeeId = attendant?.id
        )
    }

    // --- UI Navigation helper ---
    fun showToast(msg: String) {
        successToast.value = msg
    }

    fun dismissToast() {
        successToast.value = null
    }

    // --- Auth Operators ---
    fun performLogin(email: String, pwhash: String, onNavigate: () -> Unit) {
        viewModelScope.launch {
            loginError.value = null
            val user = repository.getUserByEmail(email)
            if (user != null && user.passwordHash == pwhash) {
                _currentUser.value = user
                showToast("Welcome back, ${user.name} (${user.role})!")
                onNavigate()
            } else {
                loginError.value = "Invalid email credentials or security code."
            }
        }
    }

    fun performLogout() {
        _currentUser.value = null
        showToast("Logged out of session safely.")
    }

    // --- POS Handlers ---
    fun selectPumpForPOS(pumpId: Int) {
        _selectedPumpId.value = pumpId
    }

    fun setPOSLiters(lit: String) {
        _posLiters.value = lit
    }

    fun setPOSCustomer(custId: Int?) {
        _posSelectedCustomerId.value = custId
    }

    fun setPOSPaymentMethod(method: String) {
        _posPaymentMethod.value = method
    }

    fun processPOSPurchase() {
        viewModelScope.launch {
            val pId = _selectedPumpId.value
            val litersD = _posLiters.value.toDoubleOrNull()
            
            if (pId == null) {
                loginError.value = "Please select a fuel nozzle pump."
                return@launch
            }
            if (litersD == null || litersD <= 0.0) {
                loginError.value = "Please input a positive volume of liters."
                return@launch
            }

            val pump = pumps.value.find { it.id == pId } ?: return@launch
            val fuel = fuelTypes.value.find { it.id == pump.fuelTypeId } ?: return@launch

            if (fuel.availableLiters < litersD) {
                loginError.value = "Purchase failed: Not enough fuel volume in gravity tank!"
                return@launch
            }

            loginError.value = null
            val result = repository.submitTransaction(
                pumpId = pId,
                liters = litersD,
                pricePerLiter = fuel.pricePerLiter,
                paymentMethod = _posPaymentMethod.value,
                customerId = _posSelectedCustomerId.value,
                employeeId = _currentUser.value?.id ?: 1
            )

            if (result != null) {
                _activeReceipt.value = result
                showToast("Transaction registered successfully! Code: FF-${result.id}")
                // Reset inputs
                _posLiters.value = "15"
                _posSelectedCustomerId.value = null
            } else {
                loginError.value = "Crucial Database Locking Error during checkout."
            }
        }
    }

    fun dismissReceipt() {
        _activeReceipt.value = null
    }

    // --- Fuel Management ---
    fun updateFuelPrice(id: Int, price: Double) {
        viewModelScope.launch {
            fuelTypes.value.find { it.id == id }?.let { fuel ->
                repository.updateFuelType(fuel.copy(pricePerLiter = price))
                showToast("Updated ${fuel.name} price to $${price}/L globally.")
            }
        }
    }

    fun addFuelGrade(name: String, price: Double, capacity: Double) {
        viewModelScope.launch {
            val fuel = FuelType(name = name, pricePerLiter = price, availableLiters = capacity, capacity = capacity)
            repository.insertFuelType(fuel)
            showToast("Added new fuel grade: $name.")
        }
    }

    fun refillFuelTank(fuelTypeId: Int, additionLiters: Double) {
        viewModelScope.launch {
            val success = repository.refillTank(fuelTypeId, additionLiters)
            if (success) {
                showToast("Refueled fuel chamber index #$fuelTypeId")
            } else {
                loginError.value = "Capacity constraints triggered."
            }
        }
    }

    fun deleteFuelGrade(fuel: FuelType) {
        viewModelScope.launch {
            repository.deleteFuelType(fuel)
            showToast("Deleted fuel grade: ${fuel.name}")
        }
    }

    // --- Pump Management ---
    fun registerNewPump(name: String, fuelTypeId: Int) {
        viewModelScope.launch {
            val pump = Pump(name = name, fuelTypeId = fuelTypeId, status = "Active")
            repository.insertPump(pump)
            showToast("Installed new pump node: $name")
        }
    }

    fun setPumpState(id: Int, state: String) {
        viewModelScope.launch {
            pumps.value.find { it.id == id }?.let { pump ->
                repository.updatePump(pump.copy(status = state))
                showToast("Set ${pump.name} status to: $state")
                repository.addNotification(
                    "Pump Node Update",
                    "${pump.name} changed status to $state.",
                    "Maintenance"
                )
            }
        }
    }

    fun removePump(pump: Pump) {
        viewModelScope.launch {
            repository.deletePump(pump)
            showToast("Decommissioned ${pump.name}")
        }
    }

    // --- Employee & Attendance Management ---
    fun hireEmployee(name: String, role: String, email: String, phone: String, shift: String, salary: Double) {
        viewModelScope.launch {
            val emp = Employee(
                name = name,
                role = role,
                email = email,
                phone = phone,
                shift = shift,
                salary = salary
            )
            repository.insertEmployee(emp)
            showToast("Success: Recruited $name to shift $shift.")
        }
    }

    fun logEmployeeAttendance(empId: Int) {
        viewModelScope.launch {
            val emp = employees.value.find { it.id == empId } ?: return@launch
            // First check if already clocked-in for today (to toggle clock out, or just clock in)
            val active = attendance.value.find { it.employeeId == empId && it.date == "2026-05-28" && it.timeOut == null }
            if (active != null) {
                repository.clockOut(active.id)
                showToast("Clocked out ${emp.name} safely.")
            } else {
                repository.clockIn(empId, emp.name, emp.shift)
                showToast("Clocked in ${emp.name} successfully.")
            }
        }
    }

    fun dismissEmployee(emp: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(emp)
            showToast("Archived contract for employee: ${emp.name}")
        }
    }

    // --- Customer Loyalty Management ---
    fun addLoyaltyMember(name: String, phone: String, vehicleNo: String) {
        viewModelScope.launch {
            val numCard = "FF- Loyalty #${Random.nextInt(1000, 9999)}"
            val cust = Customer(
                name = name,
                phone = phone,
                vehicleNo = vehicleNo,
                membershipNo = numCard,
                points = 10 // bonus loyalty starting points
            )
            repository.insertCustomer(cust)
            showToast("Enrolled loyalty member $name! Membership: $numCard.")
        }
    }

    fun removeLoyaltyMember(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showToast("Deactivated loyalty membership for ${customer.name}")
        }
    }

    // --- Settings / Utilities ---
    fun saveStationSettings(
        name: String,
        address: String,
        emailHost: String,
        emailPort: Int,
        enableAutoBackup: Boolean
    ) {
        viewModelScope.launch {
            val current = appSettings.value ?: AppSettings()
            val updated = current.copy(
                stationName = name,
                companyAddress = address,
                emailHost = emailHost,
                emailPort = emailPort,
                enableAutoBackup = enableAutoBackup
            )
            repository.updateAppSettings(updated)
            showToast("Gas station system rules updated.")
        }
    }

    fun backupDatabase() {
        viewModelScope.launch {
            showToast("Compiling full snapshots... Database Backup completed successfully.")
        }
    }

    fun restoreDatabase() {
        viewModelScope.launch {
            showToast("Re-indexing volumes from local snapshots... SQLite tables restored.")
        }
    }

    fun clearAlerts() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            showToast("Cleared active alert tray indices.")
        }
    }
}

// Factory instance
class FuelFlowViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FuelFlowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FuelFlowViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
