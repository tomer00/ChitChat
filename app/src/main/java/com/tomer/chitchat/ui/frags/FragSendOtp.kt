package com.tomer.chitchat.ui.frags

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.FragmentSendOtpBinding
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FragSendOtp : Fragment() {

    private var _binding: FragmentSendOtpBinding? = null
    private val b get() = requireNotNull(_binding)

    private lateinit var viewModel: LoginViewModel

    //region ------lifecycle----->>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSendOtpBinding.inflate(inflater)
        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        init()
        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion ------lifecycle----->>>
    
    private fun init(){
        b.buttonGetOTP.setOnClickListener {
            if (b.inputMobile.text.length!=10) Toast.makeText(requireActivity(),
                "Please enter Valid Phone NO...", Toast.LENGTH_SHORT).show()
            else viewModel.setPhone(b.inputMobile.text.toString())
        }
    }
        
}