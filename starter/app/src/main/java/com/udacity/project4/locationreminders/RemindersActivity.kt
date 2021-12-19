package com.udacity.project4.locationreminders

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityRemindersBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.Companion.REQUEST_TURN_DEVICE_LOCATION_ON


/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    // bind views
    private lateinit var binding: ActivityRemindersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Inflate the layout
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminders)
        setContentView(binding.root)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                binding.navHostFragment.findNavController().popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

//    // !!!!!!!!!!!!!!!!!!!!!!!
//    // redirect the gms "onActivityResult" from activity to fragment
//    // ... see the answer to this question:
//    // ... https://stackoverflow.com/questions/22602988/google-login-not-working-properly-on-android-fragment
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
//            // forward this to the fragment...
//            val fragment: SaveReminderFragment =
//                getSupportFragmentManager().findFragmentById(R.id.clSaveReminderFragment) as SaveReminderFragment
//            fragment.myOnActivityResult(requestCode)
//        } else {
//            super.onActivityResult(requestCode, resultCode, data)
//        }
//    }
}
