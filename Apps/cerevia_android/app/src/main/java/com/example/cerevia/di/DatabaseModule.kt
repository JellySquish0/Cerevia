package com.example.cerevia.di

import android.content.Context
import androidx.room.Room
import com.example.cerevia.data.local.AnalysisDao
import com.example.cerevia.data.local.CereviaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CereviaDatabase =
        Room.databaseBuilder(
            context,
            CereviaDatabase::class.java,
            "cerevia_db"
        ).build()

    @Provides
    @Singleton
    fun provideAnalysisDao(db: CereviaDatabase): AnalysisDao = db.analysisDao()
}
