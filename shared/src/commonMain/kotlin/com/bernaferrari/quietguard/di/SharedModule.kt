package com.bernaferrari.quietguard.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module(includes = [DataModule::class, PlatformDiModule::class])
@Configuration
@ComponentScan(
    "com.bernaferrari.quietguard.ui",
    "com.bernaferrari.quietguard.data",
    "com.bernaferrari.quietguard.domain",
)
class SharedModule