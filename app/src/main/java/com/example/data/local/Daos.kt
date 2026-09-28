package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BonEntity
import com.example.data.model.CalendarDayEntity
import com.example.data.model.FixedPostEntity
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.QuinzaineEntity
import com.example.data.model.TransportEntity
import com.example.data.model.WorkerGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuinzaineDao {
    @Query("SELECT * FROM quinzaines ORDER BY updatedAt DESC")
    fun getAllQuinzaines(): Flow<List<QuinzaineEntity>>

    @Query("SELECT * FROM quinzaines WHERE id = :id LIMIT 1")
    fun getQuinzaineById(id: Long): Flow<QuinzaineEntity?>

    @Query("SELECT * FROM quinzaines WHERE id = :id LIMIT 1")
    suspend fun getQuinzaineByIdDirect(id: Long): QuinzaineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuinzaine(quinzaine: QuinzaineEntity): Long

    @Update
    suspend fun updateQuinzaine(quinzaine: QuinzaineEntity)

    @Delete
    suspend fun deleteQuinzaine(quinzaine: QuinzaineEntity)

    @Query("DELETE FROM quinzaines WHERE id = :id")
    suspend fun deleteQuinzaineById(id: Long)
}

@Dao
interface WorkerGroupDao {
    @Query("SELECT * FROM worker_groups WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    fun getGroupsForQuinzaine(quinzaineId: Long): Flow<List<WorkerGroupEntity>>

    @Query("SELECT * FROM worker_groups WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    suspend fun getGroupsForQuinzaineDirect(quinzaineId: Long): List<WorkerGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: WorkerGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGroups(groups: List<WorkerGroupEntity>)

    @Update
    suspend fun updateGroup(group: WorkerGroupEntity)

    @Delete
    suspend fun deleteGroup(group: WorkerGroupEntity)

    @Query("DELETE FROM worker_groups WHERE quinzaineId = :quinzaineId")
    suspend fun deleteGroupsForQuinzaine(quinzaineId: Long)
}

@Dao
interface HourlyWorkerDao {
    @Query("SELECT * FROM hourly_workers WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    fun getHourlyWorkersForQuinzaine(quinzaineId: Long): Flow<List<HourlyWorkerEntity>>

    @Query("SELECT * FROM hourly_workers WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    suspend fun getHourlyWorkersForQuinzaineDirect(quinzaineId: Long): List<HourlyWorkerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyWorker(worker: HourlyWorkerEntity): Long

    @Update
    suspend fun updateHourlyWorker(worker: HourlyWorkerEntity)

    @Delete
    suspend fun deleteHourlyWorker(worker: HourlyWorkerEntity)
}

@Dao
interface FixedPostDao {
    @Query("SELECT * FROM fixed_posts WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    fun getFixedPostsForQuinzaine(quinzaineId: Long): Flow<List<FixedPostEntity>>

    @Query("SELECT * FROM fixed_posts WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    suspend fun getFixedPostsForQuinzaineDirect(quinzaineId: Long): List<FixedPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedPost(post: FixedPostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFixedPosts(posts: List<FixedPostEntity>)

    @Update
    suspend fun updateFixedPost(post: FixedPostEntity)

    @Delete
    suspend fun deleteFixedPost(post: FixedPostEntity)
}

@Dao
interface CalendarDayDao {
    @Query("SELECT * FROM calendar_days WHERE quinzaineId = :quinzaineId ORDER BY date ASC")
    fun getCalendarDaysForQuinzaine(quinzaineId: Long): Flow<List<CalendarDayEntity>>

    @Query("SELECT * FROM calendar_days WHERE quinzaineId = :quinzaineId ORDER BY date ASC")
    suspend fun getCalendarDaysForQuinzaineDirect(quinzaineId: Long): List<CalendarDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCalendarDay(day: CalendarDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCalendarDays(days: List<CalendarDayEntity>)

    @Query("DELETE FROM calendar_days WHERE quinzaineId = :quinzaineId")
    suspend fun deleteCalendarDaysForQuinzaine(quinzaineId: Long)
}

@Dao
interface TransportDao {
    @Query("SELECT * FROM transports WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    fun getTransportsForQuinzaine(quinzaineId: Long): Flow<List<TransportEntity>>

    @Query("SELECT * FROM transports WHERE quinzaineId = :quinzaineId ORDER BY id ASC")
    suspend fun getTransportsForQuinzaineDirect(quinzaineId: Long): List<TransportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransport(transport: TransportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransports(transports: List<TransportEntity>)

    @Update
    suspend fun updateTransport(transport: TransportEntity)

    @Delete
    suspend fun deleteTransport(transport: TransportEntity)

    @Query("DELETE FROM transports WHERE id = :id")
    suspend fun deleteTransportById(id: Long)
}

@Dao
interface BonDao {
    @Query("SELECT * FROM bons WHERE quinzaineId = :quinzaineId ORDER BY id DESC")
    fun getBonsForQuinzaine(quinzaineId: Long): Flow<List<BonEntity>>

    @Query("SELECT * FROM bons WHERE quinzaineId = :quinzaineId ORDER BY id DESC")
    suspend fun getBonsForQuinzaineDirect(quinzaineId: Long): List<BonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBon(bon: BonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBons(bons: List<BonEntity>)

    @Update
    suspend fun updateBon(bon: BonEntity)

    @Delete
    suspend fun deleteBon(bon: BonEntity)

    @Query("DELETE FROM bons WHERE id = :id")
    suspend fun deleteBonById(id: Long)
}
