package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FleetDao {

    // Drivers
    @Query("SELECT * FROM drivers ORDER BY name ASC")
    fun getAllDrivers(): Flow<List<Driver>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: Driver)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<Driver>)

    @Query("DELETE FROM drivers WHERE id = :id")
    suspend fun deleteDriverById(id: String)

    // Trucks
    @Query("SELECT * FROM trucks ORDER BY id ASC")
    fun getAllTrucks(): Flow<List<Truck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTruck(truck: Truck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrucks(trucks: List<Truck>)

    @Query("DELETE FROM trucks WHERE id = :id")
    suspend fun deleteTruckById(id: String)

    // Machines
    @Query("SELECT * FROM machines ORDER BY name ASC")
    fun getAllMachines(): Flow<List<Machine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: Machine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachines(machines: List<Machine>)

    @Query("DELETE FROM machines WHERE id = :id")
    suspend fun deleteMachineById(id: String)

    // Shifts
    @Query("SELECT * FROM shifts ORDER BY id ASC")
    fun getAllShifts(): Flow<List<Shift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<Shift>)

    // Truck Cycles
    @Query("SELECT * FROM truck_cycles ORDER BY unloadMillis DESC")
    fun getAllCycles(): Flow<List<TruckCycle>>

    @Query("SELECT * FROM truck_cycles WHERE date = :date ORDER BY unloadMillis DESC")
    fun getCyclesByDate(date: String): Flow<List<TruckCycle>>

    @Query("SELECT * FROM truck_cycles WHERE truckId = :truckId")
    suspend fun getCyclesForTruck(truckId: String): List<TruckCycle>

    @Query("SELECT * FROM truck_cycles WHERE truckId = :truckId ORDER BY unloadMillis DESC LIMIT 1")
    suspend fun getLastCycleForTruck(truckId: String): TruckCycle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: TruckCycle)

    @Delete
    suspend fun deleteCycle(cycle: TruckCycle)

    @Query("DELETE FROM truck_cycles")
    suspend fun clearAllCycles()

    @Transaction
    suspend fun clearAllData() {
        clearAllCycles()
        queryDeleteAllDrivers()
        queryDeleteAllTrucks()
        queryDeleteAllMachines()
    }

    @Query("DELETE FROM drivers")
    suspend fun queryDeleteAllDrivers()

    @Query("DELETE FROM trucks")
    suspend fun queryDeleteAllTrucks()

    @Query("DELETE FROM machines")
    suspend fun queryDeleteAllMachines()
}
