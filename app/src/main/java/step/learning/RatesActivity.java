package step.learning;

import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RatesActivity extends AppCompatActivity {
    private final String nbuApiUrl = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json";
    private TextView tvContent;
    private String content;
    private List<Rate> rates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rates);

        tvContent = findViewById(R.id.tv_rates_content);

        new Thread(this::loadUrl).start();
    }

    private void loadUrl() {
        try (InputStream urlStream = new URL(nbuApiUrl).openStream()) {
            StringBuilder sb = new StringBuilder();
            int symbol;
            while ((symbol = urlStream.read()) > -1) {
                sb.append((char) symbol);
            }
            content = new String(
                    sb.toString().getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8
            );
            new Thread(this::parseContent).start();
        } catch (NetworkOnMainThreadException ignored) {
            Log.d("RatesActivity::loadUrl", "NetworkOnMainThreadException");
        } catch (MalformedURLException ex) {
            Log.d("RatesActivity::loadUrl", "MalformedURLException: " + ex.getMessage());
        } catch (IOException ex) {
            Log.d("RatesActivity::loadUrl", "IOException: " + ex.getMessage());
        }
    }

    private void parseContent() {
        try {
            JSONArray array = new JSONArray(content);
            rates = new ArrayList<>();
            int length = array.length();
            JSONObject object;
            for (int i = 0; i < length; ++i) {
                object = array.getJSONObject(i);
                rates.add(new Rate(object));
            }
        } catch (JSONException ex) {
            Log.d(
                    "RatesActivity::parseContent",
                    "JSONException: " + ex.getMessage()
            );
        }
        runOnUiThread(this::showContent);
    }

    private void showContent() {
        StringBuilder sb = new StringBuilder();
        int length = rates.size();
        sb.append(rates.get(0).getExchangeDate());
        sb.append("\n");
        sb.append("\n");

        Rate currentRate;
        for (int i = 0; i < length; ++i) {
            currentRate = rates.get(i);
            sb.append(currentRate.getR030());
            sb.append("\n");
            sb.append(currentRate.getTxt());
            sb.append("\n");
            sb.append(currentRate.getRate());
            sb.append("\n");
            sb.append(currentRate.getCc());
            sb.append("\n");
            sb.append("\n");
        }

        content = sb.toString();
        tvContent.setText(content);
    }

    static class Rate {
        private int r030;
        private String txt;
        private double rate;
        private String cc;
        private String exchangeDate;

        public Rate(JSONObject object) throws JSONException {
            setR030(object.getInt("r030"));
            setTxt(object.getString("txt"));
            setRate(object.getDouble("rate"));
            setCc(object.getString("cc"));
            setExchangeDate(object.getString("exchangedate"));
        }

        public int getR030() {
            return r030;
        }

        public void setR030(int r030) {
            this.r030 = r030;
        }

        public String getTxt() {
            return txt;
        }

        public void setTxt(String txt) {
            this.txt = txt;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }

        public String getCc() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }

        public String getExchangeDate() {
            return exchangeDate;
        }

        public void setExchangeDate(String exchangeDate) {
            this.exchangeDate = exchangeDate;
        }
    }
}