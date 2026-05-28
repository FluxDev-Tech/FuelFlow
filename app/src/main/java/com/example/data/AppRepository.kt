package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AppRepository(private val dao: AppDao) {

    // --- Exposed Streams of Reactive Data ---
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val allEmployees: Flow<List<Employee>> = dao.getAllEmployees()
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val allFuelTypes: Flow<List<FuelType>> = dao.getAllFuelTypes()
    val allPumps: Flow<List<Pump>> = dao.getAllPumps()
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val allAttendance: Flow<List<Attendance>> = dao.getAllAttendance()
    val allNotifications: Flow<List<Notification>> = dao.getAllNotifications()
    val unreadNotifications: Flow<List<Notification>> = dao.getUnreadNotifications()
    val appSettingsFlow: Flow<AppSettings?> = dao.getSettingsFlow()

    // --- Authentication ---
    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        dao.getUserByEmail(email)
    }

    suspend fun registerUser(user: User): Long = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        dao.updateUser(user)
    }

    // --- Employees CRUD ---
    suspend fun insertEmployee(employee: Employee) = withContext(Dispatchers.IO) {
        dao.insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: Employee) = withContext(Dispatchers.IO) {
        dao.updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: Employee) = withContext(Dispatchers.IO) {
        dao.deleteEmployee(employee)
    }

    // --- Customers CRUD ---
    suspend fun insertCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.deleteCustomer(customer)
    }

    // --- Fuel Types CRUD ---
    suspend fun insertFuelType(fuelType: FuelType) = withContext(Dispatchers.IO) {
        dao.insertFuelType(fuelType)
    }

    suspend fun updateFuelType(fuelType: FuelType) = withContext(Dispatchers.IO) {
        dao.updateFuelType(fuelType)
    }

    suspend fun deleteFuelType(fuelType: FuelType) = withContext(Dispatchers.IO) {
        dao.deleteFuelType(fuelType)
    }

    // --- Pumps CRUD ---
    suspend fun insertPump(pump: Pump) = withContext(Dispatchers.IO) {
        dao.insertPump(pump)
    }

    suspend fun updatePump(pump: Pump) = withContext(Dispatchers.IO) {
        dao.updatePump(pump)
    }

    suspend fun deletePump(pump: Pump) = withContext(Dispatchers.IO) {
        dao.deletePump(pump)
    }

    // --- Transactions POS System ---
    suspend fun submitTransaction(
        pumpId: Int,
        liters: Double,
        pricePerLiter: Double,
        paymentMethod: String,
        customerId: Int?,
        employeeId: Int?
    ): Transaction? = withContext(Dispatchers.IO) {
        val pump = dao.getPumpById(pumpId) ?: return@withContext null
        val fuelType = dao.getFuelTypeById(pump.fuelTypeId) ?: return@withContext null

        if (fuelType.availableLiters < liters) {
            // Insufficient fuel in tank!
            return@withContext null
        }

        // Deduct fuel inventory
        val updatedFuelLiters = (fuelType.availableLiters - liters).coerceAtLeast(0.0)
        dao.updateFuelType(fuelType.copy(availableLiters = updatedFuelLiters))

        // Update Pump lifetime stats
        val updatedPump = pump.copy(
            lifetimeSalesLiters = pump.lifetimeSalesLiters + liters,
            lifetimeSalesCash = pump.lifetimeSalesCash + (liters * pricePerLiter)
        )
        dao.updatePump(updatedPump)

        // Process Customer Loyalty Points
        var customerName: String? = null
        if (customerId != null) {
            dao.getAllCustomers().firstOrNull()?.find { it.id == customerId }?.let { cust ->
                customerName = cust.name
                val pointsGained = (liters * 1).toInt() // 1 point per liter
                dao.updateCustomer(cust.copy(points = cust.points + pointsGained))
            }
        }

        // Fetch Employee
        var employeeName: String? = null
        if (employeeId != null) {
            dao.getAllEmployees().firstOrNull()?.find { it.id == employeeId }?.let { emp ->
                employeeName = emp.name
            }
        }

        val totalPrice = liters * pricePerLiter

        // Create transaction
        val tx = Transaction(
            pumpId = pumpId,
            pumpName = pump.name,
            fuelTypeId = fuelType.id,
            fuelTypeName = fuelType.name,
            liters = liters,
            pricePerLiter = pricePerLiter,
            totalPrice = totalPrice,
            paymentMethod = paymentMethod,
            customerId = customerId,
            customerName = customerName,
            employeeId = employeeId,
            employeeName = employeeName
        )
        val txId = dao.insertTransaction(tx)

        // Low fuel notification check (warn below 30% of standard 10000.0 L capacity = 3000.0 L)
        if (updatedFuelLiters < 3000.0) {
            dao.insertNotification(
                Notification(
                    title = "Low Fuel Warning: ${fuelType.name}",
                    message = "Fuel level for ${fuelType.name} has fallen to ${String.format("%.1f", updatedFuelLiters)} Liters (critical limit). Please request a tanker refill.",
                    type = "Low Fuel"
                )
            )
        }

        // Also add a sales notification
        dao.insertNotification(
            Notification(
                title = "New Purchase - ${fuelType.name}",
                message = "Sold ${String.format("%.2f", liters)}L ($${String.format("%.2f", totalPrice)}) via $paymentMethod on ${pump.name}.",
                type = "SalesAlert"
            )
        )

        tx.copy(id = txId.toInt())
    }

    // --- Attendance Logs ---
    suspend fun clockIn(employeeId: Int, employeeName: String, shift: String): Boolean = withContext(Dispatchers.IO) {
        val today = "2026-05-28"
        // Insert clock in entry
        dao.insertAttendance(
            Attendance(
                employeeId = employeeId,
                employeeName = employeeName,
                date = today,
                timeIn = "08:15 AM",
                timeOut = null,
                status = "Present"
            )
        )
        dao.insertNotification(
            Notification(
                title = "Attendance Checked-In",
                message = "$employeeName has successully clocked in for the $shift shift.",
                type = "EmployeeAlert"
            )
        )
        true
    }

    suspend fun clockOut(attendanceId: Int): Boolean = withContext(Dispatchers.IO) {
        // Fetch attendances from standard stream and update
        // (Just a helper query for updates)
        val list = dao.getAllAttendance().firstOrNull() ?: return@withContext false
        val attendance = list.find { it.id == attendanceId } ?: return@withContext false
        dao.updateAttendance(attendance.copy(timeOut = "05:00 PM"))
        true
    }

    // --- Notifications ---
    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        dao.markAllAsRead()
    }

    suspend fun deleteNotification(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteNotificationById(id)
    }

    suspend fun addNotification(title: String, message: String, type: String) = withContext(Dispatchers.IO) {
        dao.insertNotification(Notification(title = title, message = message, type = type))
    }

    // --- Settings Panel ---
    suspend fun updateAppSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dao.insertAppSettings(settings)
    }

    // --- Database Refilling Manager ---
    suspend fun refillTank(fuelTypeId: Int, liters: Double): Boolean = withContext(Dispatchers.IO) {
        val fuel = dao.getFuelTypeById(fuelTypeId) ?: return@withContext false
        val newLiters = (fuel.availableLiters + liters).coerceAtMost(fuel.capacity)
        dao.updateFuelType(fuel.copy(availableLiters = newLiters))
        dao.insertNotification(
            Notification(
                title = "Fuel Tank Refiled",
                message = "Successfully added ${String.format("%.1f", liters)}L to ${fuel.name}. Current volume: ${String.format("%.1f", newLiters)}L.",
                type = "Maintenance"
            )
        )
        true
    }

    // --- Seeding Sandbox Logic ---
    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        Log.d("AppRepository", "Checking database seeding...")

        // 1. Seed App Settings
        val currentSettings = dao.getAppSettings()
        if (currentSettings == null) {
            dao.insertAppSettings(AppSettings())
            Log.d("AppRepository", "Seeded AppSettings")
        }

        // 2. Seed Users
        val users = dao.getAllUsers().firstOrNull()
        if (users.isNullOrEmpty()) {
            dao.insertUser(User(email = "admin@fuelflow.com", passwordHash = "admin123", role = "Admin", name = "John Lawrence Martinez"))
            dao.insertUser(User(email = "cashier@fuelflow.com", passwordHash = "cashier123", role = "Cashier", name = "David Miller"))
            dao.insertUser(User(email = "staff@fuelflow.com", passwordHash = "staff123", role = "Staff", name = "Sarah Connor"))
            dao.insertUser(User(email = "attendant@fuelflow.com", passwordHash = "attendant123", role = "Fuel Attendant", name = "Arnie Schwarzenegger"))
            Log.d("AppRepository", "Seeded Users")
        }

        // 3. Seed Fuel Types
        val fuels = dao.getAllFuelTypes().firstOrNull()
        if (fuels.isNullOrEmpty()) {
            dao.insertFuelType(FuelType(name = "Premium Gasoline", pricePerLiter = 62.50, availableLiters = 8200.0, capacity = 10000.0))
            dao.insertFuelType(FuelType(name = "Diesel", pricePerLiter = 54.20, availableLiters = 9400.0, capacity = 10000.0))
            dao.insertFuelType(FuelType(name = "Regular Gasoline", pricePerLiter = 59.80, availableLiters = 2100.0, capacity = 10000.0)) // low fuel indicator!
            dao.insertFuelType(FuelType(name = "Unleaded", pricePerLiter = 61.10, availableLiters = 7500.0, capacity = 10000.0))
            dao.insertFuelType(FuelType(name = "Kerosene", pricePerLiter = 58.00, availableLiters = 1400.0, capacity = 5000.0)) // low fuel indicator!
            Log.d("AppRepository", "Seeded FuelTypes")
        }

        // 4. Seed Pumps
        val pumps = dao.getAllPumps().firstOrNull()
        if (pumps.isNullOrEmpty()) {
            dao.insertPump(Pump(name = "Pump A1", fuelTypeId = 1, status = "Active", lifetimeSalesLiters = 4250.0, lifetimeSalesCash = 265625.0))
            dao.insertPump(Pump(name = "Pump A2", fuelTypeId = 2, status = "Active", lifetimeSalesLiters = 6100.0, lifetimeSalesCash = 330620.0))
            dao.insertPump(Pump(name = "Pump B1", fuelTypeId = 3, status = "Active", lifetimeSalesLiters = 2800.0, lifetimeSalesCash = 167440.0))
            dao.insertPump(Pump(name = "Pump B2", fuelTypeId = 4, status = "Active", lifetimeSalesLiters = 3900.0, lifetimeSalesCash = 238290.0))
            dao.insertPump(Pump(name = "Pump C1", fuelTypeId = 5, status = "Maintenance", lifetimeSalesLiters = 1200.0, lifetimeSalesCash = 69600.0))
            dao.insertPump(Pump(name = "Pump C2", fuelTypeId = 1, status = "Inactive", lifetimeSalesLiters = 800.0, lifetimeSalesCash = 50000.0))
            Log.d("AppRepository", "Seeded Pumps")
        }

        // 5. Seed Employees
        val employees = dao.getAllEmployees().firstOrNull()
        if (employees.isNullOrEmpty()) {
            dao.insertEmployee(Employee(name = "John Lawrence Martinez", role = "Admin", email = "johnlawrencemartinez05@gmail.com", phone = "+63 917 123 4567", shift = "Morning (6 AM - 2 PM)", salary = 65000.0))
            dao.insertEmployee(Employee(name = "David Miller", role = "Cashier", email = "david@fuelflow.com", phone = "+63 919 765 4321", shift = "Afternoon (2 PM - 10 PM)", salary = 24000.0))
            dao.insertEmployee(Employee(name = "Sarah Connor", role = "Staff", email = "sarah@fuelflow.com", phone = "+63 920 111 2222", shift = "Night (10 PM - 6 AM)", salary = 32000.0))
            dao.insertEmployee(Employee(name = "Arnie Schwarzenegger", role = "Fuel Attendant", email = "arnie@fuelflow.com", phone = "+63 915 888 9999", shift = "Morning (6 AM - 2 PM)", salary = 20000.0))
            Log.d("AppRepository", "Seeded Employees")
        }

        // 6. Seed Customers
        val customers = dao.getAllCustomers().firstOrNull()
        if (customers.isNullOrEmpty()) {
            dao.insertCustomer(Customer(name = "Michael Jordan", phone = "+63 918 333 4444", points = 125, membershipNo = "FF- Loyalty #7721", vehicleNo = "BULL-23"))
            dao.insertCustomer(Customer(name = "James Bond", phone = "+63 922 007 0007", points = 350, membershipNo = "FF- Loyalty #0007", vehicleNo = "JB-007"))
            dao.insertCustomer(Customer(name = "Keanu Reeves", phone = "+63 955 888 1234", points = 990, membershipNo = "FF- Loyalty #8822", vehicleNo = "WICK-11"))
            Log.d("AppRepository", "Seeded Customers")
        }

        // 7. Seed Notifications
        val notifications = dao.getAllNotifications().firstOrNull()
        if (notifications.isNullOrEmpty()) {
            dao.insertNotification(Notification(title = "System Ready", message = "FuelFlow Pro Enterprise Core has initialized successfully.", type = "Maintenance"))
            dao.insertNotification(Notification(title = "Low Fuel warning: Regular Gasoline", message = "Fuel tank of Regular Gasoline has dropped to 2100 Liters. Ordering refill is highly recommended.", type = "Low Fuel"))
            dao.insertNotification(Notification(title = "Low Fuel warning: Kerosene", message = "Fuel level of Kerosene is at 1400 Liters.", type = "Low Fuel"))
            Log.d("AppRepository", "Seeded Notifications")
        }

        // 8. Seed Transactions (simulating past sales history)
        val transactions = dao.getAllTransactions().firstOrNull()
        if (transactions.isNullOrEmpty()) {
            val hourMs = 3600000L
            val now = System.currentTimeMillis()
            dao.insertTransaction(Transaction(timestamp = now - 5 * hourMs, pumpId = 1, pumpName = "Pump A1", fuelTypeId = 1, fuelTypeName = "Premium Gasoline", liters = 45.0, pricePerLiter = 62.50, totalPrice = 2812.50, paymentMethod = "GCash", customerId = 1, customerName = "Michael Jordan", employeeId = 2, employeeName = "David Miller"))
            dao.insertTransaction(Transaction(timestamp = now - 18 * hourMs, pumpId = 2, pumpName = "Pump A2", fuelTypeId = 2, fuelTypeName = "Diesel", liters = 120.0, pricePerLiter = 54.20, totalPrice = 6504.00, paymentMethod = "Card", customerId = 2, customerName = "James Bond", employeeId = 4, employeeName = "Arnie Schwarzenegger"))
            dao.insertTransaction(Transaction(timestamp = now - 24 * hourMs, pumpId = 3, pumpName = "Pump B1", fuelTypeId = 3, fuelTypeName = "Regular Gasoline", liters = 30.0, pricePerLiter = 59.80, totalPrice = 1794.00, paymentMethod = "Cash", employeeId = 2, employeeName = "David Miller"))
            dao.insertTransaction(Transaction(timestamp = now - 30 * hourMs, pumpId = 4, pumpName = "Pump B2", fuelTypeId = 4, fuelTypeName = "Unleaded", liters = 55.0, pricePerLiter = 61.10, totalPrice = 3360.50, paymentMethod = "Maya", customerId = 3, customerName = "Keanu Reeves", employeeId = 4, employeeName = "Arnie Schwarzenegger"))
            Log.d("AppRepository", "Seeded Transactions")
        }
    }
}
