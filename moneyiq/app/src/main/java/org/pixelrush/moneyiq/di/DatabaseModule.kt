package org.pixelrush.moneyiq.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.pixelrush.moneyiq.data.db.AppDatabase
import org.pixelrush.moneyiq.data.db.MIGRATION_1_2
import org.pixelrush.moneyiq.data.db.MIGRATION_2_3
import org.pixelrush.moneyiq.data.db.MIGRATION_3_4
import org.pixelrush.moneyiq.data.db.MIGRATION_4_5
import org.pixelrush.moneyiq.data.db.MIGRATION_5_6
import org.pixelrush.moneyiq.data.db.MIGRATION_6_7
import org.pixelrush.moneyiq.data.db.MIGRATION_7_8
import org.pixelrush.moneyiq.data.db.MIGRATION_8_9
import org.pixelrush.moneyiq.data.db.MIGRATION_9_10
import org.pixelrush.moneyiq.data.db.MIGRATION_10_11
import org.pixelrush.moneyiq.data.db.MIGRATION_11_12
import org.pixelrush.moneyiq.data.db.MIGRATION_12_13
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "moneyiq.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
}
