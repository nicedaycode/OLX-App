package com.example.olx.activity.editProfile

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.olx.databinding.ActivityChangePasswordBinding
import com.example.olx.utils.Utils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    private companion object {
        private const val TAG = "CHANGE_PASSWORD_TAG"
    }

    // firebase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    // Dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        // Dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        // handel click
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.submitBtn.setOnClickListener {
            validateDate()
        }
    }

    private var currentPassword = ""
    private var newPassword = ""
    private var confirmNewPassword = ""

    private fun validateDate() {

        currentPassword = binding.currentPasswordEt.text.toString().trim()
        newPassword = binding.newPasswordEt.text.toString().trim()
        confirmNewPassword = binding.confirmNewPasswordEt.text.toString().trim()

        Log.d(TAG, "validateDate: currentPassword: $currentPassword")
        Log.d(TAG, "validateDate: newPassword: $newPassword")
        Log.d(TAG, "validateDate: confirmNewPassword: $confirmNewPassword")

        if (currentPassword.isEmpty()) {
            binding.currentPasswordEt.error = "Enter current password!"
            binding.currentPasswordEt.requestFocus()

        } else if (newPassword.isEmpty()) {
            binding.newPasswordEt.error = " Enter new password!"
            binding.currentPasswordEt.requestFocus()

        } else if (confirmNewPassword.isEmpty()) {
            binding.confirmNewPasswordEt.error = "Enter confirm new password!"
            binding.confirmNewPasswordEt.requestFocus()
        } else if (newPassword != confirmNewPassword) {
            binding.confirmNewPasswordEt.error = "Password doesn't match"
            binding.confirmNewPasswordEt.requestFocus()
        } else {

            authenticateUserForUpdatePassword()
        }
    }

    private fun authenticateUserForUpdatePassword() {

        progressDialog.setMessage("Authenticating User")
        progressDialog.show()


        val authCredential =
            EmailAuthProvider.getCredential(firebaseUser.email.toString(), currentPassword)

        firebaseUser.reauthenticate(authCredential)
            .addOnSuccessListener {

                Log.d(TAG, "authenticateUserForUpdatePassword: Auth success")
                updatePassword()
            }
            .addOnFailureListener { e ->

                Log.d(TAG, "authenticateUserForUpdatePassword: ", e)
                Utils.toast(this, "Failed to authenticate due to ${e.message}")

            }
    }

    private fun updatePassword() {

        progressDialog.setMessage("Changing Password")
        progressDialog.show()

        firebaseUser.updatePassword(newPassword)
            .addOnSuccessListener {

                Log.d(TAG, "updatePassword: Password update.")
                progressDialog.dismiss()
                Utils.toast(this, "Password update...!")
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "updatePassword: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to update password due to ${e.message}")
            }
    }

}