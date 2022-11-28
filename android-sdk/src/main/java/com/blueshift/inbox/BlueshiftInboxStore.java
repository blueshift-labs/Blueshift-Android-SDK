package com.blueshift.inbox;

import java.util.List;

public interface BlueshiftInboxStore {

    List<BlueshiftInboxMessage> getMessages();

    void addMessage(BlueshiftInboxMessage message);

    void removeMessage(BlueshiftInboxMessage message);

    void updateMessage(BlueshiftInboxMessage message);

}
