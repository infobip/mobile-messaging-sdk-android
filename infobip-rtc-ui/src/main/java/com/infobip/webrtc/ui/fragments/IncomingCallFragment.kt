package com.infobip.webrtc.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.infobip.webrtc.Injector.colors
import com.infobip.webrtc.Injector.icons
import com.infobip.webrtc.Injector.incomingCallMessageStyle
import com.infobip.webrtc.ui.CallViewModel
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.databinding.FragmentIncomingCallBinding
import com.infobip.webrtc.ui.utils.navigate

class IncomingCallFragment : Fragment() {
    private var _binding: FragmentIncomingCallBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CallViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
                val foregroundColorStateList = ColorStateList.valueOf(res.rtcUiForeground)
                logo.imageTintList = foregroundColorStateList
                name.setTextColor(foregroundColorStateList)
                decline.setIconTint(foregroundColorStateList)
                decline.setBackgroundColor(ColorStateList.valueOf(res.rtcUiHangup))
                accept.setIconTint(foregroundColorStateList)
                accept.setBackgroundColor(ColorStateList.valueOf(res.rtcUiAccept))
                background.setBackgroundColor(res.rtcUiBackground)
            }
            incomingCallMessageStyle?.let { res ->
                res.headlineText?.takeIf { it.isNotEmpty() }.let {
                    customHeadline.text = it
                    customHeadline.visibility = View.VISIBLE
                }
                res.headlineTextAppearance?.let { TextViewCompat.setTextAppearance(customHeadline, it) }
                res.headlineTextColor?.let { customHeadline.setTextColor(it) }
                res.headlineBackground?.let { customHeadline.background = AppCompatResources.getDrawable(requireContext(), it) }
                res.messageText?.takeIf { it.isNotEmpty() }.let {
                    customMessage.text = it
                    customMessage.visibility = View.VISIBLE
                }
                res.messageTextAppearance?.let { TextViewCompat.setTextAppearance(customMessage, it) }
                res.messageTextColor?.let { customMessage.setTextColor(it) }
                res.messageBackground?.let { customMessage.background = AppCompatResources.getDrawable(requireContext(), it) }
            }
        }
    }
}