package com.example.olx.activity.signIn.register

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import com.example.olx.activity.main.MainActivity
import com.example.olx.databinding.ActivityRegisterEmailBinding
import com.example.olx.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterEmailActivity : AppCompatActivity() {
    lateinit var binding: ActivityRegisterEmailBinding

    private companion object {
        private const val TAG = "REGISTER_TAG"
    }

    // firebase
    private lateinit var firebaseAuth: FirebaseAuth

    // Dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterEmailBinding.inflate(layoutInflater)
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

        binding.haveaccountTv.setOnClickListener {
            onBackPressed()
        }

        binding.registerBtn.setOnClickListener {
            validateData()
        }

    }

    private var email = ""
    private var password = ""
    private var cpassword = ""

    private fun validateData() {

        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        cpassword = binding.confirmPasswordEt.text.toString().trim()

        Log.d(TAG, "validateData: email: $email")
        Log.d(TAG, "validateData: password: $password")
        Log.d(TAG, "validateData: confirm password: $cpassword")

        // validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEt.error = "Invalid Email Format"
            binding.emailEt.requestFocus()
        } else if (password.isEmpty()) {
            binding.passwordEt.error = "Enter Password"
            binding.passwordEt.requestFocus()
        } else if (cpassword.isEmpty()) {
            binding.confirmPasswordEt.error = "Enter Confirm Password"
            binding.confirmPasswordEt.requestFocus()
        } else if (password != cpassword) {
            binding.confirmPasswordEt.error = "Password Doesn't Match"
            binding.confirmPasswordEt.requestFocus()
        } else {
            registerUser()
        }

    }

    private fun registerUser() {
        Log.d(TAG, "registerUser: ")

        // show progress
        progressDialog.setMessage("Creating account")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {

            Log.d(TAG, "registerUser: Register Success")
            updateUserInfo()

            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()

        }.addOnFailureListener { e ->

            Log.d(TAG, "registerUser: ", e)
            progressDialog.dismiss()

            Utils.toast(this, "Failed to create account due to ${e.message}")
        }
    }

    private fun updateUserInfo() {
        Log.d(TAG, "updateUserInfo: ")
        progressDialog.setMessage("Saving User Info")

        val timestamp = Utils.getTimestamp()
        val registeredUserEmail = firebaseAuth.currentUser!!.email
        val registeredUserUid = firebaseAuth.uid

        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = ""
        hashMap["phoneCode"] = ""
        hashMap["phoneNumber"] = ""
        hashMap["profileImageUrl"] = ""
        hashMap["dob"] = ""
        hashMap["userType"] = "Email"
        hashMap["typingTo"] = ""
        hashMap["timesTamp"] = timestamp
        hashMap["onlineStatus"] = true
        hashMap["email"] = "$registeredUserEmail"
        hashMap["uid"] = registeredUserUid

        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(registeredUserUid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "updateUserInfo: User registered...")
                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()

            }.addOnFailureListener {e ->

                Log.d(TAG, "updateUserInfo: ",e)
                progressDialog.dismiss()

                Utils.toast(this, "Failed to save user info due to ${e.message}")

            }
    }


}