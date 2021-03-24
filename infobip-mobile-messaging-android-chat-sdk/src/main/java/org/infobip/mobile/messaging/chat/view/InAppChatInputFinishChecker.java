package org.infobip.mobile.messaging.chat.view;

import org.infobip.mobile.messaging.chat.core.InAppChatClient;

public class InAppChatInputFinishChecker implements Runnable {

    private final InAppChatClient chatClient;
    private String inputValue;

    public InAppChatInputFinishChecker(InAppChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        chatClient.sendInputDraft(inputValue);
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }
}
