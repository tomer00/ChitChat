package com.tomer.chitchat.ui.frags

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tomer.chitchat.databinding.FragmentVerifyOtpBinding
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FragVerifyOtp : Fragment() {

    private var _binding: FragmentVerifyOtpBinding? = null
    private val b get() = requireNotNull(_binding)


    private lateinit var viewModel: LoginViewModel
    //region ------lifecycle----->>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentVerifyOtpBinding.inflate(inflater)
        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        setTextMobile()
        setupOTPInputs()
        setListener()
        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion ------lifecycle----->>>

    private fun setListener() {
        b.textResendOTP.setOnClickListener {
            viewModel.setReSend(true)
        }


        b.buttonVerify.setOnClickListener { v ->
            if (b.inputCode1.getText().toString().trim().isEmpty()
                || b.inputCode2.getText().toString().trim().isEmpty()
                || b.inputCode3.getText().toString().trim().isEmpty()
                || b.inputCode4.getText().toString().trim().isEmpty()
                || b.inputCode5.getText().toString().trim().isEmpty()
                || b.inputCode6.getText().toString().trim().isEmpty()) {
                Toast.makeText(requireActivity(), "Please enter valid code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val code: String =
                b.inputCode1.getText().toString() +
                        b.inputCode2.getText().toString() +
                        b.inputCode3.getText().toString() +
                        b.inputCode4.getText().toString() +
                        b.inputCode5.getText().toString() +
                        b.inputCode6.getText().toString()
            viewModel.setOtp(code)
        }


        viewModel.codeSend.observe(viewLifecycleOwner) {
            if (it){
                b.buttonVerify.isEnabled = true
                b.textResendOTP.isEnabled = true
            }else{
                b.buttonVerify.isEnabled = true
                b.textResendOTP.isEnabled = true
            }
        }

        viewModel.loginProg.observe(viewLifecycleOwner) {
            if (it){
                b.buttonVerify.visibility = View.GONE
                b.progressBar.visibility = View.VISIBLE
            }else{
                b.buttonVerify.visibility = View.VISIBLE
                b.progressBar.visibility = View.GONE
            }
        }


    }

    /** If Intent() getStringExtra == "mobile" -> startActivity(VerifyActivity),
     * (TextView) textMobile will be received value "user mobile number" */
    private fun setTextMobile() {
        b.textMobile.text = String.format("+91-%s", viewModel.phone.value.toString())
    }

    /** When the edittext1 (b.b.inputCode1) was inserted, the cursor will be jump to the
     * next edittext (in this case it would be "b.inputCode2") */
    private fun setupOTPInputs() {
        b.inputCode1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.isNotEmpty()) {
                    b.inputCode2.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
        b.inputCode2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.isNotEmpty()) {
                    b.inputCode3.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
        b.inputCode3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.isNotEmpty()) {
                    b.inputCode4.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
        b.inputCode4.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.isNotEmpty()) {
                    b.inputCode5.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
        b.inputCode5.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.isNotEmpty()) {
                    b.inputCode6.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
    }
}