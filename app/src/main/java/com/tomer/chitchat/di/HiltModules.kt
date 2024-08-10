package com.tomer.chitchat.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.tomer.chitchat.assets.RepoAssets
import com.tomer.chitchat.assets.WebAssetsRepo
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.impl.RepoMessagesImpl
import com.tomer.chitchat.repo.impl.RepoUtilImpl
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.repo.impl.RepoPersonRoom
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.retro.SyncCallAdapterFactory
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.Database
import com.tomer.chitchat.crypto.CryptoCipher
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.crypto.RepoCipher
import com.tomer.chitchat.crypto.RoomCipherRepo
import com.tomer.chitchat.notifications.AndroidNotificationService
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMedia
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.impl.RepoFileStorage
import com.tomer.chitchat.repo.impl.RepoRelationImpl
import com.tomer.chitchat.repo.impl.RepoSavedMedia
import com.tomer.chitchat.utils.Utils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class HiltModules {

    @Provides
    @Singleton
    fun provideRetroApiCompiler(repoUtils: RepoUtils): Api {
        return Retrofit.Builder().baseUrl(Utils.SERVER_LINK)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(SyncCallAdapterFactory.create())
            .client(OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val authenticatedRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer ${repoUtils.getToken()}")
                        .build()
                    chain.proceed(authenticatedRequest)
                }
                .build())
            .build()
            .create(Api::class.java)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()


    @Provides
    @Singleton
    fun provideCryptoService(repo:RepoCipher): CryptoService = CryptoCipher(repo)

    @Provides
    @Singleton
    fun provideNotificationService(@ApplicationContext appContext: Context): NotificationService = AndroidNotificationService(appContext)

    //region REPOS

    @Provides
    @Singleton
    fun provideRepoMsgs(dao: Dao): RepoMessages = RepoMessagesImpl(dao)

    @Provides
    @Singleton
    fun provideRepoUtils(@ApplicationContext appContext: Context): RepoUtils = RepoUtilImpl(appContext)

    @Provides
    @Singleton
    fun provideRepoAssets(@ApplicationContext appContext: Context): RepoAssets = WebAssetsRepo(appContext)

    @Provides
    @Singleton
    fun provideRepoStorage(@ApplicationContext appContext: Context): RepoStorage = RepoFileStorage(appContext)

    @Provides
    @Singleton
    fun provideRepoRelation(dao: Dao): RepoRelations = RepoRelationImpl(dao)

    @Provides
    @Singleton
    fun provideRepoPersons(dao: Dao): RepoPersons = RepoPersonRoom(dao)

    @Provides
    @Singleton
    fun provideRepoCrypto(dao: Dao): RepoCipher = RoomCipherRepo(dao)

    @Provides
    @Singleton
    fun provideRepoMedia(dao: Dao): RepoMedia = RepoSavedMedia(dao)

    //endregion REPOS

    //region ROOM


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
        )
            .allowMainThreadQueries()
            .build()
    }

    //endregion ROOM
}