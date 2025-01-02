package com.mayor.kavi.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.*
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AuthRepositoryTest {
    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firestore: FirebaseFirestore

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var authResult: AuthResult

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    @Mock
    private lateinit var collectionReference: CollectionReference

    @Mock
    private lateinit var documentReference: DocumentReference

    @Mock
    private lateinit var documentSnapshot: DocumentSnapshot

    @Mock
    private lateinit var querySnapshot: QuerySnapshot

    private lateinit var repository: AuthRepositoryImpl
    private val testUserId = "test-user-id"

    @Before
    fun setup() {
        repository = AuthRepositoryImpl(firebaseAuth, firestore, userRepository)

        whenever(firestore.collection(any())).thenReturn(collectionReference)
        whenever(collectionReference.document(any())).thenReturn(documentReference)
        whenever(userRepository.getCurrentUserId()).thenReturn(testUserId)
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(documentReference.set(any<UserProfile>(), any())).thenReturn(mock())
    }

    @Test
    fun `isUserSignedIn returns true when user is signed in`() {
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        assertTrue(repository.isUserSignedIn())
    }

    @Test
    fun `isUserSignedIn returns false when user is not signed in`() {
        whenever(firebaseAuth.currentUser).thenReturn(null)
        assertFalse(repository.isUserSignedIn())
    }

    @Test
    fun `signInUser with email succeeds`() = runTest {
        // Setup
        val email = "test@example.com"
        val password = "password"
        val userProfile = UserProfile(testUserId, "Test User", email)

        // Mock Firebase auth
        val signInTask = Tasks.forResult(authResult)
        whenever(firebaseAuth.signInWithEmailAndPassword(email, password))
            .thenReturn(signInTask)
        whenever(authResult.user).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(testUserId)

        // Mock user repository
        whenever(userRepository.getUserById(testUserId))
            .thenReturn(Result.Success(userProfile))

        // Execute
        val results = mutableListOf<Result<AuthResult>>()
        repository.signInUser(email, password).collect { results.add(it) }

        // Verify
        assertTrue(results.any { it is Result.Loading })
        assertTrue(results.last() is Result.Success)
        verify(firebaseAuth).signInWithEmailAndPassword(email, password)
        verify(userRepository).getUserById(testUserId)
    }

    @Test
    fun `signInUser with username succeeds`() = runTest {
        val username = "testuser"
        val email = "test@example.com"
        val password = "password"
        val userProfile = UserProfile(testUserId, username, email)

        // Mock username to email query
        val query = mock<Query>()
        whenever(collectionReference.whereEqualTo("name", username)).thenReturn(query)
        whenever(query.get()).thenReturn(Tasks.forResult(querySnapshot))
        whenever(querySnapshot.documents).thenReturn(listOf(documentSnapshot))
        whenever(documentSnapshot.get("email")).thenReturn(email)


        // Mock Firebase auth
        val signInTask = Tasks.forResult(authResult)
        whenever(firebaseAuth.signInWithEmailAndPassword(email, password))
            .thenReturn(signInTask)
        whenever(authResult.user).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(testUserId)


        // Mock user repository with Result.Success
        whenever(userRepository.getUserById(testUserId))
            .thenReturn(Result.Success(userProfile))

        // Execute and collect
        val results = mutableListOf<Result<AuthResult>>()
        repository.signInUser(username, password).collect { results.add(it) }


        // Verify
        assertTrue(results.any { it is Result.Loading })
        assertTrue(results.last() is Result.Success)
        verify(firebaseAuth).signInWithEmailAndPassword(email, password)
        verify(userRepository).getUserById(testUserId)
    }

    @Test
    fun `googleSignIn succeeds with valid credential`() = runTest {
        // Setup test data
        val credential = mock<AuthCredential>()
        val email = "test@gmail.com"
        val displayName = "Test User"
        val testUserId = "test-uid"

        // Mock Firebase Auth
        whenever(firebaseAuth.signInWithCredential(credential))
            .thenReturn(Tasks.forResult(authResult))
        whenever(authResult.user).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn(testUserId)
        whenever(firebaseUser.email).thenReturn(email)
        whenever(firebaseUser.displayName).thenReturn(displayName)

        // Mock Firestore
        whenever(firestore.collection("users")).thenReturn(collectionReference)
        whenever(collectionReference.document(testUserId)).thenReturn(documentReference)
        whenever(documentReference.set(any(), any())).thenReturn(Tasks.forResult(null))

        // Mock UserRepository
        whenever(userRepository.getCurrentUserId()).thenReturn(testUserId)
        whenever(userRepository.getUserById(testUserId))
            .thenReturn(
                Result.Success(
                    UserProfile(
                        id = testUserId,
                        name = displayName,
                        email = email,
                        avatar = Avatar.DEFAULT,
                        lastSeen = System.currentTimeMillis()
                    )
                )
            )

        // Execute
        val results = mutableListOf<Result<AuthResult>>()
        repository.googleSignIn(credential).collect { results.add(it) }

        // Verify
        assertTrue(results.any { it is Result.Loading })
        assertTrue(results.last() is Result.Success)
        verify(firebaseAuth).signInWithCredential(credential)
        verify(documentReference).set(any(), any())
    }

    @Test
    fun `createUser succeeds with valid email`() = runTest {
        val username = "new-user"
        val email = "test@example.com"
        val password = "password"
        val testUserId = "test-uid"

        // Mock username availability check
        val query = mock<Query>()
        val queryTask = Tasks.forResult(querySnapshot)
        whenever(collectionReference.whereEqualTo("name", username)).thenReturn(query)
        whenever(query.get()).thenReturn(queryTask)
        whenever(querySnapshot.isEmpty).thenReturn(true)

        // Mock successful user creation
        val signUpTask = Tasks.forResult(authResult)
        whenever(firebaseAuth.createUserWithEmailAndPassword(email, password))
            .thenReturn(signUpTask)
        whenever(userRepository.getCurrentUserId()).thenReturn(testUserId)

        // Execute and collect
        val results = mutableListOf<Result<AuthResult>>()
        repository.createUser(username, email, password).collect { results.add(it) }

        // Verify
        assertTrue(results.any { it is Result.Loading })
        assertTrue(results.last() is Result.Success)
        verify(firebaseAuth).createUserWithEmailAndPassword(email, password)
        verify(userRepository, times(2)).getCurrentUserId()
    }

    @Test
    fun `signOut calls Firebase signOut`() {
        repository.signOut()
        verify(firebaseAuth).signOut()
    }
}