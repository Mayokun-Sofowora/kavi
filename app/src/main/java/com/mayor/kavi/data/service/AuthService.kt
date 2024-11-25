package com.mayor.kavi.data.service

import com.mayor.kavi.data.models.Users
import javax.inject.Inject


interface AuthService {
    suspend fun authenticate(username: String): Users?
}

class AuthServiceImpl @Inject constructor(): AuthService {

    // For example, a hardcoded user to authenticate against (could be replaced with network or DB lookup)
    private val hardcodedUser = Users(
        id = 1L,
        username = "Mayokun Tester",
        preferences = "{}" // Default empty JSON for now
    )

    override suspend fun authenticate(username: String): Users? {
        // Simple authentication logic: check if username and password match the hardcoded credentials
        return if (username == "Mayokun Tester") {
            hardcodedUser
        } else {
            null // Invalid credentials
        }
    }
}
