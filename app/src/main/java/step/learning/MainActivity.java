package step.learning;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.calc_button)
                .setOnClickListener(this::btnCalcClick);
        findViewById(R.id.game_button)
                .setOnClickListener(this::btnGameClick);
        findViewById(R.id.rates_button)
                .setOnClickListener(this::btnRatesClick);
        findViewById(R.id.chat_button)
                .setOnClickListener(this::btnChatClick);
        findViewById(R.id.exit_button)
                .setOnClickListener(this::btnExitClick);
    }

    private void btnCalcClick(View v) {
        Intent calcIntent = new Intent(
                MainActivity.this,
                CalcActivity.class
        );
        startActivity(calcIntent);
    }

    private void btnGameClick(View v) {
        Intent gameIntent = new Intent(
                MainActivity.this,
                GameActivity.class
        );
        startActivity(gameIntent);
    }

    private void btnRatesClick(View v) {
        Intent ratesIntent = new Intent(
                MainActivity.this,
                RatesActivity.class
        );
        startActivity(ratesIntent);
    }

    private void btnChatClick(View v) {
        Intent chatIntent = new Intent(
                MainActivity.this,
                ChatActivity.class
        );
        startActivity(chatIntent);
    }

    private void btnExitClick(View v) {
        finish();
    }
}