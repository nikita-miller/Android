package step.learning;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        findViewById(R.id.game_layout)
                .setOnTouchListener(new OnSwipeListener(GameActivity.this) {
                    @Override
                    public void onSwipeLeft() {
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Left",
                                        Toast.LENGTH_SHORT
                                )
                                .show();
                    }

                    @Override
                    public void onSwipeRight() {
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Right",
                                        Toast.LENGTH_SHORT
                                )
                                .show();
                    }

                    @Override
                    public void onSwipeTop() {
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Top",
                                        Toast.LENGTH_SHORT
                                )
                                .show();
                    }

                    @Override
                    public void onSwipeBottom() {
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Bottom",
                                        Toast.LENGTH_SHORT
                                )
                                .show();
                    }
                });
    }
}