package com.tomer.chitchat.di

import android.content.Context
import androidx.room.Room
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.Database
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.utils.Utils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoMessagesImpl
import com.tomer.chitchat.repo.RepoUtilImpl
import com.tomer.chitchat.repo.RepoUtils
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
        return Retrofit.Builder().baseUrl(Utils.SERVER_LINK)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .build()
            .create(Api::class.java)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideRepoMsgs(dao: Dao): RepoMessages = RepoMessagesImpl(dao)

    @Provides
    @Singleton
    fun provideRepoUtils(@ApplicationContext appContext: Context): RepoUtils = RepoUtilImpl(appContext)


    @Provides
    @Singleton
    fun provideMessageDao(appDatabase: Database): Dao = appDatabase.messageDao()


    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): Database {
        return Room.databaseBuilder(
            appContext,
            Database::class.java,
            "MSG_DB"
        ).build()
    }

}