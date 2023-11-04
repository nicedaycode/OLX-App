package com.example.olx.fragments

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.olx.Adapter.AdapterChats
import com.example.olx.Models.ModelChats
import com.example.olx.R
import com.example.olx.databinding.FragmentChatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatsFragment : Fragment() {

    private lateinit var binding: FragmentChatsBinding


    private companion object {
        private const val TAG = "CHATS_TAG"
    }

    private lateinit var mContext: Context
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private lateinit var chatsArrayList: ArrayList<ModelChats>
    private lateinit var adapterChats: AdapterChats

    private var myUid = ""

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        myUid = "${firebaseAuth.uid}"

        Log.d(TAG, "onViewCreated: myUid: $myUid")

        loadChats()


        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d(TAG, "onTextChanged: Query: $s")
                try {

                    val query = s.toString()
                    Log.d(TAG, "onTextChanged: Search Query: $query")
                    
                    adapterChats.filter.filter(query)

                } catch (e: Exception) {
                    Log.e(TAG, "onTextChanged: ", e)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }


    private fun loadChats() {

        Log.d(TAG, "loadChats: ")

        chatsArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatsArrayList.clear()

                for (ds in snapshot.children) {
                    val chatKey = "${ds.key}"
                    Log.d(TAG, "onDataChange: chatKey: $chatKey")

                    if (chatKey.contains(myUid)) {
                        Log.d(TAG, "onDataChange: Contains, Add to list")

                        val modelChats = ModelChats()
                        modelChats.chatKey = chatKey

                        chatsArrayList.add(modelChats)
                    } else {
                        Log.d(TAG, "onDataChange: Not contains, Skip")
                    }
                }

                adapterChats = AdapterChats(mContext, chatsArrayList)
                binding.chatsRv.adapter = adapterChats
                sort()
            }

            override fun onCancelled(error: DatabaseError) {
                
            }

        })
    }

    private fun sort() {
        Handler().postDelayed({
            chatsArrayList.sortWith { model1: ModelChats, model2: ModelChats ->
                model2.timestamp.compareTo(model1.timestamp)
            }
            adapterChats.notifyDataSetChanged()
        }, 1000)
    }


}