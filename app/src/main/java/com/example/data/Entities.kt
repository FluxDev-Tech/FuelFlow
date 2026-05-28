package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val role: String, // "Admin", "Staff", "Cashier", "Fuel Attendant"
    val name: String,
    val imageUrl: String = ""
)

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val email: String,
    val phone: String,
    val shift: String, // "Morning (6 AM - 2 PM)", "Afternoon (2 PM - 10 PM)", "Night (10 PM - 6 AM)"
    val salary: Double,
    val imgUrl: String = "",
    val dateJoined: String = "2026-05-28"
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val points: Int = 0,
    val membershipNo: String = "",
    val vehicleNo: String = "",
    val dateRegistered: String = "2026-05-28"
)

@Entity(tableName = "fuel_types")
data class FuelType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // "Diesel", "Premium Gasoline", "Regular Gasoline", "Unleaded", "Kerosene"
    val pricePerLiter: Double,
    val availableLiters: Double,
    val capacity: Double = 10000.0 // Tank Capacity e.g. 10000 Liters
)

@Entity(tableName = "pumps")
data class Pump(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // "Pump 1", "Pump 2", etc.
    val fuelTypeId: Int, // Refers to FuelType
    val status: String = "Active", // "Active", "Inactive", "Maintenance"
    val lifetimeSalesLiters: Double = 0.0,
    val lifetimeSalesCash: Double = 0.0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val pumpId: Int,
    val pumpName: String = "",
    val fuelTypeId: Int,
    val fuelTypeName: String = "",
    val liters: Double,
    val pricePerLiter: Double,
    val totalPrice: Double,
    val paymentMethod: String, // "Cash", "GCash", "Maya", "Card"
    val customerId: Int? = null,
    val customerName: String? = null,
    val employeeId: Int? = null,
    val employeeName: String? = null
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val employeeName: String,
    val date: String, // YYYY-MM-DD
    val timeIn: String, // HH:MM AM/PM
    val timeOut: String? = null, // HH:MM AM/PM
    val status: String = "Present" // "Present", "On Leave", "Late"
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String, // "Low Fuel", "Maintenance", "SalesAlert", "EmployeeAlert"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val stationName: String = "FuelFlow Pro Station",
    val companyAddress: String = "123 Fuel Boulevard, Metro City",
    val emailHost: String = "smtp.fuelflow.com",
    val emailPort: Int = 587,
    val enableAutoBackup: Boolean = true,
    val darkTheme: Boolean = true,
    val mockWeatherTemp: String = "85°F Sunny",
    val pricePremium: Double = 62.50,
    val priceDiesel: Double = 54.20,
    val priceRegular: Double = 59.80,
    val priceUnleaded: Double = 61.10,
    val priceKerosene: Double = 58.00
)
