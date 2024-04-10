package com.okada.android.Utils

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.okada.android.Common
import com.okada.android.data.model.TokenModel

object UserUtils {
    fun UpdateUser(view: View, updateData:Map<String,Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener {e->
                e.message?.let { Snackbar.make(view, it, Snackbar.LENGTH_LONG).show() }
            }.addOnSuccessListener {
                Snackbar.make(view, "Update image worked", Snackbar.LENGTH_LONG).show()
            }
    }

}