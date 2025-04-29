package com.infobip.webrtc.ui.internal.ui.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.databinding.FragmentIncomingCallBinding
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.model.CallAction
import com.infobip.webrtc.ui.internal.service.ActiveCallService
import com.infobip.webrtc.ui.internal.ui.CallViewModel
import com.infobip.webrtc.ui.internal.utils.navigate
import com.infobip.webrtc.ui.internal.utils.show

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
            name.text = (Injector.cache.incomingCallScreenStyle?.callerName ?: viewModel.peerName)
            accept.setOnClickListener {
                viewModel.accept()
                ActiveCallService.start(requireContext(), CallAction.INCOMING_CALL_ACCEPTED)
                parentFragmentManager.navigate(InCallFragment(), R.id.navHost)
            }
            decline.setOnClickListener {
                viewModel.decline()
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
            Injector.cache.icons?.let { res ->
                logo.setImageResource(res.caller)
                accept.setIcon(res.accept)
                decline.setIcon(res.decline)
            }
            Injector.cache.colors?.let { res ->
                val foregroundColorStateList = ColorStateList.valueOf(res.foreground)
                logo.imageTintList = foregroundColorStateList
                name.setTextColor(foregroundColorStateList)
                decline.setIconTint(foregroundColorStateList)
                decline.setBackgroundColor(ColorStateList.valueOf(res.hangup))
                accept.setIconTint(foregroundColorStateList)
                accept.setBackgroundColor(ColorStateList.valueOf(res.accept))
                background.setBackgroundColor(res.background)
            }
            Injector.cache.incomingCallScreenStyle?.let { res ->
                logo.show(res.callerIconVisible)
                res.headlineText?.takeIf { it.isNotEmpty() }.let {
                    customHeadline.text = it
                    customHeadline.visibility = View.VISIBLE
                }
                res.headlineTextAppearance?.let { TextViewCompat.setTextAppearance(customHeadline, it) }
                customHeadline.setTextColor(res.headlineTextColor)
                res.headlineBackground?.let { customHeadline.background = AppCompatResources.getDrawable(requireContext(), it) }
                res.messageText?.takeIf { it.isNotEmpty() }.let {
                    customMessage.text = it
                    customMessage.visibility = View.VISIBLE
                }
                res.messageTextAppearance?.let { TextViewCompat.setTextAppearance(customMessage, it) }
                customMessage.setTextColor(res.messageTextColor)
                res.messageBackground?.let { customMessage.background = AppCompatResources.getDrawable(requireContext(), it) }
            }
        }
    }
}