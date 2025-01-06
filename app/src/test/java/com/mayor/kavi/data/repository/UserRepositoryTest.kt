package com.mayor.kavi.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.mayor.kavi.data.models.Avatar
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class UserRepositoryTest {
    @Mock
    private lateinit var firestore: FirebaseFirestore
    @Mock
    private lateinit var auth: FirebaseAuth
    @Mock
    private lateinit var currentUser: FirebaseUser
    @Mock
    private lateinit var documentSnapshot: DocumentSnapshot

    @Mock
    private lateinit var documentReference: DocumentReference
    @Mock
    private lateinit var collectionReference: CollectionReference

    private lateinit var repository: UserRepositoryImpl
    private val testUserId = "test-user-id"

    @Before
    fun setup() {
        whenever(firestore.collection("users")).thenReturn(collectionReference)
        whenever(collectionReference.document(any())).thenReturn(documentReference)
        whenever(auth.currentUser).thenReturn(currentUser)
        whenever(currentUser.uid).thenReturn(testUserId)

        repository = UserRepositoryImpl(firestore, auth)
    }

    @Test
    fun `getCurrentUserId returns current user id when authenticated`() {
        val result = repository.getCurrentUserId()
        assertEquals(testUserId, result)
    }

    @Test
    fun `getCurrentUserId returns null when not authenticated`() {
        whenever(auth.currentUser).thenReturn(null)
        val result = repository.getCurrentUserId()
        assertEquals(null, result)
    }

    @Test
    fun `getUserById returns existing profile successfully`() = runTest {
        val expectedProfile = UserProfile(
            id = testUserId,
            name = "Test User",
            email = "test@example.com",
            avatar = Avatar.DEFAULT
        )

        whenever(documentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))
        whenever(documentSnapshot.exists()).thenReturn(true)
        whenever(documentSnapshot.toObject(UserProfile::class.java)).thenReturn(expectedProfile)

        val result = repository.getUserById(testUserId)
        assertTrue(result is Result.Success)
        assertEquals(expectedProfile, result.data)
    }

    @Test
    fun `getUserById creates default profile when document doesn't exist`() = runTest {
        whenever(documentReference.get()).thenReturn(Tasks.forResult(documentSnapshot))
        whenever(documentSnapshot.exists()).thenReturn(false)
        whenever(documentReference.set(any())).thenReturn(Tasks.forResult(null))

        val result = repository.getUserById(testUserId)

        assertTrue(result is Result.Success)
        val profile = result.data
        assertEquals(testUserId, profile.id)
        assertEquals("Player", profile.name)
        assertEquals("", profile.email)
        assertEquals(Avatar.DEFAULT, profile.avatar)
    } // retest this

    @Test
    fun `updateUserProfile updates profile successfully`() = runTest {
        val profile = UserProfile(
            id = testUserId,
            name = "Updated Name",
            avatar = Avatar.DEFAULT
        )

        whenever(documentReference.set(any<Map<String, Any>>(), any<SetOptions>()))
            .thenReturn(Tasks.forResult(null))

        val result = repository.updateUserProfile(profile)

        assertTrue(result is Result.Success)
        assertEquals(profile, result.data)
        verify(documentReference).set(
            check<Map<String, Any>> {
                assertEquals(profile.name, it["name"])
                assertEquals(profile.avatar, it["avatar"])
                assertTrue(it.containsKey("lastSeen"))
            },
            eq(SetOptions.merge())
        )
    }

}