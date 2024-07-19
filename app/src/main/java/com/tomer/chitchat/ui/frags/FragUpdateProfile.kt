package com.tomer.chitchat.ui.frags

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.tomer.chitchat.databinding.FragmentLoginProfileBinding
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragUpdateProfile :Fragment() {

    private var _binding: FragmentLoginProfileBinding? = null
    private val b get() = requireNotNull(_binding)


    private lateinit var viewModel: LoginViewModel
    private var uploadBmp : Bitmap? =null
    //region ------lifecycle----->>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginProfileBinding.inflate(inflater)
        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        addObservers()
        return b.root.apply {
            b.etName.setText(viewModel.name)
            b.etName.requestFocus()
            b.btNext.setOnClickListener{
                if (b.etName.text.toString() . isEmpty()){
                    b.etName.error = "Please enter your name"
                    return@setOnClickListener
                }
                viewModel.login(b.etName.text.toString(),uploadBmp)
            }
            b.imgSelectDp.setOnClickListener{
                viewModel.showGallery()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion ------lifecycle----->>>

    private fun addObservers(){
        viewModel.selectedImg.observe(requireActivity()){
            b.apply {
                this.imgSelectDp.setPadding(0,0,0,0)
                Glide.with(this@FragUpdateProfile)
                    .asBitmap()
                    .load(it)
                    .override(600)
                    .into(
                       object :CustomTarget<Bitmap>(){
                           override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                               uploadBmp = resource
                               Glide.with(this@FragUpdateProfile)
                                   .asBitmap()
                                   .load(resource)
                                   .override(400)
                                   .centerCrop()
                                   .circleCrop()
                                   .into(b.imgSelectDp)
                           }
                           override fun onLoadCleared(placeholder: Drawable?) {
                           }
                       }
                    )
            }
        }
        viewModel.loginProg.observe(requireActivity()){
            if (it){
                b.apply {
                    b.prog.visibility = View.VISIBLE
                    b.btNext.visibility = View.GONE
                }
            }else b.apply {
                b.prog.visibility = View.GONE
                b.btNext.visibility = View.VISIBLE
            }
        }
    }
}