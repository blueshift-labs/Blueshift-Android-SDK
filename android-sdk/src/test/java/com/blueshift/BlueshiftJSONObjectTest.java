package com.blueshift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class BlueshiftJSONObjectTest {

    @Test
    public void testPutAll_WithNullInput() {
        BlueshiftJSONObject target = new BlueshiftJSONObject();
        target.putAll((JSONObject) null);
        assertEquals(0, target.length());
    }

    @Test
    public void testPutAll_WithEmptyJSONObject() throws Exception {
        BlueshiftJSONObject target = new BlueshiftJSONObject();
        target.putAll(new JSONObject("{}"));
        assertEquals(0, target.length());
    }

    @Test
    public void testPutAll_WithSimpleJSONObject() throws Exception {
        // Prepare test data
        JSONObject source = new JSONObject();
        source.put("key1", "value1");
        source.put("key2", "value2");

        // Execute
        BlueshiftJSONObject target = new BlueshiftJSONObject();
        target.putAll(source);

        // Verify
        assertEquals(2, target.length());
        assertEquals("value1", target.getString("key1"));
        assertEquals("value2", target.getString("key2"));
    }

    @Test
    public void testPutAll_WithNestedJSONObject() throws Exception {
        // Prepare nested test data
        JSONObject nested = new JSONObject();
        nested.put("nestedKey", "nestedValue");

        JSONObject source = new JSONObject();
        source.put("key1", "value1");
        source.put("nested", nested);

        // Execute
        BlueshiftJSONObject target = new BlueshiftJSONObject();
        target.putAll(source);

        // Verify
        assertEquals(2, target.length());
        assertEquals("value1", target.getString("key1"));
        JSONObject resultNested = target.getJSONObject("nested");
        assertEquals("nestedValue", resultNested.getString("nestedKey"));
    }

    @Test
    public void testPutAll_WithLargeDataSet() throws Exception {
        // Prepare large test data
        JSONObject source = new JSONObject();
        for (int i = 0; i < 1000; i++) {
            source.put("key" + i, "value" + i);
        }

        // Execute
        BlueshiftJSONObject target = new BlueshiftJSONObject();
        target.putAll(source);

        // Verify
        assertEquals(1000, target.length());
        for (int i = 0; i < 1000; i++) {
            assertEquals("value" + i, target.getString("key" + i));
        }
    }

    @Test
    public void testPutAll_WithModificationDuringIteration() throws Exception {
        // Prepare test data that will be modified during iteration
        JSONObject source = new JSONObject();
        source.put("key1", "value1");
        source.put("key2", "value2");
        source.put("trigger", "triggerValue");

        // Create a custom BlueshiftJSONObject that modifies itself during putAll
        BlueshiftJSONObject target = new BlueshiftJSONObject() {
            @Override
            public JSONObject put(String key, Object value) throws JSONException {
                JSONObject result = super.put(key, value);
                // When we see the trigger key, add a new key
                if ("trigger".equals(key)) {
                    super.put("newKey", "newValue");
                }
                return result;
            }
        };

        // Execute
        target.putAll(source);

        // Verify
        assertTrue(target.has("key1"));
        assertTrue(target.has("key2"));
        assertTrue(target.has("trigger"));
        assertTrue(target.has("newKey"));
        assertEquals("newValue", target.getString("newKey"));
    }

    @Test
    public void testPutAll_WithConcurrentModification() throws Exception {
        // Prepare test data
        JSONObject source = new JSONObject();
        for (int i = 0; i < 100; i++) {
            source.put("key" + i, "value" + i);
        }

        // Create target object
        BlueshiftJSONObject target = new BlueshiftJSONObject();

        // Use CountDownLatch to ensure all threads start at the same time
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(10);

        // Create and start 10 threads
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for signal to start
                    target.putAll(source);
                } catch (Exception e) {
                    fail("Exception occurred: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        completionLatch.await();

        // Verify results
        assertEquals("Size should be exactly 100", 100, target.length());

        // Verify all key-value pairs
        for (int i = 0; i < 100; i++) {
            String key = "key" + i;
            assertTrue("Should contain key: " + key, target.has(key));
            assertEquals("Value should match for key: " + key,
                    "value" + i, target.getString(key));
        }
    }
}