package com.tomer.chitchat.ui.frags

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.karumi.dexter.Dexter
import com.karumi.dexter.DexterBuilder
import com.karumi.dexter.DexterBuilder.MultiPermissionListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.FragmentLoginProfileBinding
import com.tomer.chitchat.utils.Utils.Companion.getDpLink
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragUpdateProfile : Fragment() {

    private var _binding: FragmentLoginProfileBinding? = null
    private val b get() = requireNotNull(_binding)


    private lateinit var viewModel: LoginViewModel
    private var uploadBmp: Bitmap? = null
    //region ------lifecycle----->>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginProfileBinding.inflate(inflater)
        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        addObservers()
        askPermissions()
        return b.root.apply {
            b.etName.setText(viewModel.name)
            b.etName.requestFocus()
            b.btNext.setOnClickListener {
                if (b.etName.text.toString().isEmpty()) {
                    b.etName.error = "Please enter your name"
                    return@setOnClickListener
                }
                viewModel.login(b.etName.text.toString(), uploadBmp)
            }
            b.imgSelectDp.setOnClickListener {
                viewModel.showGallery(true)
            }
            Glide.with(requireActivity())
                .asBitmap()
                .load(viewModel.phone.value!!.getDpLink())
                .centerCrop()
                .circleCrop()
                .error(R.drawable.ic_add_a_photo)
                .placeholder(R.drawable.ic_add_a_photo)
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            b.imgSelectDp.setPadding(0, 0, 0, 0)
                            b.imgSelectDp.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    }
                )
        }
    }

    private fun askPermissions() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport) {
                    if (p0.isAnyPermissionPermanentlyDenied) {
                        lifecycleScope.launch { viewModel.flowToasts.emit("Please Provide Proper Permissions...") }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                }

            }).check()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion ------lifecycle----->>>

    private fun addObservers() {
        viewModel.selectedImg.observe(viewLifecycleOwner) {
            b.apply {
                this.imgSelectDp.setPadding(0, 0, 0, 0)
                Glide.with(this@FragUpdateProfile)
                    .asBitmap()
                    .load(it)
                    .override(600)
                    .into(
                        object : CustomTarget<Bitmap>() {
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
        viewModel.loginProg.observe(viewLifecycleOwner) {
            if (it) {
                b.apply {
                    b.prog.visibility = View.VISIBLE
                    b.btNext.visibility = View.GONE
                }
            } else b.apply {
                b.prog.visibility = View.GONE
                b.btNext.visibility = View.VISIBLE
            }
        }
    }
}