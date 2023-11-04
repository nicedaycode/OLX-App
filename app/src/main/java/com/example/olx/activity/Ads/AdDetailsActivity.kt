package com.example.olx.activity.Ads

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import com.bumptech.glide.Glide
import com.example.olx.Adapter.AdapterImageSlider
import com.example.olx.Models.ModelAd
import com.example.olx.Models.ModelImageSlider
import com.example.olx.R
import com.example.olx.activity.chat.ChatActivity
import com.example.olx.databinding.ActivityAdDetailsBinding
import com.example.olx.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdDetailsBinding

    private companion object {
        private const val TAG = "AD_DETAILS_TAG"
    }

    private lateinit var firebaseAuth: FirebaseAuth

    private var adId = ""

    private var adLatitude = 0.0
    private var adLongitude = 0.0

    private var sellerUid = ""
    private var sellerPhone = ""

    private var favorite = false

    private lateinit var imageSliderArrayList: ArrayList<ModelImageSlider>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarEditBtn.visibility = View.GONE
        binding.toolbarDeleteBtn.visibility = View.GONE
        binding.chatBtn.visibility = View.GONE
        binding.callBtn.visibility = View.GONE
        binding.smsBtn.visibility = View.GONE

        firebaseAuth = FirebaseAuth.getInstance()

        adId = intent.getStringExtra("adId").toString()

        Log.d(TAG, "onCreate: adId: $adId")

        if (firebaseAuth.currentUser != null) {

            checkIsFavorite()
        }

        loadAdDetails()
        loadAdImage()

        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.toolbarDeleteBtn.setOnClickListener {

            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this)
            materialAlertDialogBuilder.setTitle("Delete Ad")
                .setMessage("Are you sure you delete this Ad?")
                .setPositiveButton("DELETE") { dialog, which ->
                    Log.d(TAG, "onCreate: DELETE clicked")
                    deleteAd()
                }
                .setNegativeButton("CANCEL") { dialog, which ->
                    Log.d(TAG, "onCreate: CANCEL clicked....")
                    dialog.dismiss()
                }
                .show()
        }

        binding.toolbarEditBtn.setOnClickListener {

            editOptionsDialog()

        }

        binding.toolbarFavBtn.setOnClickListener {

            if (favorite) {

                Utils.removeFromFavorite(this, adId)
            } else {

                Utils.addToFavorite(this, adId)

            }
        }

        binding.sellerProfileCv.setOnClickListener {

            val intent = Intent(this, AdSellerProfileActivity::class.java )
            intent.putExtra("sellerUid", sellerUid)
            startActivity(intent)

        }

        binding.chatBtn.setOnClickListener {

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiptUid", sellerUid)
            startActivity(intent)
        }

        binding.callBtn.setOnClickListener {
            Utils.callIntent(this, sellerPhone)
        }

        binding.smsBtn.setOnClickListener {
            Utils.smsIntent(this, sellerPhone)
        }

        binding.mapBtn.setOnClickListener {
            Utils.mapIntent(this, adLatitude, adLongitude)
        }

    }

    private fun editOptionsDialog() {
        Log.d(TAG, "editOptionsDialog: ")

        val popupMenu = PopupMenu(this, binding.toolbarEditBtn)

        popupMenu.menu.add(Menu.NONE, 0, 0, "Edit")
        popupMenu.menu.add(Menu.NONE, 1, 1, "Mark As Sold")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->

            val itemId = menuItem.itemId

            if (itemId == 0) {
                val intent = Intent(this, AdCreateActivity::class.java)
                intent.putExtra("isEditMode", true)
                intent.putExtra("adId", adId)
                startActivity(intent)
            } else {
                showMarkSoldDialog()
            }

            return@setOnMenuItemClickListener true
        }
    }


    private fun showMarkSoldDialog() {
        Log.d(TAG, "showMarkSoldDialog: ")

        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        alertDialogBuilder.setTitle("Mark as sold")
            .setMessage("Are you sure you want to mark this Ad as sold?")
            .setPositiveButton("SOLD") { dialog, which ->

                Log.d(TAG, "showMarkSoldDialog: SOLD clicked")

                val hashMap = HashMap<String, Any>()
                hashMap["status"] = "${Utils.AD_STATUS_SOLD}"

                val ref = FirebaseDatabase.getInstance().getReference("Ads")
                ref.child(adId)
                    .updateChildren(hashMap)
                    .addOnSuccessListener {

                        Log.d(TAG, "showMarkSoldDialog: Marked as sold")
                    }
                    .addOnFailureListener { e ->

                        Log.e(TAG, "showMarkSoldDialog: ", e)
                        Utils.toast(this, "Failed to mark as sold due to ${e.message}")
                    }
            }
            .setNegativeButton("CANCEL") { dialog, which ->

                Log.d(TAG, "showMarkSoldDialog: CANCEL clicked")
                dialog.dismiss()
            }
            .show()
    }

    private fun loadAdDetails() {
        Log.d(TAG, "loadAdDetails: ")

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    try {
                        val modelAd = snapshot.getValue(ModelAd::class.java)

                        sellerUid = "${modelAd!!.uid}"
                        val title = modelAd.title
                        val description = modelAd.description
                        val address = modelAd.address
                        val condition = modelAd.condition
                        val price = modelAd.price
                        val timestamp = modelAd.timestamp
                        val category = modelAd.category
                        adLatitude = modelAd.latitude
                        adLongitude = modelAd.longitude

                        val formattedDate = Utils.formatTimestampData(timestamp)

                        if (sellerUid == firebaseAuth.uid) {

                            binding.toolbarEditBtn.visibility = View.VISIBLE
                            binding.toolbarDeleteBtn.visibility = View.VISIBLE

                            binding.chatBtn.visibility = View.GONE
                            binding.callBtn.visibility = View.GONE
                            binding.smsBtn.visibility = View.GONE
                            binding.sellerProfileLabelTv.visibility = View.GONE
                            binding.sellerProfileCv.visibility = View.GONE

                        } else {

                            binding.toolbarEditBtn.visibility = View.GONE
                            binding.toolbarDeleteBtn.visibility = View.GONE

                            binding.chatBtn.visibility = View.VISIBLE
                            binding.callBtn.visibility = View.VISIBLE
                            binding.smsBtn.visibility = View.VISIBLE
                            binding.sellerProfileLabelTv.visibility = View.VISIBLE
                            binding.sellerProfileCv.visibility = View.VISIBLE



                        }

                        binding.titleTv.text = title
                        binding.descriptionTv.text = description
                        binding.addressTv.text = address
                        binding.conditionTv.text = condition
                        binding.categoryTv.text = category
                        binding.priceTv.text = price
                        binding.dateTv.text = formattedDate

                        loadSellerDetails()


                    } catch (e: Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

    }

    private fun loadSellerDetails() {
        Log.d(TAG, "loadSellerDetails: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(sellerUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val phoneCode = "${snapshot.child("phoneCode").value}"
                    val phoneNumber = "${snapshot.child("phoneNumber").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImageUri = "${snapshot.child("profileImageUri").value}"
                    val timestamp = snapshot.child("timesTamp").value as Long

                    val formattedDate = Utils.formatTimestampData(timestamp)

                    sellerPhone = "$phoneCode$phoneNumber"

                    binding.sellerNameTv.text = name
                    binding.memberSinceTv.text = formattedDate

                    try {
                        Glide.with(this@AdDetailsActivity)
                            .load(profileImageUri)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.sellerProfilerIv)

                    } catch (e: java.lang.Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

    }

    private fun checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}").child("Favorites").child(adId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    favorite = snapshot.exists()
                    Log.d(TAG, "onDataChange: favorite: $favorite")

                    if (favorite) {
                        binding.toolbarFavBtn.setImageResource(R.drawable.ic_favorite_yes)
                    } else {
                        binding.toolbarFavBtn.setImageResource(R.drawable.ic_fav_no)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun loadAdImage() {
        Log.d(TAG, "loadAdImage: ")

        imageSliderArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId).child("Images")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    imageSliderArrayList.clear()

                    for (ds in snapshot.children) {

                        try {
                            val modelImageSlider = ds.getValue(ModelImageSlider::class.java)

                            imageSliderArrayList.add(modelImageSlider!!)

                        } catch (e: Exception) {
                            Log.e(TAG, "onDataChange: ", e)
                        }
                    }

                    val adapterImageSlider =
                        AdapterImageSlider(this@AdDetailsActivity, imageSliderArrayList)
                    binding.imageSliderVp.adapter = adapterImageSlider
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun deleteAd() {
        Log.d(TAG, "deleteAd: ")

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId)
            .removeValue()
            .addOnSuccessListener {

                Log.d(TAG, "deleteAd: Deleted")
                Utils.toast(this, "Deleted...!")
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "deleteAd: ", e)
                Utils.toast(this, "Failed to delete due to ${e.message}")
            }
    }

}