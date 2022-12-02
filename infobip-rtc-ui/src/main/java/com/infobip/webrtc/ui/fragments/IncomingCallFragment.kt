package com.infobip.webrtc.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.infobip.webrtc.Injector.colors
import com.infobip.webrtc.Injector.icons
import com.infobip.webrtc.ui.CallViewModel
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.databinding.FragmentIncomingCallBinding
import com.infobip.webrtc.ui.navigate

class IncomingCallFragment : Fragment() {
    private var _binding: FragmentIncomingCallBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CallViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentIncomingCallBinding.inflate(inflater, container, false).also { _binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        customize()
        with(binding) {
            name.text = viewModel.peerName
            accept.setOnClickListener {
                viewModel.accept()
                parentFragmentManager.navigate(InCallFragment(), R.id.navHost)
            }
            decline.setOnClickListener {
                viewModel.decline()
                viewModel.updateState { copy(isFinished = true) }
            }
        }
    }

    override fun onDestroyView() {
        binding.accept.setOnClickListener(null)
        binding.decline.setOnClickListener(null)
        _binding = null
        super.onDestroyView()
    }

    private fun customize() {
        with(binding) {
            icons?.let { res ->
                logo.setImageResource(res.callsIcon)
                accept.setIcon(res.accept)
                decline.setIcon(res.decline)
            }
            colors?.let { res ->
                val foregroundColorStateList = ColorStateList.valueOf(res.rtc_ui_foreground)
                logo.imageTintList = foregroundColorStateList
                name.setTextColor(foregroundColorStateList)
                decline.setIconTint(foregroundColorStateList)
                decline.setCircleColor(ColorStateList.valueOf(res.rtc_ui_hangup))
                accept.setIconTint(foregroundColorStateList)
                accept.setCircleColor(ColorStateList.valueOf(res.rtc_ui_accept))
                background.setBackgroundColor(res.rtc_ui_background)
            }
        }
    }
}