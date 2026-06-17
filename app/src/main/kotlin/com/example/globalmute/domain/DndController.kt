package com.example.globalmute.domain

enum class DndMode { ALL, NONE }

interface DndController {
    fun setFilter(mode: DndMode)
}
