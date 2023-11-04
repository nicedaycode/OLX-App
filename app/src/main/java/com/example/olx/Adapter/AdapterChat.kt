package com.example.olx.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olx.Models.ModelChat
import com.example.olx.R
import com.example.olx.utils.Utils
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth

class AdapterChat : RecyclerView.Adapter<AdapterChat.HolderChat> {

    private var context: Context
    private val chatArrayList: ArrayList<ModelChat>

    private companion object {
        private const val TAG = "ADAPTER_CHAT_TAG"

        private const val MSG_TYPE_LEFT = 0
        private const val MSG_TYPE_RIGHT = 1
    }

    private val firebaseAuth: FirebaseAuth


    constructor(context: Context, chatArrayList: ArrayList<ModelChat>) {
        this.context = context
        this.chatArrayList = chatArrayList

        firebaseAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChat {
        if (viewType == MSG_TYPE_RIGHT) {
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false)
            return HolderChat(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false)
            return HolderChat(view)
        }
    }

    override fun onBindViewHolder(holder: HolderChat, position: Int) {

        val modelChat = chatArrayList[position]

        val message = modelChat.message
        val messageType = modelChat.messageType
        val timestamp = modelChat.timestamp
        val formattedDate = Utils.formatTimestampDataTime(timestamp)

        holder.times.text = formattedDate

        if (messageType == Utils.MESSAGE_TYPE_TEXT) {

            holder.messageTv.visibility = View.VISIBLE
            holder.messageIv.visibility = View.GONE

            holder.messageTv.text = message

        } else {
            holder.messageTv.visibility = View.GONE
            holder.messageIv.visibility = View.VISIBLE

            try {
                Glide.with(context)
                    .load(message)
                    .placeholder(R.drawable.ic_image_gray)
                    .error(R.drawable.ic_image_broker_gray)
                    .into(holder.messageIv)

            } catch (e: Exception) {
                Log.e(TAG, "onBindViewHolder: ", e)
            }
        }


    }

    override fun getItemCount(): Int {
        return chatArrayList.size
    }

    override fun getItemViewType(position: Int): Int {

        if (chatArrayList[position].fromUid == firebaseAuth.uid) {

            return MSG_TYPE_RIGHT
        } else {

            return MSG_TYPE_LEFT
        }
        return super.getItemViewType(position)
    }


    inner class HolderChat(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var messageTv: TextView = itemView.findViewById(R.id.messageTv)
        var times: TextView = itemView.findViewById(R.id.timesTv)
        var messageIv: ShapeableImageView = itemView.findViewById(R.id.messageIv)
    }

}