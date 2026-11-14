package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BillReminder
import com.example.data.model.ElectricityBill
import com.example.data.model.ElectricityConnection
import com.example.data.model.UsageReading

@Database(
    entities = [
        ElectricityConnection::class,
        ElectricityBill::class,
        BillReminder::class,
        UsageReading::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BijliDatabase : RoomDatabase() {
    abstract fun bijliDao(): BijliDao

    companion object {
        @Volatile
        private var instance: BijliDatabase? = null

        fun getInstance(context: Context): BijliDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BijliDatabase::class.java,
                    "bijlipay.db"
                ).build().also { instance = it }
            }
        }
    }
}
