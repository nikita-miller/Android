package step.learning;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    private String chatUrlString;
    private String channelId;
    private String content;
    private LinearLayout chatContainer;
    private List<ChatMessage> chatMessages;
    private EditText etUserName;
    private EditText etUserMessage;
    private ChatMessage userMessage;
    private ScrollView svChatMessages;
    private Handler handler;
    private MediaPlayer incomingMessagePlayer;

    private static final SimpleDateFormat scanFormat =
            new SimpleDateFormat(
                    "MMM d, yyyy h:mm:ss a",
                    Locale.US
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatUrlString = getString(R.string.chat_api_url);
        channelId = getString(R.string.channel_id);

        chatContainer = findViewById(R.id.chat_container);
        etUserName = findViewById(R.id.et_user_name);
        etUserMessage = findViewById(R.id.et_user_message);
        svChatMessages = findViewById(R.id.sv_chat_messages);

        findViewById(R.id.btn_chat_send)
                .setOnClickListener(this::sendButtonClick);

        chatMessages = new ArrayList<>();
        handler = new Handler();
        handler.post(this::updateChat);

        incomingMessagePlayer = MediaPlayer.create(this, R.raw.sound_1);
//        handler.postDelayed(this::showNotification, 3000);
    }

    private void updateChat() {
        new Thread(this::loadUrl).start();
        handler.postDelayed(this::updateChat, 3000);
    }

    private void sendButtonClick(View v) {
        String author = etUserName.getText().toString();
        if (author.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Enter author's name",
                    Toast.LENGTH_SHORT
            ).show();
            etUserName.requestFocus();
            return;
        }

        String messageTxt = etUserMessage.getText().toString();
        if (messageTxt.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Enter text of the message",
                    Toast.LENGTH_SHORT
            ).show();
            etUserMessage.requestFocus();
            return;
        }
        etUserMessage.setText("");

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
            String responseBody = new String(
                    bytes.toByteArray(),
                    StandardCharsets.UTF_8
            );
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

                int length = array.length();
                for (int i = 0; i < length; ++i) {
                    ChatMessage tmp = new ChatMessage(
                            array.getJSONObject(i)
                    );
                    if (chatMessages.stream().noneMatch(
                            cm -> cm.getId().equals(tmp.getId())
                    )
                    ) {
                        chatMessages.add(tmp);
                    }
                }
                chatMessages.sort(Comparator.comparing(ChatMessage::getMoment));

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
        boolean scrollNeeded = false;
        for (ChatMessage chatMessage : chatMessages) {
            if (chatMessage.getView() != null) {
                continue;
            }

            Log.d("uuid", chatMessage.getId().toString());
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
            chatMessage.setView(tvMessage);
            tvMessage.setTag(chatMessage);
            scrollNeeded = true;
        }

        if (scrollNeeded) {
            // ?????????? ???????? ?????? ?????????????????? ?????? TextView ???????????????????? ???????????? ????????
            // ?????????? post ???????????????????? runnable object ?? ?????????????? ?????????????????? ????????????????????
            // ?? ?????????????????? ?????? ?? UI ????????????
            svChatMessages.post(
                    () -> svChatMessages.fullScroll(ScrollView.FOCUS_DOWN)
            );
        }
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(android.R.drawable.sym_def_app_icon)
                        .setContentTitle("Chat")
                        .setContentText("New message in chat")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = notificationBuilder.build();
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        10002
                );

                return;
            }
        }

        notificationManager.notify(1002, notification);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 10002) {
        }
    }

    private static class ChatMessage {
        private View view;
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
                setMoment(scanFormat.parse("Feb 26, 2023 9:23:59 PM"));
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

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
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