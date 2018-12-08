package com.bsuirlabs.softwaredesign

import android.content.ContentValues.TAG
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        profileEmail.text = currentUser!!.email
        progressBar.visibility = View.VISIBLE
        val userProfileListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userProfile = dataSnapshot.getValue(UserProfile::class.java)
                if (userProfile != null) {
                    profileFirstName?.text = userProfile.firstName
                    profileLastName?.text = userProfile.lastName
                    val fullName = """${userProfile.firstName} ${userProfile.lastName}"""
                    (activity as MainActivity).getNameTextViewFromNavView().text = fullName
                    profilePhone?.text = userProfile.phone
                    if (!userProfile.image.isNullOrBlank()) {
                        val imageReference = FirebaseStorage.getInstance()
                                .getReference(userProfile.image!!)
                        if (profileImage != null) {
                            GlideApp.with(this@ProfileFragment)
                                    .load(imageReference)
                                    .into(profileImage)
                            GlideApp.with(this@ProfileFragment)
                                    .load(imageReference)
                                    .into((activity as MainActivity).getProfileImageViewFromNavView())
                        }
                    }
                }
                progressBar?.visibility = View.INVISIBLE
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        FirebaseDatabase.getInstance().reference.child(currentUser.uid).addValueEventListener(userProfileListener)


        profileEditButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_profileEditFragment)
        }
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            (activity as MainActivity).startAuthActivity()
        }
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }
}
