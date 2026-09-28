package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "quinzaines")
data class QuinzaineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startDate: String,    // Format YYYY-MM-DD
    val currentDate: String,  // Format YYYY-MM-DD (date d'arrêté de la paie courante)
    val endDate: String,      // Format YYYY-MM-DD (fin de la quinzaine)
    val currentAmount: Double = 0.0,
    val currency: String = "DH",
    val isCompleted: Boolean = false,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "worker_groups",
    foreignKeys = [
        ForeignKey(
            entity = QuinzaineEntity::class,
            parentColumns = ["id"],
            childColumns = ["quinzaineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quinzaineId")]
)
data class WorkerGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quinzaineId: Long,
    val name: String,
    val workerCount: Int,
    val paymentTypeString: String = PaymentType.DAILY.name,
    val dailyRate: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val hoursWorked: Double = 0.0,
    val customAmount: Double = 0.0,
    val description: String = ""
) {
    val paymentType: PaymentType
        get() = runCatching { PaymentType.valueOf(paymentTypeString) }.getOrDefault(PaymentType.DAILY)

    val dailySubtotal: Double
        get() = when (paymentType) {
            PaymentType.DAILY -> workerCount * dailyRate
            PaymentType.HOURLY -> (hoursWorked * hourlyRate) // If per day or lump
            PaymentType.CUSTOM_AMOUNT -> customAmount
        }
}

@Entity(
    tableName = "hourly_workers",
    foreignKeys = [
        ForeignKey(
            entity = QuinzaineEntity::class,
            parentColumns = ["id"],
            childColumns = ["quinzaineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quinzaineId")]
)
data class HourlyWorkerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quinzaineId: Long,
    val name: String,
    val hourlyRate: Double,
    val hoursWorked: Double,
    val targetAmount: Double? = null,
    val description: String = ""
) {
    val subtotal: Double
        get() = hoursWorked * hourlyRate
}

@Entity(
    tableName = "fixed_posts",
    foreignKeys = [
        ForeignKey(
            entity = QuinzaineEntity::class,
            parentColumns = ["id"],
            childColumns = ["quinzaineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quinzaineId")]
)
data class FixedPostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quinzaineId: Long,
    val name: String,
    val category: String = "Autre", // Any user-entered category
    val typeString: String = FixedPostType.ONE_TIME.name,
    val amount: Double,
    val quantity: Int = 1,
    val daysCount: Int = 1,
    val isDirectLumpSum: Boolean = true // If true, uses amount directly as total; otherwise calculates with formula
) {
    val type: FixedPostType
        get() = runCatching { FixedPostType.valueOf(typeString) }.getOrDefault(FixedPostType.ONE_TIME)
}

@Entity(
    tableName = "calendar_days",
    foreignKeys = [
        ForeignKey(
            entity = QuinzaineEntity::class,
            parentColumns = ["id"],
            childColumns = ["quinzaineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quinzaineId", "date"], unique = true)]
)
data class CalendarDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quinzaineId: Long,
    val date: String, // YYYY-MM-DD
    val statusString: String = DayStatus.WORK.name,
    val holidayCustomAmountPerWorker: Double = 0.0,
    val note: String = ""
) {
    val status: DayStatus
        get() = runCatching { DayStatus.valueOf(statusString) }.getOrDefault(DayStatus.WORK)
}

/**
 * Section indépendante: Prévision Transport
 * Strictly isolated from Quinzaine payroll forecast.
 * Formula: Total Transport = Effectif × Prix (× jours if > 0)
 */
@Entity(
    tableName = "transports",
    foreignKeys = [
        ForeignKey(
            entity = QuinzaineEntity::class,
            parentColumns = ["id"],
            childColumns = ["quinzaineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quinzaineId")]
)
data class TransportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quinzaineId: Long,
    val name: String,
    val effectif: Int,
    val pricePerPerson: Double,
    val daysCount: Int = 1,
    val note: String = ""
) {
    val totalAmount: Double
        get() = effectif * pricePerPerson * (if (daysCount > 0) daysCount else 1)
}

/**
 * Section indépendante: Bons
 * Strictly informational tracking only.
 * NO forecast, NO payroll impact.
 */
@Entity(
    tableName = "bons",
    foreignKeys = [
        ForeignKey(
            entity = QuinzaineEntity::class,
            parentColumns = ["id"],
            childColumns = ["quinzaineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quinzaineId")]
)
data class BonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quinzaineId: Long,
    val bonNumber: String,
    val date: String, // YYYY-MM-DD
    val description: String = "",
    val quantityOrEffectif: Double = 1.0,
    val amount: Double = 0.0,
    val note: String = ""
)
