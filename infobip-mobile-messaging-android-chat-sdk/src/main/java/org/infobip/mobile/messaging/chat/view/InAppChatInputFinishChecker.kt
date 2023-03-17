package org.infobip.mobile.messaging.chat.view

class InAppChatInputFinishChecker(private val sendInputDraft: (String) -> Unit) : Runnable {

    private var inputValue: String? = null

    override fun run() {
        inputValue?.let(sendInputDraft)
    }

    fun setInputValue(inputValue: String?) {
        this.inputValue = inputValue
    }

}
