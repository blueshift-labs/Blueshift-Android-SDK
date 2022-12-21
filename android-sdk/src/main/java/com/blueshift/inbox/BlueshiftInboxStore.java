package com.blueshift.inbox;

import java.util.List;

public interface BlueshiftInboxStore {

    List<BlueshiftInboxMessage> getMessages();

    void addMessages(List<BlueshiftInboxMessage> messages);

    void deleteMessage(BlueshiftInboxMessage message);

    void updateMessage(BlueshiftInboxMessage message);

}
