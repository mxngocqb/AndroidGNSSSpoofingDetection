package com.example.pseudorange;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NavMessageProvider {

    private static final String BASE_URL = "https://ace-rationally-flounder.ngrok-free.app/";

    public String fetchData(String endpoint)  {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String result = null;

        try {
            URL url = new URL(BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Read the response
            InputStream inputStream = connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                return null;
            }
            result = buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public String extractNavigationMessage(String navMessageData) {
        try {
            JSONObject jsonObject = new JSONObject(navMessageData);
            return jsonObject.getString("navigationMessage");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extractSignature(String navMessageData) {
        try {
            JSONObject jsonObject = new JSONObject(navMessageData);
            return jsonObject.getString("signature");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
