package com.example.olx.activity.signIn.logIn

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import com.example.olx.databinding.ActivityForgotPasswordBinding
import com.example.olx.utils.Utils
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    private companion object {
        private const val TAG = "FORGOT_PASSWORD"
    }

    // firebase
    private lateinit var firebaseAuth: FirebaseAuth

    // Dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // firebase
        firebaseAuth = FirebaseAuth.getInstance()

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

    private var email = ""

    private fun validateDate() {

        email = binding.emailEt.text.toString().trim()

        Log.d(TAG, "validateDate: email: $email")

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEt.error = "Invalid Email Pattern!"
            binding.emailEt.requestFocus()
        } else {

            sendPasswordRecoveryInstructions()

        }

    }

    private fun sendPasswordRecoveryInstructions() {

        Log.d(TAG, "sendPasswordRecoveryInstructions: ")

        progressDialog.setMessage("Sending password reset instructions to $email")
        progressDialog.show()


        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {

                progressDialog.dismiss()
                Utils.toast(this, "instructions to reset password sent to $email")
            }
            .addOnFailureListener { e ->

                Log.d(TAG, "sendPasswordRecoveryInstructions: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to sent due to ${e.message}")

            }

    }

}