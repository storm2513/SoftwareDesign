package com.bsuirlabs.softwaredesign

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_profile_edit.*
import java.io.ByteArrayOutputStream
import java.util.*


class ProfileEditFragment : Fragment() {
    private var photoChanged = false
    private var userProfile : UserProfile? = null
    var dataChanged = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile_edit, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("firstname", profileFirstName?.editText?.text.toString())
        outState.putString("lastname", profileLastName?.editText?.text.toString())
        outState.putString("phone", profilePhone?.editText?.text.toString())
        outState.putBoolean("photoChanged", photoChanged)
        if (profileImage != null)
            outState.putByteArray("image", imageViewToByteArray(profileImage))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textChangeListener = object:TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setHasChanges(true)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        }

        setDataFromBundle(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        photoChanged = false
        if (currentUser != null) {
            progressBar.visibility = View.VISIBLE
            val userProfileListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    userProfile = dataSnapshot.getValue(UserProfile::class.java)
                    if (userProfile != null) {
                        if (savedInstanceState != null) {
                            setDataFromBundle(savedInstanceState)
                        } else {
                            profileFirstName?.editText?.setText(userProfile?.firstName)
                            profileLastName?.editText?.setText(userProfile?.lastName)
                            profilePhone?.editText?.setText(userProfile?.phone)
                            if (!userProfile?.image.toString().isBlank()) {
                                val imageReference = FirebaseStorage.getInstance()
                                        .getReference(userProfile?.image.toString())
                                if (profileImage != null) {
                                    GlideApp.with(this@ProfileEditFragment)
                                            .load(imageReference)
                                            .into(profileImage)
                                }
                            }
                        }
                    }
                    showInputs()
                    profileFirstName?.editText?.addTextChangedListener(textChangeListener)
                    profileLastName?.editText?.addTextChangedListener(textChangeListener)
                    profilePhone?.editText?.addTextChangedListener(textChangeListener)
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(ContentValues.TAG, "loadPost:onCancelled", databaseError.toException())
                }
            }
            FirebaseDatabase.getInstance().reference.child(currentUser.uid).addValueEventListener(userProfileListener)
        }

        profileEditImageButton.setOnClickListener {
            val pictureDialog = context?.let { context -> AlertDialog.Builder(context) }
            val pictureDialogItems = arrayOf(getString(R.string.select_photo_from_gallery), getString(R.string.capture_photo))
            pictureDialog?.setItems(pictureDialogItems
            ) { _, which ->
                when (which) {
                    0 -> getPhotoFromGallery()
                    1 -> getPhotoFromCamera()
                }
            }
            pictureDialog?.show()
        }

        cancelButton.setOnClickListener {
            setHasChanges(false)
            findNavController().popBackStack()
        }

        saveButton.setOnClickListener {
            disableButtons()
            setHasChanges(false)
            hideInputs()
            val firstName = profileFirstName.editText?.text.toString().trim()
            val lastName = profileLastName.editText?.text.toString().trim()
            val phone = profilePhone.editText?.text.toString().trim()
            if (userProfile == null) {
                userProfile = UserProfile(firstName = firstName, lastName = lastName, phone = phone)
            } else {
                userProfile?.firstName = firstName
                userProfile?.lastName = lastName
                userProfile?.phone = phone
            }

            if (photoChanged) {
                val storageReference = FirebaseStorage.getInstance().reference
                val imageBytes = imageViewToByteArray(profileImage)
                if (imageBytes != null) {
                    val imageReference = storageReference
                        .child(currentUser?.uid.toString())
                        .child("profilePhotos/${UUID.nameUUIDFromBytes(
                                imageBytes)}"
                        )
                    imageReference.putBytes(imageBytes).addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            userProfile?.image = imageReference.path
                            updateProfile(currentUser, userProfile)
                        } else {
                            showInputs()
                        }
                    }
                }
            }
            else {
                updateProfile(currentUser, userProfile)
            }
            enableButtons()
        }
    }

    private fun updateProfile(user : FirebaseUser?, userProfile : UserProfile?) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        if (user != null) {
            databaseReference.child(user.uid).setValue(userProfile).addOnCompleteListener {
                if (it.isSuccessful)
                    findNavController().popBackStack()
                else
                    showInputs()
            }
        }
    }

    private fun imageViewToByteArray(imageView : ImageView) : ByteArray? {
        if (imageView.drawable is BitmapDrawable) {
            val bitmap = (imageView.drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            return stream.toByteArray()
        }
        return null
    }

    private val CameraRequest = 1
    private val GalleryRequest = 2
    private val PermissionRequestCamera = 3

    private fun getPhotoFromGallery(){
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        this.startActivityForResult(galleryIntent, GalleryRequest)
    }

    private fun getPhotoFromCamera(){
        if (context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.CAMERA) } != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            openCamera()
        }
    }

    private fun requestCameraPermission(){
        requestPermissions(arrayOf(Manifest.permission.CAMERA), PermissionRequestCamera)
    }

    private fun setHasChanges(bool: Boolean){
        dataChanged = bool
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PermissionRequestCamera -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openCamera()
                } else {
                   super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK)
            return
        when (requestCode) {
            CameraRequest -> {
                val extras = data?.extras
                val imageBitmap = extras?.get("data") as Bitmap
                photoChanged = true
                profileImage.setImageBitmap(imageBitmap)
                setHasChanges(true)
            }
            GalleryRequest -> {
                val selectedImage = data?.data
                photoChanged = true
                profileImage.setImageURI(selectedImage)
                setHasChanges(true)
            }
        }
    }

    private fun openCamera(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        this.startActivityForResult(takePictureIntent, CameraRequest)
    }

    private fun showInputs(){
        profileFirstName?.visibility = View.VISIBLE
        profileLastName?.visibility = View.VISIBLE
        profilePhone?.visibility = View.VISIBLE
        progressBar?.visibility = View.INVISIBLE
    }

    private fun hideInputs(){
        profileFirstName?.visibility = View.INVISIBLE
        profileLastName?.visibility = View.INVISIBLE
        profilePhone?.visibility = View.INVISIBLE
        progressBar?.visibility = View.VISIBLE
    }

    private fun setDataFromBundle(savedInstanceState: Bundle?){
        if (savedInstanceState != null) {
            profileFirstName?.editText?.setText(savedInstanceState.getString("firstname"))
            profileLastName?.editText?.setText(savedInstanceState.getString("lastname"))
            profilePhone?.editText?.setText(savedInstanceState.getString("phone"))
            photoChanged = savedInstanceState.getBoolean("photoChanged")
            val byteArray = savedInstanceState.getByteArray("image")
            if (byteArray != null)
                profileImage?.setImageBitmap(byteArray.size.let { BitmapFactory.decodeByteArray(byteArray, 0, it) })
        }
    }

    private fun disableButtons() {
        saveButton?.isEnabled = false
        cancelButton?.isEnabled = false
    }

    private fun enableButtons() {
        saveButton?.isEnabled = true
        cancelButton?.isEnabled = true
    }


    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }
}
