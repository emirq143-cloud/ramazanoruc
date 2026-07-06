package com.emir.oructakibi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emir.oructakibi.data.AppDatabase
import com.emir.oructakibi.data.FastRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FastViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).fastRecordDao()

    private val _fastedDates = MutableStateFlow<Set<String>>(emptySet())
    val fastedDates: StateFlow<Set<String>> = _fastedDates.asStateFlow()

    private val _kazaDates = MutableStateFlow<Set<String>>(emptySet())
    val kazaDates: StateFlow<Set<String>> = _kazaDates.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAll().collect { records ->
                _fastedDates.value = records.filter { it.type == 1 }.map { it.date }.toSet()
                _kazaDates.value = records.filter { it.type == 2 }.map { it.date }.toSet()
            }
        }
    }

    fun cycleDate(date: String) {
        viewModelScope.launch {
            val fasted = _fastedDates.value.contains(date)
            val kaza = _kazaDates.value.contains(date)
            when {
                !fasted && !kaza -> dao.upsert(FastRecord(date = date, type = 1))
                fasted -> dao.upsert(FastRecord(date = date, type = 2))
                else -> dao.deleteByDate(date)
            }
        }
    }
}

