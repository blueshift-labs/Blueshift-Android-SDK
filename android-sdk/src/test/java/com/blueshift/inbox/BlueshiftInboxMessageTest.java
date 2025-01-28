package com.blueshift.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

public class BlueshiftInboxMessageTest {

    @Test
    public void testConstructor_validJson() throws JSONException {
        JSONObject jsonObject = new JSONObject("""
                    {
                      "created_at": 1678886400,
                      "account_uuid": "account_id",
                      "user_uuid": "user_id",
                      "message_uuid": "message_id",
                      "campaign_attr": {},
                      "status": "read",
                      "data": {
                        "inapp": {
                          "trigger": "now",
                          "display_on_android": "activity",
                          "scope": "inbox+inapp",
                          "type": "modal",
                          "expires_at": 1678972800
                        }
                      }
                    }
                """);

        BlueshiftInboxMessage message = new BlueshiftInboxMessage(jsonObject);

        assertEquals(new Date(1678886400000L), message.createdAt);
        assertEquals("account_id", message.accountId);
        assertEquals("user_id", message.userId);
        assertEquals("message_id", message.messageId);
        assertEquals(new JSONObject().toString(), message.campaignAttr.toString());
        assertEquals(BlueshiftInboxMessage.Status.READ, message.status);
        assertEquals("now", message.trigger);
        assertEquals("activity", message.displayOn);
        assertEquals(BlueshiftInboxMessage.Scope.INBOX_AND_INAPP, message.scope);
        assertEquals("modal", message.messageType);
        assertEquals(new Date(1678972800000L), message.expiresAt);
    }

    @Test
    public void testConstructor_validJsonWithExtraKeys() throws JSONException {
        JSONObject jsonObject = new JSONObject("""
                    {
                      "created_at": 1678886400,
                      "account_uuid": "account_id",
                      "user_uuid": "user_id",
                      "message_uuid": "message_id",
                      "campaign_attr": {},
                      "status": "read",
                      "new_key" : "new_value",
                      "data": {
                      "new_key" : "new_value",
                        "inapp": {
                          "new_key" : "new_value",
                          "trigger": "now",
                          "display_on_android": "activity",
                          "scope": "inbox+inapp",
                          "type": "modal",
                          "expires_at": 1678972800
                        }
                      }
                    }
                """);

        BlueshiftInboxMessage message = new BlueshiftInboxMessage(jsonObject);

        assertEquals(new Date(1678886400000L), message.createdAt);
        assertEquals("account_id", message.accountId);
        assertEquals("user_id", message.userId);
        assertEquals("message_id", message.messageId);
        assertEquals(new JSONObject().toString(), message.campaignAttr.toString());
        assertEquals(BlueshiftInboxMessage.Status.READ, message.status);
        assertEquals("now", message.trigger);
        assertEquals("activity", message.displayOn);
        assertEquals(BlueshiftInboxMessage.Scope.INBOX_AND_INAPP, message.scope);
        assertEquals("modal", message.messageType);
        assertEquals(new Date(1678972800000L), message.expiresAt);
    }

    @Test
    public void testConstructor_missingKeys() throws JSONException {
        JSONObject jsonObject = new JSONObject("{}");

        BlueshiftInboxMessage message = new BlueshiftInboxMessage(jsonObject);

        assertEquals(new Date(0), message.createdAt);
        assertEquals("", message.accountId);
        assertEquals("", message.userId);
        assertEquals("", message.messageId);
        assertNull(message.campaignAttr);
        assertEquals(BlueshiftInboxMessage.Status.UNKNOWN, message.status);

        // since the JSON is empty and has no 'data' object present, following will be null.
        assertNull(message.trigger);
        assertNull(message.displayOn);
        assertNull(message.scope);
        assertNull(message.messageType);
        assertNull(message.expiresAt);
    }
}