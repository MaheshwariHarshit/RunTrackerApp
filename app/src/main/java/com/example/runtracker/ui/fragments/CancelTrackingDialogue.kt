package com.example.runtracker.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentOnAttachListener
import com.example.runtracker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CancelTrackingDialogue : DialogFragment() {

    private var yesListener : (()-> Unit) ? = null

    fun setYesListener(listener: () -> Unit){
        yesListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel Run?")
            .setMessage("Cancelling the Run will delete the data. Continue?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _, _ ->
//                stopRun()
                yesListener?.let {
                    yes->
                    yes()
                }
            }
            .setNegativeButton("No"){
                    dialogInterface,_->
                dialogInterface.cancel()
            }
            .create()
    }
}