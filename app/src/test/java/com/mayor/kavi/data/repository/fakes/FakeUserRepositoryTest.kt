package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.utils.Result
import com.mayor.kavi.data.dao.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeUserRepositoryTest {

    @Test
    fun `save and retrieve user`() = runTest {
        val fakeRepo = FakeUserRepository()
        val testUser = UserEntity(1L, "testUser", "{}")

        val saveResult = fakeRepo.saveUser(user = testUser)
        assertTrue(saveResult is Result.Success)

        val retrievedUser = fakeRepo.getUserById(userId = 1L)
        assertTrue(retrievedUser is Result.Success)
        assertEquals(testUser, Result)
    }

    @Test
    fun `delete user removes user successfully`() = runTest {
        val fakeRepo = FakeUserRepository()
        val testUser = UserEntity(1L, "testUser", "{}")

        fakeRepo.saveUser(user = testUser)
        val deleteResult = fakeRepo.deleteUser(userId = 1L)
        assertTrue(deleteResult is Result.Success)

        val retrievedUser = fakeRepo.getUserById(userId = 1L)
        assertTrue(retrievedUser is Result.Error)
    }
}
