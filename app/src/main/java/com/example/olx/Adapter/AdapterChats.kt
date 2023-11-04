package com.example.olx.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olx.Filter.FilterChats
import com.example.olx.Models.ModelChats
import com.example.olx.R
import com.example.olx.activity.chat.ChatActivity
import com.example.olx.databinding.RowChatsBinding
import com.example.olx.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterChats : RecyclerView.Adapter<AdapterChats.HolderChats>, Filterable {

    private var context: Context
    var chatsArrayList: ArrayList<ModelChats>
    private var filterList: ArrayList<ModelChats>

    private var filter: FilterChats? = null

    private companion object {
        private const val TAG = "ADAPTER_CHATS_TAG"
    }

    private lateinit var binding: RowChatsBinding

    private val firebaseAuth: FirebaseAuth

    private var myUid = ""

    constructor(context: Context, chatsArrayList: ArrayList<ModelChats>) : super() {
        this.context = context
        this.chatsArrayList = chatsArrayList
        this.filterList = chatsArrayList

        firebaseAuth = FirebaseAuth.getInstance()
        myUid = "${firebaseAuth.uid}"


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChats {
        binding = RowChatsBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderChats(binding.root)
    }

    override fun onBindViewHolder(holder: HolderChats, position: Int) {
        val modelsChats = chatsArrayList[position]

        loadLastMessage(modelsChats, holder)

        holder.itemView.setOnClickListener {
            val receiptUid = modelsChats.receiptUid

            if (receiptUid != null) {
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("receiptUid", receiptUid)
                context.startActivity(intent)
            }
        }
    }

    private fun loadLastMessage(modelsChats: ModelChats, holder: AdapterChats.HolderChats) {
        val chatKey = modelsChats.chatKey
        Log.d(TAG, "loadLastMessage: chatKey: $chatKey")

        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatKey).limitToLast(1)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {

                        val fromUid = "${ds.child("fromUid").value}"
                        val message = "${ds.child("message").value}"
                        val messageId = "${ds.child("messageId").value}"
                        val messageType = "${ds.child("messageType").value}"
                        val timestamp = ds.child("timestamp").value as Long ?: 0
                        val toUid = "${ds.child("toUid").value}"

                        val formattedDate = Utils.formatTimestampDataTime(timestamp)

                        modelsChats.message = message
                        modelsChats.messageId = messageId
                        modelsChats.messageType = messageType
                        modelsChats.fromUid = fromUid
                        modelsChats.timestamp = timestamp
                        modelsChats.toUid = toUid

                        holder.dateTimeTv.text = "$formattedDate"

                        if (messageType == Utils.MESSAGE_TYPE_TEXT) {
                            holder.lastMessageTv.text = message

                        } else {
                            holder.lastMessageTv.text = "sends Attachment"
                        }
                    }

                    loadReceiptUserInfo(modelsChats, holder)

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

    }

    private fun loadReceiptUserInfo(modelsChats: ModelChats, holder: AdapterChats.HolderChats) {

        val fromUid = modelsChats.fromUid
        val toUid = modelsChats.toUid

        var receiptUid = ""
        if (fromUid == myUid) {
            receiptUid = toUid
        } else {
            receiptUid = fromUid
        }

        Log.d(TAG, "loadReceiptUserInfo: fromUid: $fromUid")
        Log.d(TAG, "loadReceiptUserInfo: toUid: $toUid")
        Log.d(TAG, "loadReceiptUserInfo: receiptUid: $receiptUid")

        modelsChats.receiptUid = receiptUid

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(receiptUid)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = "${snapshot.child("name").value}"
                    val profileImageUri = "${snapshot.child("profileImageUri").value}"

                    modelsChats.name = name
                    modelsChats.profileImageUrl = profileImageUri

                    holder.nameTv.text = name

                    try {
                        Glide.with(context)
                            .load(profileImageUri)
                            .placeholder(R.drawable.ic_person_white)
                            .into(holder.profileIv)

                    } catch (e: java.lang.Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    override fun getItemCount(): Int {
        return chatsArrayList.size
    }

    inner class HolderChats(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var profileIv = binding.ProfilerIv
        var nameTv = binding.NameTv
        var lastMessageTv = binding.lastMessageTv
        var dateTimeTv = binding.dateTimeTv
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterChats(this, filterList)

        }
        return filter as FilterChats
    }


}