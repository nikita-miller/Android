package step.learning;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    private String chatUrlString;
    private String content;
    private LinearLayout chatContainer;
    private List<ChatMessage> chatMessages;
    private EditText etUserName;
    private EditText etUserMessage;
    private ChatMessage userMessage;

    private static final SimpleDateFormat scanFormat =
            new SimpleDateFormat(
                    "MMM d, yyyy h:mm:ss a",
                    Locale.US
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        new Thread(this::loadUrl).start();

        chatUrlString = getString(R.string.chat_api_url);

        chatContainer = findViewById(R.id.chat_container);
        etUserName = findViewById(R.id.et_user_name);
        etUserMessage = findViewById(R.id.et_user_message);

        findViewById(R.id.btn_chat_send)
                .setOnClickListener(this::sendButtonClick);
    }

    private void sendButtonClick(View v) {
        String author = etUserName.getText().toString();
        if (author.isEmpty()) {
            Toast.makeText(
                    this,
                    "Enter author's name",
                    Toast.LENGTH_SHORT
            ).show();
            etUserName.requestFocus();
            return;
        }
        String messageTxt = etUserMessage.getText().toString();
        this.userMessage = new ChatMessage();
        this.userMessage.setAuthor(author);
        this.userMessage.setTxt(messageTxt);
        new Thread(this::postUserMessage).start();
    }

    private void postUserMessage() {
        try {
            URL chatUrl = new URL(chatUrlString);
            HttpURLConnection connection = (HttpURLConnection) chatUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "*/*");
            connection.setChunkedStreamingMode(0);

            OutputStream body = connection.getOutputStream();
            body.write(userMessage.toJsonString().getBytes());
            body.flush();
            body.close();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                Log.d(
                        "ChatActivity::postUserMessage",
                        "Request failed with code " + responseCode
                );
                return;
            }
            InputStream response = connection.getInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int length;
            while ((length = response.read(chunk)) > -1) {
                bytes.write(chunk, 0, length);
            }
            String responseBody = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            Log.i("ChatActivity::postUserMessage", responseBody);

            bytes.close();
            response.close();
            connection.disconnect();

            new Thread(this::loadUrl).start();
        } catch (Exception ex) {
            Log.d("ChatActivity::postUserMessage", ex.getMessage());
        }
    }

    private void loadUrl() {
        try (InputStream urlStream = new URL(chatUrlString).openStream()) {
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

    private void parseContent() {
        try {
            JSONObject object = new JSONObject(content);
            if ("success".equals(object.getString("status"))) {
                JSONArray array = object.getJSONArray("data");
                chatMessages = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                int length = array.length();
                for (int i = 0; i < length; ++i) {
                    chatMessages.add(
                            new ChatMessage(
                                    array.getJSONObject(i)
                            )
                    );
                }
                Collections.reverse(chatMessages);
                runOnUiThread(this::showChatMessages);
            }
        } catch (JSONException ex) {
            Log.d(
                    "ChatActivity::parseContent",
                    "JSONException: " + ex.getMessage()
            );
        }
    }

    private void showChatMessages() {
        Drawable otherBg = AppCompatResources.getDrawable(
                getApplicationContext(),
                R.drawable.rates_left_shape
        );
        Drawable myBg = AppCompatResources.getDrawable(
                getApplicationContext(),
                R.drawable.rates_right_shape
        );

        LinearLayout.LayoutParams otherLayoutParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        otherLayoutParams.setMargins(10, 7, 10, 7);

        LinearLayout.LayoutParams myLayoutParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        myLayoutParams.setMargins(10, 7, 10, 7);
        myLayoutParams.gravity = Gravity.END;

        TextView tvMessage;
        boolean isMyMessage;
        String author;
        for (ChatMessage chatMessage : chatMessages) {
            tvMessage = new TextView(this);
            author = chatMessage.getAuthor();
            isMyMessage = author.equals(etUserName.getText().toString());
            tvMessage.setText(String.format(
                    "%s:%n%s - %s",
                    scanFormat.format(chatMessage.getMoment()),
                    author,
                    chatMessage.getTxt())
            );
            tvMessage.setBackground(
                    isMyMessage
                            ? myBg
                            : otherBg
            );
            tvMessage.setLayoutParams(
                    isMyMessage
                            ? myLayoutParams
                            : otherLayoutParams
            );
            tvMessage.setTextSize(16);
            tvMessage.setPadding(10, 5, 10, 5);
            chatContainer.addView(tvMessage);
        }
    }

    private static class ChatMessage {
        private UUID id;
        private String author;
        private String txt;
        private Date moment;
        private UUID idReply;
        private String replyPreview;
        private static final SimpleDateFormat scanFormat =
                new SimpleDateFormat(
                        "MMM d, yyyy h:mm:ss a",
                        Locale.US
                );

        public ChatMessage() {
        }

        public ChatMessage(JSONObject object) throws JSONException {
            setId(UUID.fromString(object.getString("id")));
            setAuthor(object.getString("author"));
            setTxt(object.getString("txt"));
            String moment = object.getString("moment");
            try {
                setMoment(scanFormat.parse(moment));
            } catch (ParseException ex) {
                throw new JSONException("Date (moment) parse error: " + moment);
            }

            if (object.has("idReply")) {
                setIdReply(UUID.fromString(object.getString("idReply")));
            }
            if (object.has("replyPreview")) {
                setReplyPreview(object.getString("replyPreview"));
            }
        }

        public String toJsonString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(
                    "{\"author\":\"%s\", \"txt\":\"%s\"",
                    getAuthor(), getTxt()
            ));

            if (idReply != null) {
                sb.append(String.format(
                        ", \"idReply\":\"%s\"",
                        getIdReply()
                ));
            }
            sb.append("}");

            return sb.toString();
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getTxt() {
            return txt;
        }

        public void setTxt(String txt) {
            this.txt = txt;
        }

        public Date getMoment() {
            return moment;
        }

        public void setMoment(Date moment) {
            this.moment = moment;
        }

        public UUID getIdReply() {
            return idReply;
        }

        public void setIdReply(UUID idReply) {
            this.idReply = idReply;
        }

        public String getReplyPreview() {
            return replyPreview;
        }

        public void setReplyPreview(String replyPreview) {
            this.replyPreview = replyPreview;
        }
    }
}