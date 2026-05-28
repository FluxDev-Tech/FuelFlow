package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)


    // --- Employees ---
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)


    // --- Customers ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)


    // --- Fuel Types ---
    @Query("SELECT * FROM fuel_types ORDER BY id ASC")
    fun getAllFuelTypes(): Flow<List<FuelType>>

    @Query("SELECT * FROM fuel_types WHERE id = :id LIMIT 1")
    suspend fun getFuelTypeById(id: Int): FuelType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelType(fuelType: FuelType): Long

    @Update
    suspend fun updateFuelType(fuelType: FuelType)

    @Delete
    suspend fun deleteFuelType(fuelType: FuelType)


    // --- Pumps ---
    @Query("SELECT * FROM pumps ORDER BY name ASC")
    fun getAllPumps(): Flow<List<Pump>>

    @Query("SELECT * FROM pumps WHERE id = :id LIMIT 1")
    suspend fun getPumpById(id: Int): Pump?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPump(pump: Pump): Long

    @Update
    suspend fun updatePump(pump: Pump)

    @Delete
    suspend fun deletePump(pump: Pump)


    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)


    // --- Attendance ---
    @Query("SELECT * FROM attendance ORDER BY date DESC, id DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Update
    suspend fun updateAttendance(attendance: Attendance)


    // --- Notifications ---
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)


    // --- Settings ---
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettings)
}
