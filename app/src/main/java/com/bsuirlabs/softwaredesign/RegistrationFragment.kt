package com.bsuirlabs.softwaredesign

import android.net.Uri
import android.os.Bundle
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("email", email?.editText?.text.toString().trim())
        outState.putString("password", password?.editText?.text.toString().trim())
        outState.putString("password_confirmation", password_confirmation?.editText?.text.toString().trim())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginButton.setOnClickListener {
            findNavController().popBackStack()
        }

        if (savedInstanceState != null) {
            email.editText?.setText(savedInstanceState.getString("email"))
            password.editText?.setText(savedInstanceState.getString("password"))
            password_confirmation?.editText?.setText(savedInstanceState.getString("password_confirmation"))
        }

        registerButton.setOnClickListener {
            disableButtons()
            if (email.editText?.text.toString().trim().isNotBlank() &&
                    password.editText?.text.toString().trim().isNotBlank() &&
                    password.editText?.text.toString().trim() == password_confirmation.editText?.text.toString().trim()){
                progressBar.visibility = View.VISIBLE
                FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email.editText?.text.toString().trim(),
                                                        password.editText?.text.toString().trim())
                        .addOnCompleteListener { auth ->
                            enableButtons()
                            if (auth.isSuccessful){
                                (activity as AuthActivity).startMainActivity()
                            } else {
                                Toast.makeText(context, auth.exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
            } else {
                enableButtons()
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
