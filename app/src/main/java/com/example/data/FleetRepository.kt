package com.example.data

import kotlinx.coroutines.flow.Flow

class FleetRepository(private val fleetDao: FleetDao) {

    val allDrivers: Flow<List<Driver>> = fleetDao.getAllDrivers()
    val allTrucks: Flow<List<Truck>> = fleetDao.getAllTrucks()
    val allMachines: Flow<List<Machine>> = fleetDao.getAllMachines()
    val allShifts: Flow<List<Shift>> = fleetDao.getAllShifts()
    val allCycles: Flow<List<TruckCycle>> = fleetDao.getAllCycles()

    fun getCyclesByDate(date: String): Flow<List<TruckCycle>> {
        return fleetDao.getCyclesByDate(date)
    }

    suspend fun insertDriver(driver: Driver) = fleetDao.insertDriver(driver)
    suspend fun insertDrivers(drivers: List<Driver>) = fleetDao.insertDrivers(drivers)
    suspend fun deleteDriver(id: String) = fleetDao.deleteDriverById(id)

    suspend fun insertTruck(truck: Truck) = fleetDao.insertTruck(truck)
    suspend fun insertTrucks(trucks: List<Truck>) = fleetDao.insertTrucks(trucks)
    suspend fun deleteTruck(id: String) = fleetDao.deleteTruckById(id)

    suspend fun insertMachine(machine: Machine) = fleetDao.insertMachine(machine)
    suspend fun insertMachines(machines: List<Machine>) = fleetDao.insertMachines(machines)
    suspend fun deleteMachine(id: String) = fleetDao.deleteMachineById(id)

    suspend fun insertShift(shift: Shift) = fleetDao.insertShift(shift)
    suspend fun insertShifts(shifts: List<Shift>) = fleetDao.insertShifts(shifts)

    suspend fun insertCycle(cycle: TruckCycle) = fleetDao.insertCycle(cycle)
    suspend fun deleteCycle(cycle: TruckCycle) = fleetDao.deleteCycle(cycle)
    suspend fun clearAllCycles() = fleetDao.clearAllCycles()
    suspend fun clearAllData() = fleetDao.clearAllData()

    suspend fun getLastCycleForTruck(truckId: String): TruckCycle? {
        return fleetDao.getLastCycleForTruck(truckId)
    }
}
