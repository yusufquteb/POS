package com.pos.system.di;

import com.pos.system.utils.AppExecutors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    static AppExecutors provideAppExecutors() {
        return new AppExecutors();
    }
}
