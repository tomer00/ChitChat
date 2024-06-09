package com.tomer.chitchat.ui.frags

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.FragmentVerifyOtpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragVerifyOtp : Fragment() {

    private var _binding: FragmentVerifyOtpBinding? = null
    private val b get() = requireNotNull(_binding)


    //region ------lifecycle----->>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentVerifyOtpBinding.inflate(inflater)
        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion ------lifecycle----->>>
}