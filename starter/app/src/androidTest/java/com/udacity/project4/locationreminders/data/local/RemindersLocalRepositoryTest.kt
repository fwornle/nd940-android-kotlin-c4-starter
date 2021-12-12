package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // test data for (fake) DB
    private lateinit var reminderDtoList: MutableList<ReminderDTO>
    private lateinit var newReminderDTO: ReminderDTO

    // fake data source (repo)
    private lateinit var reminderRepo: ReminderDataSource

    // fake DB (room, in-memory)
    private lateinit var fakeDB: RemindersDatabase
    private lateinit var dao: RemindersDao

    // populate the fake DB / repo
    private suspend fun populateFakeDB() {
        reminderDtoList.map {
            reminderRepo.saveReminder(it)
        }
    }

    // specify query and transaction executors for Room
    // ... necessary to avoid "java.lang.IllegalStateException: This job has not completed yet"
    //     error, see: https://medium.com/@eyalg/testing-androidx-room-kotlin-coroutines-2d1faa3e674f
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)


    @Before
    fun setUp() {

        // generate some test database items (location reminders)
        reminderDtoList = mutableListOf<ReminderDTO>()

        // generate some test data
        for (idx in 0..19) {
            reminderDtoList.add(
                ReminderDTO(
                    "test title $idx",
                    "test description $idx",
                    "test location $idx",
                    idx.toDouble(),
                    idx.toDouble(),
                    UUID.randomUUID().toString(),
                )
            )
        }

        // initialize a new reminder for 'saveReminder' test
        newReminderDTO = ReminderDTO(
            "a new title",
            "a new test description",
            "a new test location",
            42.0,
            242.0,
            UUID.randomUUID().toString(),
        )

        // create fake datasource ... also using the DAO
        fakeDB = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java,
        )
            // use TestCoroutineDispatcher for Room 'transaction executions' - this way, all
            // suspended functions of Room's DAO run inside the same Coroutine scope which is also
            // used for the tests (testScope.runblocking { ... } - see below)
            .setTransactionExecutor(testDispatcher.asExecutor())
            .setQueryExecutor(testDispatcher.asExecutor())
            .allowMainThreadQueries()
            .build()

        // fetch DAO
        dao = fakeDB.reminderDao()

        // create repository with DAO of fake DB
        reminderRepo = RemindersLocalRepository(
            dao,
            // ensure Room (& the DAO & the repo) and the test use the same coroutine dispatcher
            testDispatcher,
        )

    }  // setUp()


    /*
     * to be tested: repo class
     *
     *  interface ReminderDataSource {
     *      suspend fun getReminders(): Result<List<ReminderDTO>>
     *      suspend fun saveReminder(reminder: ReminderDTO)
     *      suspend fun getReminder(id: String): Result<ReminderDTO>
     *      suspend fun deleteAllReminders()
     *  }
     *
     */

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    // test repo interface method 'getReminders'
    // ... use our own TestCoroutineScope (testScope, see above) to ensure that both the test and
    //     Room's DAO functions run in the same scope
    @Test
    @Suppress("UNCHECKED_CAST")
    fun repository_getReminders() = testScope.runBlockingTest {

        // store test data in fake DB
        populateFakeDB()

        // fetch reminders from (fake) repo

        when (val result = reminderRepo.getReminders()) {
            is Result.Success<*> -> {

                // check all data records
                (result.data as List<ReminderDTO>).mapIndexed { idx, reminder ->
                    // check for equality
                    assertThat(reminder, CoreMatchers.notNullValue())
                    assertThat(reminder.id, `is`(reminderDtoList[idx].id))
                    assertThat(reminder.title, `is`(reminderDtoList[idx].title))
                    assertThat(reminder.description, `is`(reminderDtoList[idx].description))
                    assertThat(reminder.location, `is`(reminderDtoList[idx].location))
                    assertThat(reminder.latitude, `is`(reminderDtoList[idx].latitude))
                    assertThat(reminder.longitude, `is`(reminderDtoList[idx].longitude))

                }

            }
            is Result.Error ->
                assertThat(result.message,
                    `is`("Could not fetch reminders from (fake) local storage."))
        }

    }


    // test repo interface method 'getReminder' - existing reminder
    @Test
    @Suppress("UNCHECKED_CAST")
    fun repository_getReminderReminderExistsInDB() = testScope.runBlockingTest {

        // store test data in fake DB
        populateFakeDB()

        // fetch first reminderer from (fake) repo
        val idx = 4

        when (val result = reminderRepo.getReminder(reminderDtoList[idx].id)) {
            is Result.Success<*> -> {

                // check all data records
                (result.data as ReminderDTO).let {
                    // check for equality
                    assertThat(it, CoreMatchers.notNullValue())
                    assertThat(it.id, `is`(reminderDtoList[idx].id))
                    assertThat(it.title, `is`(reminderDtoList[idx].title))
                    assertThat(it.description, `is`(reminderDtoList[idx].description))
                    assertThat(it.location, `is`(reminderDtoList[idx].location))
                    assertThat(it.latitude, `is`(reminderDtoList[idx].latitude))
                    assertThat(it.longitude, `is`(reminderDtoList[idx].longitude))

                }

            }
            is Result.Error ->
                assertThat(result.message,
                    `is`("Reminder not found!"))
        }

    }


    // test repo interface method 'getReminder' - non-existing reminder
    @Test
    fun repository_getReminderReminderDoesNotExistsInDB() = testScope.runBlockingTest {

        // store test data in fake DB
        populateFakeDB()

        // attempt to fetch non-existing reminderer from (fake) repo
        val nonId = "this-index-does-not-exist-in-DB"
        when (val result = reminderRepo.getReminder(nonId)) {
            is Result.Error ->
                assertThat(result.message,
                    `is`("Reminder not found!"))
            else -> {
                assertThat("this should",
                    `is`("not happen (exception during getReminder)"))
            }
        }

    }


    // test repo interface method 'deleteAllReminders'
    @Test
    @Suppress("UNCHECKED_CAST")
    fun repository_deleteAllReminders() = testScope.runBlockingTest {

        // store test data in fake DB
        populateFakeDB()

        // purge all items from (fake) repo, the read all reminders
        reminderRepo.deleteAllReminders()

        when (val result = reminderRepo.getReminders()) {
            is Result.Success<*> -> {
                assertThat((result.data as List<ReminderDTO>).size, `is`(0))
            }
            is Result.Error ->
                assertThat("this should", `is`("not happen"))
        }

    }


    // test repo interface method 'saveReminder'
    @Test
    @Suppress("UNCHECKED_CAST")
    fun repository_saveReminder() = testScope.runBlockingTest {

        // store test data in fake DB
        populateFakeDB()

        // save new reminder to (fake) repo, then read it back
        reminderRepo.saveReminder(newReminderDTO)

        when (val result = reminderRepo.getReminder(newReminderDTO.id)) {
            is Result.Success<*> -> {

                // check the read back data record
                (result.data as ReminderDTO).let {
                    // check for equality
                    assertThat(it, CoreMatchers.notNullValue())
                    assertThat(it.id, `is`(newReminderDTO.id))
                    assertThat(it.title, `is`(newReminderDTO.title))
                    assertThat(it.description, `is`(newReminderDTO.description))
                    assertThat(it.location, `is`(newReminderDTO.location))
                    assertThat(it.latitude, `is`(newReminderDTO.latitude))
                    assertThat(it.longitude, `is`(newReminderDTO.longitude))

                }

            }
            is Result.Error ->
                assertThat(result.message,
                    `is`("Reminder not found!"))
        }

    }

}