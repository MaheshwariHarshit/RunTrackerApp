package com.example.runtracker.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.runtracker.R
import com.example.runtracker.other.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    lateinit var etName : TextInputEditText
    lateinit var etWeight : TextInputEditText
    lateinit var etAge : TextInputEditText
    lateinit var tvToolBarTitle : MaterialTextView
    lateinit var btnApplyChanges : MaterialButton
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etName = view.findViewById(R.id.etName)
        etWeight = view.findViewById(R.id.etWeight)
        etAge = view.findViewById(R.id.etAge)
        tvToolBarTitle = requireActivity().findViewById(R.id.tvToolbarTitle)
        btnApplyChanges = view.findViewById(R.id.btnApplyChanges)
        loadFieldsFromSharedPref()
        btnApplyChanges.setOnClickListener{
            val success = applyChangesToSharedPref()
            if(success){
                Snackbar.make(view,"Saved Changes", Snackbar.LENGTH_SHORT).show()
            }
            else{
                Snackbar.make(view,"Please fill out all the fields", Snackbar.LENGTH_SHORT).show()
            }
        }

    }

    private fun loadFieldsFromSharedPref(){
        val name = sharedPreferences.getString(Constants.KEY_NAME,"")?:""
        val weight = sharedPreferences.getFloat(Constants.KEY_WEIGHT,80f)
        val age = sharedPreferences.getInt(Constants.KEY_AGE,0)
        etName.setText(name)
        etWeight.setText(weight.toString())
        etAge.setText(age.toString())
    }

    private fun applyChangesToSharedPref() : Boolean {
        val nameText = etName.text.toString()
        val weightText = etWeight.text.toString()
        val ageText = etAge.text.toString()
        if(nameText.isEmpty() || weightText.isEmpty() || ageText.isEmpty()){
            return false
        }
        sharedPreferences.edit()
            .putString(Constants.KEY_NAME,nameText)
            .putFloat(Constants.KEY_WEIGHT,weightText.toFloat())
            .putInt(Constants.KEY_AGE,ageText.toInt())
            .apply()
        val toolBarText = "Let's go $nameText"
        tvToolBarTitle.text = toolBarText
        return true
    }
}