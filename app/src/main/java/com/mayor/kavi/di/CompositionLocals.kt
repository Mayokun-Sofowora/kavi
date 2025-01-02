package com.mayor.kavi.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.mayor.kavi.data.repository.UserRepository

val LocalUserRepository = staticCompositionLocalOf<UserRepository> {
    error("No UserRepository provided")
} 