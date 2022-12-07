package com.blueshift.inbox;

import java.util.List;

public interface BlueshiftInboxStore {

    List<BlueshiftInboxMessage> getMessages();

    void addMessages(List<BlueshiftInboxMessage> messages);

    void removeMessage(BlueshiftInboxMessage message);

    void updateMessage(BlueshiftInboxMessage message);

}
