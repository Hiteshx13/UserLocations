package com.printful.userlocations.ui.fragment

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.printful.userlocations.R
import com.printful.userlocations.databinding.FragmentLoginBinding
import com.printful.userlocations.utils.isNetworkConnected
import com.printful.userlocations.utils.showTast

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController: NavController = Navigation.findNavController(view)
        binding.btnProceed.setOnClickListener {

            val strEmail = binding.etEmail.text.toString().trim()
            if (strEmail.isNotEmpty() &&
                Patterns.EMAIL_ADDRESS.matcher(strEmail).matches()
            ) {
                if (isNetworkConnected(requireActivity().applicationContext)) {
                    val action =
                        LoginFragmentDirections.actionEmailFragmentToEmployeeLocationFragment(
                            binding.etEmail.text.toString()
                        )
                    navController.navigate(action)
                } else {
                    activity?.let { it1 -> showTast(it1, getString(R.string.check_your_internet)) }
                }
            } else {
                activity?.let { it1 -> showTast(it1, getString(R.string.enter_valid_email)) }
            }
        }
    }
}