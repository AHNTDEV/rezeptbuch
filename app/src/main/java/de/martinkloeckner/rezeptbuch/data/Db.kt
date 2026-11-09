package de.martinkloeckner.rezeptbuch.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Structured parts (tags, photos, pp, wb) are stored as JSON text; the schema stays small. */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val name: String,
    val look: String,
    val notes: String,
    val source: String,
    val sourceUrl: String,
    val tagsJson: String,
    val photosJson: String,
    val ppJson: String,
    val wbJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "slots")
data class SlotEntity(@PrimaryKey val slot: String, val recipeId: String)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getRecipes(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: String): RecipeEntity?

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()

    @Query("SELECT * FROM slots")
    fun observeSlots(): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots")
    suspend fun getSlots(): List<SlotEntity>

    @Query("DELETE FROM slots")
    suspend fun clearSlots()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<SlotEntity>)

    @Transaction
    suspend fun replaceSlots(slots: List<SlotEntity>) {
        clearSlots()
        insertSlots(slots)
    }
}

@Database(entities = [RecipeEntity::class, SlotEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RecipeDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rezeptbuch.db").build()
    }
}
