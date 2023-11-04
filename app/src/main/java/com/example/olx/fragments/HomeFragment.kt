package com.example.olx.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.example.olx.Adapter.AdapterAd
import com.example.olx.Adapter.AdapterCategory
import com.example.olx.Interface.RvListenerCategory
import com.example.olx.Models.ModelAd
import com.example.olx.Models.ModelCategory
import com.example.olx.activity.Location.LocationPickerActivity
import com.example.olx.databinding.FragmentHomeBinding
import com.example.olx.utils.Utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private companion object {
        private const val TAG = "HOME_TAG"
        private const val MAX_DISTANCE_TO_LOAD_ADS_KM = 10
    }

    private lateinit var mContext: Context

    private lateinit var adArrayList: ArrayList<ModelAd>

    private lateinit var adapterAd: AdapterAd

    private lateinit var locationSp: SharedPreferences

    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAddress = ""

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(LayoutInflater.from(mContext), container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationSp = mContext.getSharedPreferences("LOCATION_SP", Context.MODE_PRIVATE)

        currentLatitude = locationSp.getFloat("CURRENT_LATITUDE", 0.0f).toDouble()
        currentLongitude = locationSp.getFloat("CURRENT_LONGITUDE", 0.0f).toDouble()
        currentAddress = locationSp.getString("CURRENT_ADDRESS", "")!!


        if (currentLatitude != 0.0 && currentLongitude != 0.0) {
            binding.locationTv.text = currentAddress
        }

        loadCategories()

        loadAds("All")

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d(TAG, "onTextChanged: Query: $s")
                try {

                    val query = s.toString()
                    adapterAd.filter.filter(query)

                } catch (e: Exception) {
                    Log.e(TAG, "onTextChanged: ", e)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        binding.locationCv.setOnClickListener {

            val intent = Intent(mContext, LocationPickerActivity::class.java)
            locationPickerActivityResultLauncher.launch(intent)
        }

    }

    private val locationPickerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "locationPickerActivityResultLauncher: RESULT_OK")

            val data = result.data

            if (data != null) {
                Log.d(TAG, "locationPickerActivityResultLauncher: Location Picked!")

                currentLatitude = data.getDoubleExtra("latitude", 0.0)
                currentLongitude = data.getDoubleExtra("longitude", 0.0)
                currentAddress = data.getStringExtra("address").toString()

                locationSp.edit()
                    .putFloat("CURRENT_LATITUDE", currentLatitude.toFloat())
                    .putFloat("CURRENT_LONGITUDE", currentLongitude.toFloat())
                    .putString("CURRENT_ADDRESS", currentAddress)
                    .apply()

                // set the picked address
                binding.locationTv.text = currentAddress

                // after picked address reload all ads again based on newly picked location

                loadAds("All")
            }
        } else {

            Utils.toast(mContext, "Cancelled!")
        }
    }

    private fun loadAds(category: String) {
        Log.d(TAG, "loadAds: category: $category")

        adArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                adArrayList.clear()

                for (ds in snapshot.children) {

                    try {
                        val modelAd = ds.getValue(ModelAd::class.java)

                        val distance = calculateDistanceKm(
                            modelAd?.latitude ?: 0.0,
                            modelAd?.longitude ?: 0.0
                        )

                        Log.d(TAG, "onDataChange: distance : $distance")

                        if (category == "All") {
                            if (distance <= MAX_DISTANCE_TO_LOAD_ADS_KM) {
                                adArrayList.add(modelAd!!)
                            }
                        } else {

                            if (modelAd!!.category.equals(category)) {
                                if (distance <= MAX_DISTANCE_TO_LOAD_ADS_KM) {
                                    adArrayList.add(modelAd)
                                }
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }
                adapterAd = AdapterAd(mContext, adArrayList)
                binding.adsRv.adapter = adapterAd

            }
            override fun onCancelled(error: DatabaseError) {

            }
        })

    }

    private fun calculateDistanceKm(adLatitude: Double, adLongitude: Double): Double {

        Log.d(TAG, "calculateDistanceKm: currentLatitude: $currentLatitude")
        Log.d(TAG, "calculateDistanceKm: currentLongitude: $currentLongitude")
        Log.d(TAG, "calculateDistanceKm: adLatitude: $adLatitude")
        Log.d(TAG, "calculateDistanceKm: adLongitude: $adLongitude")

        val startPoint = Location(LocationManager.NETWORK_PROVIDER)
        startPoint.latitude = adLatitude
        startPoint.longitude = adLongitude

        val endPoint = Location(LocationManager.NETWORK_PROVIDER)
        endPoint.latitude = adLatitude
        endPoint.longitude = adLongitude

        val distanceInMeters = startPoint.distanceTo(endPoint).toDouble()

        return distanceInMeters / 1000

    }

    private fun loadCategories() {

        val categoryArrayList = ArrayList<ModelCategory>()

        for (i in 0 until Utils.categories.size) {

            val modelCategory = ModelCategory(Utils.categories[i], Utils.categoryIcons[i])
            categoryArrayList.add(modelCategory)
        }

        val adapterCategory =
            AdapterCategory(mContext, categoryArrayList, object : RvListenerCategory {
                override fun onCategoryClick(modelCategory: ModelCategory) {

                    val selectedCategory = modelCategory.category
                    loadAds(selectedCategory)
                }
            })

        binding.categoriesRv.adapter = adapterCategory

    }

}