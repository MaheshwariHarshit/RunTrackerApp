package com.example.runtracker.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.runtracker.R
import com.example.runtracker.other.Constants.KEY_AGE
import com.example.runtracker.other.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.runtracker.other.Constants.KEY_NAME
import com.example.runtracker.other.Constants.KEY_WEIGHT
import com.example.runtracker.other.SortType
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    @Inject
    lateinit var sharedPref : SharedPreferences
    @set: Inject
    var isFirstAppOpen = true

    lateinit var  tvContinue : TextView
    lateinit var etName : EditText
    lateinit var  etWeight : EditText
    lateinit var etAge : TextInputEditText
    var  tvToolbarTitle : MaterialTextView? = null
    val arr = Array(100,{i->i+10})
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etName = view.findViewById(R.id.etName)
        etWeight = view.findViewById(R.id.etWeight)
        etAge = view.findViewById(R.id.etAge)
        //////////////////////////////////////////

        /////////////////////////////////////
        tvToolbarTitle = requireActivity()?.findViewById(R.id.tvToolbarTitle)
            tvContinue = view.findViewById(R.id.tvContinue)
        if(!isFirstAppOpen){
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment,true)
                .build()
            findNavController().navigate(
                R.id.action_setupFragment_to_runFragment,
                savedInstanceState,
                navOptions
            )
        }

        tvContinue.setOnClickListener {
            val success = writePersonalDataToSharedPref()
            if (!success) {
                Snackbar.make(requireView(),"Please enter all the fields",Snackbar.LENGTH_SHORT).show()
            } else {
                findNavController().navigate(R.id.action_setupFragment_to_runFragment)
            }
        }
    }

    private fun writePersonalDataToSharedPref() : Boolean {
        val name = etName.text.toString()
        val weight = etWeight.text.toString()
        val age = etAge.text.toString()
        if(name.isEmpty() || weight.isEmpty()|| age.isEmpty()){
            return false
        }
        sharedPref.edit()
            .putString(KEY_NAME,name)
            .putFloat(KEY_WEIGHT,weight.toFloat())
            .putInt(KEY_AGE,age.toInt())
            .putBoolean(KEY_FIRST_TIME_TOGGLE,false)
            .apply()
        val toolbarText = "Let's Go, ${name}!"
        tvToolbarTitle?.text = toolbarText
        return true
    }
}