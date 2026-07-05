package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class Driver(
    @PrimaryKey val id: String,
    val name: String,
    val status: String = "Active"
)

@Entity(tableName = "trucks")
data class Truck(
    @PrimaryKey val id: String,
    val model: String,
    val capacityTons: Double = 40.0,
    val status: String = "Active"
)

@Entity(tableName = "machines")
data class Machine(
    @PrimaryKey val id: String,
    val name: String, // e.g. "Shovel S-01", "Excavator EX-102"
    val type: String // e.g. "Shovel", "Excavator", "Wheel Loader"
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey val id: String, // e.g. "S_DAY", "S_NIGHT"
    val name: String, // e.g. "Day Shift", "Night Shift"
    val startTime: String, // e.g. "06:00"
    val endTime: String // e.g. "18:00"
)

@Entity(tableName = "truck_cycles")
data class TruckCycle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val truckId: String,
    val driverId: String,
    val machineId: String,
    val shiftId: String,
    val trailerId: String = "T-01",
    val date: String, // e.g., "2026-07-03"
    val queueStartMillis: Long,
    val loadingStartMillis: Long,
    val loadingEndMillis: Long,
    val unloadingQueueStartMillis: Long,
    val unloadMillis: Long,
    val cycleTimeMinutes: Double // total duration from previous unload to this unload (or queueStart to unload if first)
)
