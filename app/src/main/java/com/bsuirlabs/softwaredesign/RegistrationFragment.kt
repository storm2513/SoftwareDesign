package com.bsuirlabs.softwaredesign

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_registration.*


class RegistrationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }
        registerButton.setOnClickListener {
            disableButtons()
            if (email.editText?.text.toString().trim().isNotBlank() &&
                    password.editText?.text.toString().trim() == password_confirmation.editText?.text.toString().trim()){
                progressBar.visibility = View.VISIBLE
                FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email.editText?.text.toString().trim(),
                                                        password.editText?.text.toString().trim())
                        .addOnCompleteListener {
                            enableButtons()
                            if (it.isSuccessful){
                                findNavController().navigate(R.id.action_registrationFragment_to_profileEditFragment)
                            } else {
                                Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
            } else {
                enableButtons()
                Log.d("ERROR EMAIL", email.editText?.text.toString().trim())
                Log.d("ERROR PASSWORD", password.editText?.text.toString().trim())
                Log.d("ERROR PASSWORD", password_confirmation.editText?.text.toString().trim())
                Toast.makeText(context, getString(R.string.registration_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableButtons() {
        registerButton.isEnabled = false
        loginButton.isEnabled = false
    }

    private fun enableButtons() {
        registerButton.isEnabled = true
        loginButton.isEnabled = true
        progressBar.visibility = View.INVISIBLE
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }
}
