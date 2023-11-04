package com.example.olx.activity.chat

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.olx.Adapter.AdapterChat
import com.example.olx.Models.ModelChat
import com.example.olx.R
import com.example.olx.databinding.ActivityChatBinding
import com.example.olx.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding

    private companion object {
        private const val TAG = "CHAT_TAG"
    }

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    private var receiptUid = ""
    private var receiptFcmToken = ""

    private var myUid = ""
    private var myName = ""

    private var chatPath = ""

    private var imageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        receiptUid = intent.getStringExtra("receiptUid")!!

        myUid = firebaseAuth.uid!!

        chatPath = Utils.chatPath(receiptUid, myUid)

        loadReceiptDetails()
        loadMessages()

        binding.toolbarBackBtn.setOnClickListener {
            finish()
        }

        binding.attachFab.setOnClickListener {
            imagePickDialog()

        }

        binding.sentFab.setOnClickListener {

            validateDate()
        }

    }

    private fun loadMyInfo() {
        Log.d(TAG, "loadMyInfo: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    myName = "${snapshot.child("name").value}"
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun loadReceiptDetails() {
        Log.d(TAG, "loadSellerDetails: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(receiptUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    try {
                        val name = "${snapshot.child("name").value}"
                        val profileImageUri = "${snapshot.child("profileImageUri").value}"
                        receiptFcmToken = "${snapshot.child("fcmToken").value}"
                        Log.d(TAG, "onDataChange: name: $name")
                        Log.d(TAG, "onDataChange: profileImageUri: $profileImageUri")
                        Log.d(TAG, "onDataChange: receiptFcmToken: $receiptFcmToken")

                        binding.toolbarTitleTv.text = name

                        try {
                            Glide.with(this@ChatActivity)
                                .load(profileImageUri)
                                .placeholder(R.drawable.ic_person_white)
                                .into(binding.toolbarProfilerIv)

                        } catch (e: java.lang.Exception) {
                            Log.e(TAG, "onDataChange: ", e)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadMessages() {
        Log.d(TAG, "loadMessages: ")

        val messageArrayList = ArrayList<ModelChat>()

        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatPath)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    messageArrayList.clear()

                    for (ds: DataSnapshot in snapshot.children) {
                        try {

                            val modelChat = ds.getValue(ModelChat::class.java)
                            messageArrayList.add(modelChat!!)

                        } catch (e: Exception) {
                            Log.e(TAG, "onDataChange: ", e)
                        }
                    }

                    val adapterChat = AdapterChat(this@ChatActivity, messageArrayList)
                    binding.chatRv.adapter = adapterChat

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    // popup Menu
    private fun imagePickDialog() {

        val popupMenu = PopupMenu(this, binding.attachFab)

        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->

            val itemId = item.itemId

            if (itemId == 1) {
                Log.d(
                    TAG,
                    "imagePickDialog: Camera Clicked, check if camera permission(s) granted or not"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestCameraPermission.launch(arrayOf(Manifest.permission.CAMERA))
                } else {
                    requestCameraPermission.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )

                }

            } else if (itemId == 2) {
                Log.d(
                    TAG,
                    "imagePickDialog: Gallery, check if storage permission(s) granted or not"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickImageGallery()
                } else {
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            true
        }
    }

    // Permissions

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            Log.d(TAG, "requestCameraPermission: result: $result")

            var areAllGranted = true
            for (isGranted in result.values) {
                areAllGranted = areAllGranted && isGranted
            }

            if (areAllGranted) {
                Log.d(TAG, "requestCameraPermission: All granted e.g Camera, Storage")
                pickImageCamera()
            } else {
                Log.d(TAG, "requestCameraPermission: All or either one is denied...")
                Utils.toast(this, "Camera or Storage otr both permissions denied")
            }

        }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "requestStoragePermission: isGranted $isGranted")
            if (isGranted) {
                pickImageGallery()
            } else {
                Utils.toast(this, "Storage permission denied")
            }
        }

    // Camera
    private fun pickImageCamera() {

        Log.d(TAG, "pickImageCamera: ")

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_IMAGE_TITLE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_IMAGE_DESCRIPTION")
        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "cameraActivityResultLauncher: imageUri: $imageUri")

                uploadToFirebaseStorage()

                /*try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.toolbarProfilerIv)
                } catch (e: Exception) {
                    Log.d(TAG, "cameraActivityResultLauncher: ", e)
                }*/

            } else {
                Utils.toast(this, "Cancelled")
            }
        }

    // Gallery
    private fun pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                imageUri = data!!.data
                Log.d(TAG, "galleryActivityResultLauncher: imageUri: $imageUri")

                uploadToFirebaseStorage()

                /*try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.toolbarProfilerIv)
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "galleryActivityResultLauncher: ", e)
                }*/
            } else {
                Utils.toast(this, "Cancelled...!")
            }
        }

    private fun uploadToFirebaseStorage() {

        Log.d(TAG, "uploadToFirebaseStorage: ")

        progressDialog.setMessage("Uploading image...!")
        progressDialog.show()

        val timestamp = Utils.getTimestamp()

        val filePathAndName = "ChatImage/$timestamp"

        val storageRef = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageRef.putFile(imageUri!!)


            .addOnProgressListener { snapshot ->
                val progress = 100.0 * snapshot.bytesTransferred / snapshot.totalByteCount

                progressDialog.setMessage("Uploading image: Progress: ${progress.toUInt()} %")
            }
            .addOnSuccessListener { taskSnapshot ->

                val uriTask = taskSnapshot.storage.downloadUrl

                while (!uriTask.isSuccessful);

                val uploadedImageUrl = uriTask.result.toString()
                if (uriTask.isSuccessful) {

                    sendMessage(Utils.MESSAGE_TYPE_IMAGE, uploadedImageUrl, timestamp)
                }
            }
            .addOnFailureListener { e ->

                progressDialog.dismiss()
                Log.d(TAG, "uploadToFirebaseStorage: ", e)
                Utils.toast(this, "Failed to upload due to ${e.message}")

            }

    }

    private fun validateDate() {
        Log.d(TAG, "validateDate: ")

        val message = binding.messageEt.text.toString().trim()
        val timestamp = Utils.getTimestamp()

        if (message.isEmpty()) {

            Utils.toast(this, "Enter message to send...")
        } else {

            sendMessage(Utils.MESSAGE_TYPE_TEXT, message, timestamp)
        }

    }

    private fun sendMessage(messageType: String, message: String, timestamp: Long) {

        Log.d(TAG, "sendMessage: messageType: $messageType")
        Log.d(TAG, "sendMessage: message: $message")
        Log.d(TAG, "sendMessage: timestamp: $timestamp")

        progressDialog.setMessage("Sending message...!")
        progressDialog.show()

        val refChat = FirebaseDatabase.getInstance().getReference("Chats")

        val keyTd = "${refChat.push().key}"

        val hashMap = HashMap<String, Any>()
        hashMap["messageId"] = "$keyTd"
        hashMap["messageType"] = "$messageType"
        hashMap["message"] = "$message"
        hashMap["fromUid"] = "$myUid"
        hashMap["toUid"] = "$receiptUid"
        hashMap["timestamp"] = timestamp

        refChat.child(chatPath)
            .child(keyTd)
            .setValue(hashMap)
            .addOnSuccessListener {

                Log.d(TAG, "sendMessage: message sent")
                progressDialog.dismiss()
                binding.messageEt.setText("")

                if (messageType == Utils.MESSAGE_TYPE_TEXT) {
                    prepareNotification(message)
                } else {
                    prepareNotification("Send an attachment")
                }

            }
            .addOnFailureListener { e ->

                Log.e(TAG, "sendMessage: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to send due to ${e.message}")


            }

    }

    private fun prepareNotification(message: String) {
        Log.d(TAG, "prepareNotification: ")

        val notificationJo = JSONObject()
        val notificationDataJo = JSONObject()
        val notificationNotificationJo = JSONObject()

        try {
            notificationDataJo.put("notificationType", "${Utils.NOTIFICATION_TYPE_NEW_MESSAGE}")
            notificationDataJo.put("senderUid", "${firebaseAuth.uid}")

            notificationNotificationJo.put("title", "$myName")
            notificationNotificationJo.put("body", "$message")
            notificationNotificationJo.put("sound", "default")

            notificationJo.put("to", "$receiptFcmToken")
            notificationJo.put("notification", notificationNotificationJo)
            notificationJo.put("data", notificationDataJo)


        } catch (e: Exception) {
            Log.e(TAG, "prepareNotification: ", e)
        }

        sendFcmNotification(notificationJo)

    }

    private fun sendFcmNotification(notificationJo: JSONObject) {

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            "https://fcm.googleapis.com/fcm/send",
            notificationJo,
            Response.Listener {

                Log.d(TAG, "sendFcmNotification: Notification Send $it")
            },
            Response.ErrorListener { e ->

                Log.e(TAG, "sendFcmNotification: ", e)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "key=${Utils.FCM_SERVER_KEY}"

                return headers
            }
        }

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

}