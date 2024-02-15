package com.example.mvvm.di

import android.content.Context
import androidx.room.Room
import com.example.mvvm.room.Dao
import com.example.mvvm.room.Database
import com.example.mvvm.retro.Api
import com.example.mvvm.utils.Utils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class HiltModules {

    @Provides
    @Singleton
    fun provideRetroApiCompiler(): Api {
        return Retrofit.Builder().baseUrl(Utils.COMPILER_LINK)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .build()
            .create(Api::class.java)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()


    @Provides
    @Singleton
    fun provideChannelDao(appDatabase: Database): Dao = appDatabase.channelDao()


    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): Database {
        return Room.databaseBuilder(
            appContext,
            Database::class.java,
            "DB"
        ).allowMainThreadQueries().build()
    }

}