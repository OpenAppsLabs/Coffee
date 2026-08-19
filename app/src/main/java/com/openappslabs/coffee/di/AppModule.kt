package com.openappslabs.coffee.di

import android.content.Context
import com.openappslabs.coffee.data.CoffeeDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoffeeDataStore(@ApplicationContext context: Context): CoffeeDataStore {
        return CoffeeDataStore(context)
    }
}