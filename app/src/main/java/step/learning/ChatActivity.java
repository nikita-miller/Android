package step.learning;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatActivity extends AppCompatActivity {
    private String chatUrl;
    private String content;
    private LinearLayout chatContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatUrl = getString(R.string.chat_api_url);
        chatContainer = findViewById(R.id.chat_container);

        new Thread(this::loadUrl).start();
    }

    private void loadUrl() {
        try (InputStream urlStream = new URL(chatUrl).openStream()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int length;
            while ((length = urlStream.read(chunk)) > -1) {
                bytes.write(chunk, 0, length);
            }
            content = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            bytes.close();
            new Thread(this::parseContent).start();
        } catch (android.os.NetworkOnMainThreadException ignored) {
            Log.d("ChatActivity::loadUrl", "NetworkOnMainThreadException");
        } catch (MalformedURLException ex) {
            Log.d("ChatActivity::loadUrl", "MalformedURLException: " + ex.getMessage());
        } catch (IOException ex) {
            Log.d("ChatActivity::loadUrl", "IOException: " + ex.getMessage());
        }
    }

    private void showChatMessage() {
        TextView tvMessage = new TextView(this);
        tvMessage.setText(content);
        tvMessage.setPadding(10, 5, 10, 5);
        chatContainer.addView(tvMessage);
    }

    private void parseContent() {
        try {
            JSONObject object = new JSONObject(content);
            JSONArray array;
            if (object.getString("status").equals("success")) {
                array = object.getJSONArray("data");
                StringBuilder sb = new StringBuilder();
                int length = array.length();
                JSONObject item;
                for (int i = 0; i < length; ++i) {
                    item = array.getJSONObject(i);
                    sb.append(
                            String.format(
                                    "%s: %s - %s%n",
                                    item.getString("moment"),
                                    item.getString("author"),
                                    item.getString("txt")
                            )
                    );
                }
                content = sb.toString();
                runOnUiThread(this::showChatMessage);
            }
        } catch (JSONException ex) {
            Log.d(
                    "ChatActivity::parseContent",
                    "JSONException: " + ex.getMessage()
            );
        }
    }
}