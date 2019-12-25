/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    //Assign Instance of Job for Coroutine  if want to cancel, cancel this job
    private var viewModelJob = Job()

    // Add UI Scope that will run on MainThread, we will add viewModelJob because they want place that result placed
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    //Database Object
    private val nights = database.getAllNights()

    //Transformation Night Value to Formatted String use format function in Util
    val nightString = Transformations.map(nights){nights->
        formatNights(nights,application.resources)
    }
    //LiveData and MutableLiveData
    // Observable
    private var tonight = MutableLiveData<SleepNight?>()

    // Initial Field
    init {
        initializeTonight()
    }

    private fun initializeTonight(){
        // launch Application Based on Scope
        uiScope.launch {
            // LiveData Style
            tonight.value = getTonightFromDatabase()
        }
    }

    //Suspend Function Asynchronous Method and must be Nullable return
    private suspend fun getTonightFromDatabase():SleepNight?{
        // Getting from DB is IO operation use IO Dispatcher do with this task return asynchronous style
        return withContext(Dispatchers.IO){
            // Call Room DAO
            var night = database.getTonight()
            if (night?.endTimeMilli != night?.startTimeMilli){
                night = null
            }
            night
        }
    }

    fun onStartTracking(){
        //launch UI Scope
        uiScope.launch {
            // New Value capture on Start (Dataclass SleepNight don't require attribute)
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight){
        // launch I/O Context and Insert Data to DB
        withContext(Dispatchers.IO){
            database.insert(night)
        }
    }

    //ViewModel Automatically have method onClear so override onCleared
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

