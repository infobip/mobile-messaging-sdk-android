(function () {
    var DOCUMENT_WAS_LOADED = 'documentWasLoaded';
    var HEIGHT_CHANGED = 'heightChanged';

    // Register click handlers to all clickable elements
    const clickableElements = document.querySelectorAll('[data-action]');
    for (let i = 0; i < clickableElements.length; i++) {
        clickableElements[i].addEventListener('click', function ({ currentTarget }) {
            sendMessageToWebView(
              currentTarget.getAttribute('data-action'),
              currentTarget.getAttribute('data-action-value'),
            );
        });
    }

    window.InfobipMobileMessaging = Object.freeze({
        /**
         * Ensures that once document becomes fully loaded, appropriate message will be sent to the web view. If
         * document is already fully loaded, the message will be sent immediately.
         * @returns {DocumentReadyState} - The current value of document's `readyState`.
         */
        registerMessageSendingOnDocumentLoad() {
            if (document.readyState === 'complete') {
                sendMessageToWebView(DOCUMENT_WAS_LOADED);
            } else {
                window.onload = function () {
                    sendMessageToWebView(DOCUMENT_WAS_LOADED);
                };
            }
            return document.readyState;
        },
        readBodyHeight() {
        sendMessageToWebView(HEIGHT_CHANGED, document.body.scrollHeight);
            return document.body.scrollHeight;
        },
    });

//    var height = undefined;
//    new ResizeObserver(function() {
//        const newHeight = document.body.scrollHeight;
//        if (newHeight !== height) {
//            height = newHeight;
//            sendMessageToWebView(HEIGHT_CHANGED, height);
//        }
//    }).observe(document.body);
//
//    function getHeight() {
//        sendMessageToWebView(HEIGHT_CHANGED, document.body.scrollHeight);
//        //return document.body.scrollHeight;
//    }

    /** Sends the message with payload to the webView's message handler. */
    function sendMessageToWebView(messageName, payload) {
        const isIOS = window.webkit !== undefined;
        const isAndroid = window.InAppWebViewInterface !== undefined;

        if (isIOS) {
            window.webkit.messageHandlers[messageName].postMessage(payload);
        } else if (isAndroid) {
            window.InAppWebViewInterface[messageName](payload);
        } else {
            console.log(messageName, payload);
        }
    }
})();