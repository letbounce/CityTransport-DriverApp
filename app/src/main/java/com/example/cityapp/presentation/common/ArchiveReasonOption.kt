package com.example.cityapp.presentation.common

enum class ArchiveReasonOption(val code: String, val labelUa: String) {
    MISTAKEN_CREATION("mistaken_creation", "Помилкове створення"),
    DUPLICATE_ENTRY("duplicate_entry", "Дублікат запису"),
    NO_LONGER_RELEVANT("no_longer_relevant", "Більше не актуально"),
    ENTERED_IN_ERROR("entered_in_error", "Введено помилково"),
    OTHER("other", "Інше")
}
