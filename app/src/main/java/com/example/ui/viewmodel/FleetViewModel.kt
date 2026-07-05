package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FleetViewModel(
    application: Application,
    private val repository: FleetRepository
) : AndroidViewModel(application) {

    // Date formatting
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
    private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Current selected date for filtering
    private val _selectedDate = MutableStateFlow(dateFormatter.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate

    // Navigation and Tab State
    private val _currentTab = MutableStateFlow(0) // 0: Dashboard, 1: Timeline, 2: Real-time Tracker, 3: Rankings, 4: Shift Comparison, 5: Export
    val currentTab: StateFlow<Int> = _currentTab

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    // Flows from repository
    val allDrivers = repository.allDrivers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTrucks = repository.allTrucks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMachines = repository.allMachines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allShifts = repository.allShifts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCycles = repository.allCycles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected shifts for comparison
    private val _compareShiftA = MutableStateFlow("S_DAY")
    val compareShiftA: StateFlow<String> = _compareShiftA

    private val _compareShiftB = MutableStateFlow("S_NIGHT")
    val compareShiftB: StateFlow<String> = _compareShiftB

    fun setCompareShiftA(id: String) { _compareShiftA.value = id }
    fun setCompareShiftB(id: String) { _compareShiftB.value = id }

    // ACTIVE TRACKER STATE MACHINE
    enum class TrackerState {
        IDLE,
        QUEUING_LOAD,
        LOADING,
        TRAVELING,
        QUEUING_UNLOAD
    }

    private val _trackerState = MutableStateFlow(TrackerState.IDLE)
    val trackerState: StateFlow<TrackerState> = _trackerState

    // Active tracking values
    private val _activeTruckId = MutableStateFlow("")
    val activeTruckId: StateFlow<String> = _activeTruckId

    private val _activeDriverId = MutableStateFlow("")
    val activeDriverId: StateFlow<String> = _activeDriverId

    private val _activeMachineId = MutableStateFlow("")
    val activeMachineId: StateFlow<String> = _activeMachineId

    private val _activeShiftId = MutableStateFlow("")
    val activeShiftId: StateFlow<String> = _activeShiftId

    private val _activeTrailerId = MutableStateFlow("T-01")
    val activeTrailerId: StateFlow<String> = _activeTrailerId

    // Active Timestamps
    private val _tQueueStart = MutableStateFlow(0L)
    val tQueueStart: StateFlow<Long> = _tQueueStart

    private val _tLoadingStart = MutableStateFlow(0L)
    val tLoadingStart: StateFlow<Long> = _tLoadingStart

    private val _tLoadingEnd = MutableStateFlow(0L)
    val tLoadingEnd: StateFlow<Long> = _tLoadingEnd

    private val _tUnloadingQueueStart = MutableStateFlow(0L)
    val tUnloadingQueueStart: StateFlow<Long> = _tUnloadingQueueStart

    fun setActiveTruck(id: String) { _activeTruckId.value = id }
    fun setActiveDriver(id: String) { _activeDriverId.value = id }
    fun setActiveMachine(id: String) { _activeMachineId.value = id }
    fun setActiveShift(id: String) { _activeShiftId.value = id }
    fun setActiveTrailer(id: String) { _activeTrailerId.value = id }

    // Start Real-time Tracker Cycle
    fun startQueueLoad() {
        if (_activeTruckId.value.isEmpty() || _activeDriverId.value.isEmpty() || _activeMachineId.value.isEmpty() || _activeShiftId.value.isEmpty()) {
            return
        }
        _tQueueStart.value = System.currentTimeMillis()
        _trackerState.value = TrackerState.QUEUING_LOAD
    }

    fun startLoading() {
        _tLoadingStart.value = System.currentTimeMillis()
        _trackerState.value = TrackerState.LOADING
    }

    fun startTraveling() {
        _tLoadingEnd.value = System.currentTimeMillis()
        _trackerState.value = TrackerState.TRAVELING
    }

    fun startQueueUnload() {
        _tUnloadingQueueStart.value = System.currentTimeMillis()
        _trackerState.value = TrackerState.QUEUING_UNLOAD
    }

    fun completeUnload() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val truckId = _activeTruckId.value
            val lastCycle = repository.getLastCycleForTruck(truckId)
            
            // Cycle Time is unload-to-unload if previous exists, else total duration of current cycle
            val cycleTimeMin = if (lastCycle != null && (now - lastCycle.unloadMillis) < 4 * 3600000L) {
                // If previous unload was within 4 hours, count as unload-to-unload
                (now - lastCycle.unloadMillis).toDouble() / 60000.0
            } else {
                // Fallback to start queue to complete unload duration
                (now - _tQueueStart.value).toDouble() / 60000.0
            }

            val newCycle = TruckCycle(
                truckId = truckId,
                driverId = _activeDriverId.value,
                machineId = _activeMachineId.value,
                shiftId = _activeShiftId.value,
                trailerId = _activeTrailerId.value,
                date = dateFormatter.format(Date()),
                queueStartMillis = _tQueueStart.value,
                loadingStartMillis = _tLoadingStart.value,
                loadingEndMillis = _tLoadingEnd.value,
                unloadingQueueStartMillis = _tUnloadingQueueStart.value,
                unloadMillis = now,
                cycleTimeMinutes = cycleTimeMin
            )

            repository.insertCycle(newCycle)
            
            // Clean up / reset state machine but preserve truck and driver for quick consecutive loads!
            _trackerState.value = TrackerState.IDLE
            _tQueueStart.value = 0L
            _tLoadingStart.value = 0L
            _tLoadingEnd.value = 0L
            _tUnloadingQueueStart.value = 0L
        }
    }

    fun cancelActiveTracking() {
        _trackerState.value = TrackerState.IDLE
        _tQueueStart.value = 0L
        _tLoadingStart.value = 0L
        _tLoadingEnd.value = 0L
        _tUnloadingQueueStart.value = 0L
    }

    // MANUAL LOGGING API
    fun logManualCycle(
        truckId: String,
        driverId: String,
        machineId: String,
        shiftId: String,
        trailerId: String,
        date: String,
        queueDurationMin: Double,
        loadingDurationMin: Double,
        travelDurationMin: Double,
        unloadQueueDurationMin: Double,
        unloadDurationMin: Double // unloading itself
    ) {
        viewModelScope.launch {
            // Reconstruct timeline backwards or forwards based on current time or arbitrary timestamps
            val baseTime = System.currentTimeMillis() - (60 * 60000L) // 1 hour ago
            
            val tQueue = baseTime
            val tLoadStart = tQueue + (queueDurationMin * 60000L).toLong()
            val tLoadEnd = tLoadStart + (loadingDurationMin * 60000L).toLong()
            val tUnloadQueueStart = tLoadEnd + (travelDurationMin * 60000L).toLong()
            val tUnloadComplete = tUnloadQueueStart + (unloadQueueDurationMin * 60000L).toLong()

            val cycleTimeMin = queueDurationMin + loadingDurationMin + travelDurationMin + unloadQueueDurationMin + unloadDurationMin

            val newCycle = TruckCycle(
                truckId = truckId,
                driverId = driverId,
                machineId = machineId,
                shiftId = shiftId,
                trailerId = trailerId,
                date = date,
                queueStartMillis = tQueue,
                loadingStartMillis = tLoadStart,
                loadingEndMillis = tLoadEnd,
                unloadingQueueStartMillis = tUnloadQueueStart,
                unloadMillis = tUnloadComplete,
                cycleTimeMinutes = cycleTimeMin
            )
            repository.insertCycle(newCycle)
        }
    }

    fun deleteCycle(cycle: TruckCycle) {
        viewModelScope.launch {
            repository.deleteCycle(cycle)
        }
    }

    fun clearAllCycles() {
        viewModelScope.launch {
            repository.clearAllCycles()
        }
    }

    // DATABASE SEEDING FOR DEMO RESULTS
    fun seedDemoData() {
        viewModelScope.launch {
            repository.clearAllData()

            val shifts = listOf(
                Shift("S_DAY", "Day Shift", "06:00", "18:00"),
                Shift("S_NIGHT", "Night Shift", "18:00", "06:00")
            )
            val drivers = listOf(
                Driver("D01", "John Doe"),
                Driver("D02", "Sarah Connor"),
                Driver("D03", "Alex Mercer"),
                Driver("D04", "Elena Fisher")
            )
            val trucks = listOf(
                Truck("T-01", "Caterpillar 777F", 90.0),
                Truck("T-02", "Caterpillar 785D", 140.0),
                Truck("T-03", "Komatsu HD785-7", 100.0),
                Truck("T-04", "Komatsu HD830E", 240.0)
            )
            val machines = listOf(
                Machine("M-01", "P&H 4100XPC Shovel", "Shovel"),
                Machine("M-02", "CAT 6060 Hydraulic Shovel", "Excavator")
            )

            repository.insertShifts(shifts)
            repository.insertDrivers(drivers)
            repository.insertTrucks(trucks)
            repository.insertMachines(machines)

            // Generate 12 cycles for Day Shift, 8 cycles for Night Shift for realistic analysis
            val todayStr = dateFormatter.format(Date())
            val baseTimeToday = System.currentTimeMillis() - 10 * 3600000L // 10 hours ago

            val sampleCycles = listOf(
                // Load 1: John Doe, T-01, M-01, S_DAY
                createSampleCycle(
                    truckId = "T-01", driverId = "D01", machineId = "M-01", shiftId = "S_DAY",
                    baseTime = baseTimeToday, qMin = 3.0, lMin = 4.0, tMin = 11.0, uqMin = 2.0, cycleTotal = 20.0, date = todayStr
                ),
                // Load 2: Sarah Connor, T-02, M-01, S_DAY
                createSampleCycle(
                    truckId = "T-02", driverId = "D02", machineId = "M-01", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 22 * 60000L, qMin = 2.0, lMin = 5.0, tMin = 12.0, uqMin = 3.0, cycleTotal = 22.0, date = todayStr
                ),
                // Load 3: Alex Mercer, T-03, M-02, S_DAY
                createSampleCycle(
                    truckId = "T-03", driverId = "D03", machineId = "M-02", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 45 * 60000L, qMin = 1.0, lMin = 6.0, tMin = 14.0, uqMin = 1.0, cycleTotal = 22.0, date = todayStr
                ),
                // Load 4: John Doe, T-01, M-01, S_DAY
                createSampleCycle(
                    truckId = "T-01", driverId = "D01", machineId = "M-01", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 60 * 60000L, qMin = 4.0, lMin = 3.5, tMin = 10.5, uqMin = 2.0, cycleTotal = 20.0, date = todayStr
                ),
                // Load 5: Sarah Connor, T-02, M-02, S_DAY
                createSampleCycle(
                    truckId = "T-02", driverId = "D02", machineId = "M-02", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 85 * 60000L, qMin = 3.0, lMin = 5.5, tMin = 13.0, uqMin = 1.5, cycleTotal = 23.0, date = todayStr
                ),
                // Load 6: Elena Fisher, T-04, M-01, S_DAY
                createSampleCycle(
                    truckId = "T-04", driverId = "D04", machineId = "M-01", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 110 * 60000L, qMin = 5.0, lMin = 8.0, tMin = 15.0, uqMin = 4.0, cycleTotal = 32.0, date = todayStr
                ),
                // Load 7: Alex Mercer, T-03, M-02, S_DAY
                createSampleCycle(
                    truckId = "T-03", driverId = "D03", machineId = "M-02", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 130 * 60000L, qMin = 1.5, lMin = 5.0, tMin = 13.5, uqMin = 1.0, cycleTotal = 21.0, date = todayStr
                ),
                // Load 8: Elena Fisher, T-04, M-01, S_DAY
                createSampleCycle(
                    truckId = "T-04", driverId = "D04", machineId = "M-01", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 160 * 60000L, qMin = 4.0, lMin = 7.5, tMin = 14.5, uqMin = 3.0, cycleTotal = 29.0, date = todayStr
                ),
                // Load 9: John Doe, T-01, M-01, S_DAY
                createSampleCycle(
                    truckId = "T-01", driverId = "D01", machineId = "M-01", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 180 * 60000L, qMin = 2.5, lMin = 4.0, tMin = 11.5, uqMin = 1.0, cycleTotal = 19.0, date = todayStr
                ),
                // Load 10: Sarah Connor, T-02, M-02, S_DAY
                createSampleCycle(
                    truckId = "T-02", driverId = "D02", machineId = "M-02", shiftId = "S_DAY",
                    baseTime = baseTimeToday + 210 * 60000L, qMin = 2.0, lMin = 6.0, tMin = 12.5, uqMin = 2.5, cycleTotal = 23.0, date = todayStr
                ),

                // NIGHT SHIFT S_NIGHT
                // Load 11: Alex Mercer, T-03, M-02, S_NIGHT
                createSampleCycle(
                    truckId = "T-03", driverId = "D03", machineId = "M-02", shiftId = "S_NIGHT",
                    baseTime = baseTimeToday + 360 * 60000L, qMin = 6.0, lMin = 7.0, tMin = 16.0, uqMin = 5.0, cycleTotal = 34.0, date = todayStr
                ),
                // Load 12: Elena Fisher, T-04, M-01, S_NIGHT
                createSampleCycle(
                    truckId = "T-04", driverId = "D04", machineId = "M-01", shiftId = "S_NIGHT",
                    baseTime = baseTimeToday + 390 * 60000L, qMin = 8.0, lMin = 9.0, tMin = 18.0, uqMin = 4.5, cycleTotal = 39.5, date = todayStr
                ),
                // Load 13: John Doe, T-01, M-01, S_NIGHT
                createSampleCycle(
                    truckId = "T-01", driverId = "D01", machineId = "M-01", shiftId = "S_NIGHT",
                    baseTime = baseTimeToday + 420 * 60000L, qMin = 5.0, lMin = 4.5, tMin = 13.0, uqMin = 3.0, cycleTotal = 25.5, date = todayStr
                ),
                // Load 14: Sarah Connor, T-02, M-02, S_NIGHT
                createSampleCycle(
                    truckId = "T-02", driverId = "D02", machineId = "M-02", shiftId = "S_NIGHT",
                    baseTime = baseTimeToday + 450 * 60000L, qMin = 4.5, lMin = 6.5, tMin = 14.5, uqMin = 3.5, cycleTotal = 29.0, date = todayStr
                ),
                // Load 15: Alex Mercer, T-03, M-02, S_NIGHT
                createSampleCycle(
                    truckId = "T-03", driverId = "D03", machineId = "M-02", shiftId = "S_NIGHT",
                    baseTime = baseTimeToday + 480 * 60000L, qMin = 7.0, lMin = 7.5, tMin = 15.0, uqMin = 4.0, cycleTotal = 33.5, date = todayStr
                )
            )

            sampleCycles.forEach { repository.insertCycle(it) }

            // Set default active selectors so dropdowns don't start blank
            if (trucks.isNotEmpty()) _activeTruckId.value = trucks[0].id
            if (drivers.isNotEmpty()) _activeDriverId.value = drivers[0].id
            if (machines.isNotEmpty()) _activeMachineId.value = machines[0].id
            if (shifts.isNotEmpty()) _activeShiftId.value = shifts[0].id
        }
    }

    private fun createSampleCycle(
        truckId: String, driverId: String, machineId: String, shiftId: String,
        baseTime: Long, qMin: Double, lMin: Double, tMin: Double, uqMin: Double, cycleTotal: Double, date: String
    ): TruckCycle {
        val qStart = baseTime
        val lStart = qStart + (qMin * 60000).toLong()
        val lEnd = lStart + (lMin * 60000).toLong()
        val uqStart = lEnd + (tMin * 60000).toLong()
        val uComplete = uqStart + (uqMin * 60000).toLong()

        return TruckCycle(
            truckId = truckId,
            driverId = driverId,
            machineId = machineId,
            shiftId = shiftId,
            trailerId = "TR-" + (10 + (1..9).random()),
            date = date,
            queueStartMillis = qStart,
            loadingStartMillis = lStart,
            loadingEndMillis = lEnd,
            unloadingQueueStartMillis = uqStart,
            unloadMillis = uComplete,
            cycleTimeMinutes = cycleTotal
        )
    }

    // Add Driver
    fun addNewDriver(id: String, name: String) {
        viewModelScope.launch {
            repository.insertDriver(Driver(id, name))
        }
    }

    // Add Truck
    fun addNewTruck(id: String, model: String, capacity: Double) {
        viewModelScope.launch {
            repository.insertTruck(Truck(id, model, capacity))
        }
    }

    // Add Machine
    fun addNewMachine(id: String, name: String, type: String) {
        viewModelScope.launch {
            repository.insertMachine(Machine(id, name, type))
        }
    }
}

class FleetViewModelFactory(
    private val application: Application,
    private val repository: FleetRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FleetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FleetViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
