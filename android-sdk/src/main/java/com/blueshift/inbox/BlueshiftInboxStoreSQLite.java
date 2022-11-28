package com.blueshift.inbox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class BlueshiftInboxStoreSQLite implements BlueshiftInboxStore {
    String json = "{\"status\":\"success\",\"content\":[{\"account_uuid\":\"<uuid>\",\"user_uuid\":\"<uuid>\",\"message_uuid\":\"<uuid>\",\"created_at\":\"2021-04-12T09:47:15.675801Z\",\"data\":{\"bsft_account_uuid\":\"e512291d-584e-4121-bfe6-fc71faaccc50\",\"bsft_experiment_uuid\":\"b54a1eb1-6cfb-34d8-825b-eab798a882ec\",\"bsft_message_uuid\":\"5b782825-ac02-495c-9d07-ad855b4b043c\",\"bsft_transaction_uuid\":\"407c3677-9693-4628-a8e9-a2427229242d\",\"bsft_user_uuid\":\"808125f8-d982-442f-9cb2-4f31bb9bc9c9\",\"inbox\":{\"title\":\"Modal\",\"details\":\"Click to view the modal in-app message\",\"icon\":\"https://picsum.photos/id/102/200/200\"},\"inapp\":{\"content\":{\"actions\":[{\"android_link\":\"http://www.blueshiftreads.com/products/biography-autobiography-personal-memoirs/eat-pray-love\",\"background_color\":\"#0012b5\",\"background_radius\":7,\"ios_link\":\"http://www.blueshiftreads.com/products/biography-autobiography-personal-memoirs/eat-pray-love\",\"margin\":{\"bottom\":5,\"left\":5,\"right\":5,\"top\":5},\"text\":\"Lets check\",\"text_color\":\"#ffffff\",\"type\":\"open\"},{\"android_link\":\"blueshift://dismiss\",\"background_color\":\"#ffffff\",\"background_radius\":7,\"ios_link\":\"blueshift://dismiss\",\"margin\":{\"bottom\":5,\"left\":5,\"right\":5,\"top\":5},\"text\":\"Dismiss Link\",\"text_color\":\"#000000\",\"type\":\"open\"}],\"message\":\"\\\"Eat Pray Love\\\", handpicked for you! Displayed on product details page!\",\"title\":\"Recommendation\"},\"content_style\":{\"actions_margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"actions_padding\":{\"bottom\":5,\"left\":5,\"right\":5,\"top\":0},\"message_color\":\"#ffffff\",\"message_gravity\":\"center\",\"message_margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"message_padding\":{\"bottom\":20,\"left\":6,\"right\":6,\"top\":20},\"message_size\":14,\"title_background_color\":null,\"title_color\":\"#ffffff\",\"title_gravity\":\"center\",\"title_margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"title_padding\":{\"bottom\":0,\"left\":5,\"right\":5,\"top\":20},\"title_size\":22},\"content_style_dark\":{\"actions_margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"actions_padding\":{\"bottom\":5,\"left\":5,\"right\":5,\"top\":0},\"message_color\":\"#ffffff\",\"message_gravity\":\"center\",\"message_margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"message_padding\":{\"bottom\":20,\"left\":6,\"right\":6,\"top\":20},\"message_size\":14,\"title_background_color\":null,\"title_color\":\"#ffffff\",\"title_gravity\":\"center\",\"title_margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"title_padding\":{\"bottom\":0,\"left\":5,\"right\":5,\"top\":20},\"title_size\":22},\"display_on_android\":\"\",\"display_on_ios\":\"\",\"expires_at\":1669355257,\"template_style\":{\"background_color\":\"#ffffff\",\"background_image\":\"https://images.randomhouse.com/cover/9780143038412\",\"background_radius\":10,\"close_button\":{\"background_color\":\"#000000\",\"code\":\"f00d\",\"show\":false,\"text\":\"\uF00D\",\"text_color\":\"#ffffff\"},\"height\":40,\"margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"width\":80},\"template_style_dark\":{\"background_color\":\"#ffffff\",\"background_image\":\"https://images.randomhouse.com/cover/9780143038412\",\"background_radius\":10,\"close_button\":{\"background_color\":\"#000000\",\"code\":\"f00d\",\"show\":false,\"text\":\"\uF00D\",\"text_color\":\"#ffffff\"},\"height\":40,\"margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"width\":80},\"trigger\":\"now\",\"type\":\"modal\"},\"timestamp\":\"2022-11-24T05:47:37.721806Z\"}},{\"account_uuid\":\"<uuid>\",\"user_uuid\":\"<uuid>\",\"message_uuid\":\"<uuid>\",\"created_at\":\"2021-03-12T09:47:15.675801Z\",\"data\":{\"account_adapter_uuid\":\"c2102111-e660-47df-aa3e-678d71a93644\",\"bsft_account_uuid\":\"e512291d-584e-4121-bfe6-fc71faaccc50\",\"bsft_experiment_uuid\":\"86e7d8e9-1e36-4a51-b83e-c7f1d5ceaf03\",\"bsft_message_uuid\":\"f73bc575-01ce-4c28-9260-c72cd00db03d\",\"bsft_transaction_uuid\":\"8e8d9de0-075d-4b8f-b1bf-30d99daf44bb\",\"bsft_user_uuid\":\"808125f8-d982-442f-9cb2-4f31bb9bc9c9\",\"inbox\":{\"title\":\"Slide-in\",\"details\":\"Click to view the slide in in-app message\",\"icon\":\"https://picsum.photos/id/101/200/200\"},\"inapp\":{\"content\":{\"actions\":[{\"android_link\":\"http://www.blueshiftreads.com/products/drama-american-general/death-of-a-salesman\",\"background_color\":\"#4caf50\",\"ios_link\":\"http://www.blueshiftreads.com/products/drama-american-general/death-of-a-salesman\",\"text\":\"\",\"text_color\":\"#fafafa\",\"type\":\"open\"}],\"icon\":\"\uF0A1\",\"icon_image\":\"\",\"message\":\"Checkout \\\"Death of a Salesman\\\"\"},\"content_style\":{\"icon_background_color\":\"#1962a9\",\"icon_background_radius\":0,\"icon_color\":\"#ffffff\",\"icon_image_background_color\":\"\",\"icon_image_background_radius\":0,\"icon_image_padding\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"icon_padding\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"icon_size\":22,\"message_color\":\"#ffffff\",\"message_gravity\":\"start\",\"message_padding\":{\"bottom\":10,\"left\":10,\"right\":10,\"top\":10},\"message_size\":14},\"content_style_dark\":{\"icon_background_color\":\"#1962a9\",\"icon_background_radius\":0,\"icon_color\":\"#ffffff\",\"icon_image_background_color\":\"\",\"icon_image_background_radius\":0,\"icon_image_padding\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"icon_padding\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"icon_size\":22,\"message_color\":\"#ffffff\",\"message_gravity\":\"start\",\"message_padding\":{\"bottom\":10,\"left\":10,\"right\":10,\"top\":10},\"message_size\":14},\"display_on_android\":\"\",\"display_on_ios\":\"\",\"expires_at\":1671860814,\"template_style\":{\"background_color\":\"#3671ac\",\"background_radius\":0,\"enable_background_action\":false,\"height\":-2,\"margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"position\":\"bottom\",\"width\":100},\"template_style_dark\":{\"background_color\":\"#3671ac\",\"background_radius\":0,\"enable_background_action\":false,\"height\":-2,\"margin\":{\"bottom\":0,\"left\":0,\"right\":0,\"top\":0},\"position\":\"bottom\",\"width\":100},\"trigger\":\"now\",\"type\":\"slide_in_banner\"},\"timestamp\":\"2022-11-24T05:46:54.943009Z\"}}]}";

    @Override
    public List<BlueshiftInboxMessage> getMessages() {
        JSONArray jsonArray;
        try {
            JSONObject jsonObject = new JSONObject(json);
            jsonArray = jsonObject.getJSONArray("content");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return BlueshiftInboxMessage.fromJsonArray(jsonArray);
    }

    @Override
    public void addMessage(BlueshiftInboxMessage message) {

    }

    @Override
    public void removeMessage(BlueshiftInboxMessage message) {

    }

    @Override
    public void updateMessage(BlueshiftInboxMessage message) {

    }
}
