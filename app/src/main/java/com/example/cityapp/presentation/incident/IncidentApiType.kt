package com.example.cityapp.presentation.incident

enum class IncidentApiType(val apiKey: String, val labelUa: String) {
    ACCIDENT("accident", "ДТП / аварія"),
    BREAKDOWN("breakdown", "Технічна несправність"),
    TRAFFIC_JAM("traffic_jam", "Затор на дорозі"),
    OTHER("other", "Інше");

    fun templateUa(): String = when (this) {
        ACCIDENT ->
            "Час події: \n" +
                "Місце (вулиця, район, орієнтир): \n" +
                "Учасники ДТП (ТЗ, пішоходи): \n" +
                "Опис пошкоджень / наслідків: \n" +
                "Чи є постраждалі (так/ні, що робимо): \n"

        BREAKDOWN ->
            "Час виявлення: \n" +
                "Місце зупинки ТЗ: \n" +
                "Симптоми несправності (шум, індикатори): \n" +
                "Чи можна рухатися самостійно (так/ні): \n" +
                "Евакуатор / майстер викликано: \n"

        TRAFFIC_JAM ->
            "Час: \n" +
                "Ділянка дороги / напрямок руху: \n" +
                "Приблизна затримка (хв): \n" +
                "Причина (за можливості): \n" +
                "Об’їзд або коридор для ГМГС: \n"

        OTHER ->
            "Що сталося: \n" +
                "Місце: \n" +
                "Чи потрібна допомога диспетчера: \n"
    }

    companion object {
        fun fromApi(key: String): IncidentApiType =
            entries.firstOrNull { it.apiKey == key } ?: OTHER
    }
}
