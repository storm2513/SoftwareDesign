package com.bsuirlabs.softwaredesign


import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_change_rss_source.*


class ChangeRssSourceFragment : Fragment() {
    private var userProfile : UserProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_change_rss_source, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userProfileListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userProfile = dataSnapshot.getValue(UserProfile::class.java)
                if (userProfile != null) {
                    if (!userProfile!!.rssSource.isNullOrBlank()) {
                        rssSourceTextView?.editText?.setText(userProfile!!.rssSource)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(ContentValues.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        saveButton.setOnClickListener {
            val rssUrl = rssSourceTextView.editText!!.text.toString()
            if(userProfile == null) {
                userProfile = UserProfile(rssSource = rssUrl)
            } else {
                userProfile!!.rssSource = rssUrl
            }
            updateProfile(currentUser, userProfile)
            (activity as MainActivity).cleanArticlesCache()
        }
        FirebaseDatabase.getInstance().reference.child(currentUser!!.uid).addValueEventListener(userProfileListener)
    }

    private fun updateProfile(user : FirebaseUser?, userProfile : UserProfile?) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        if (user != null) {
            databaseReference.child(user.uid).setValue(userProfile).addOnCompleteListener {
                if (it.isSuccessful)
                    findNavController().popBackStack()
            }
        }
    }
}
