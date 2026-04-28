package com.raithabharosahub.di

import com.raithabharosahub.BuildConfig
import com.raithabharosahub.data.remote.WeatherApiService
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing network dependencies (Retrofit, OkHttp, API services).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.openweathermap.org/"

    /**
     * Provides OpenWeatherMap API key from BuildConfig.
     */
    @Provides
    @Singleton
    @Named("openWeatherApiKey")
    fun provideOpenWeatherApiKey(): String {
        return BuildConfig.OWM_API_KEY
    }

    /**
     * Injects the OpenWeatherMap API key into each request.
     */
    @Provides
    @Singleton
    fun provideOpenWeatherApiKeyInterceptor(@Named("openWeatherApiKey") apiKey: String): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            if (apiKey.isBlank()) {
                return@Interceptor chain.proceed(request)
            }

            val url = request.url.newBuilder()
                .addQueryParameter("appid", apiKey)
                .build()

            chain.proceed(request.newBuilder().url(url).build())
        }
    }

    /**
     * Provides OkHttpClient with logging interceptor for debug builds.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(apiKeyInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                addInterceptor(apiKeyInterceptor)
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()
    }

    /**
     * Provides Moshi instance for JSON serialization/deserialization.
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    /**
     * Provides Retrofit instance for OpenWeatherMap API.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * Provides WeatherApiService interface for weather data.
     * This is a stub implementation - will be fully implemented in Step 4.
     */
    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService {
        return retrofit.create(WeatherApiService::class.java)
    }
}