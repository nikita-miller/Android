package step.learning;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private int[][] tiles = new int[4][4];
    private TextView[][] tvTiles = new TextView[4][4];
    private final Random random = new Random();
    private Animation spawnTileAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        spawnTileAnimation = AnimationUtils.loadAnimation(
                GameActivity.this,
                R.anim.spawn_tile
        );
        spawnTileAnimation.reset();

        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                tvTiles[i][j] = findViewById(
                        getResources().getIdentifier(
                                "game_tile_" + i + j,
                                "id",
                                getPackageName()
                        ));
            }

        findViewById(R.id.game_layout)
                .setOnTouchListener(new OnSwipeListener(GameActivity.this) {
                    @Override
                    public void onSwipeLeft() {
                        if (moveLeft()) {
                            spawnTile();
                            return;
                        }
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Can't move left",
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

        spawnTile();
    }

    private void showField() {
        Resources resources = getResources();
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                tvTiles[i][j].setText(String.valueOf(tiles[i][j]));
                tvTiles[i][j].setTextAppearance(
                        resources.getIdentifier(
                                "GameTile" + (tiles[i][j] == 0
                                        ? "Empty"
                                        : tiles[i][j]),
                                "style",
                                getPackageName()
                        ));
                tvTiles[i][j].setBackgroundColor(
                        resources.getColor(
                                resources.getIdentifier(
                                        "game_tile_" +
                                                (tiles[i][j] == 0
                                                        ? "empty"
                                                        : tiles[i][j] > 2048
                                                        ? "other"
                                                        : tiles[i][j]),
                                        "color",
                                        getPackageName()
                                ),
                                getTheme())
                );
            }
    }

    private boolean spawnTile() {
        List<Integer> emptyTileIndexes = new ArrayList<>();
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                if (tiles[i][j] == 0) {
                    emptyTileIndexes.add(i * 10 + j);
                }
//                tiles[i][j] = random.nextInt(14);
//                if (tiles[i][j] > 0) {
//                    tiles[i][j] = (int) Math.pow(2, tiles[i][j]);
//                }
            }

        int count = emptyTileIndexes.size();
        if (count == 0) {
            return false;
        }

        int randIndex = random.nextInt(count);
        int x = emptyTileIndexes.get(randIndex) / 10;
        int y = emptyTileIndexes.get(randIndex) % 10;
        tiles[x][y] = random.nextInt(10) < 9 ? 2 : 4;
        tvTiles[x][y].startAnimation(spawnTileAnimation);
        showField();
        return true;
    }

    private boolean moveLeft() {
        boolean result = false;
        boolean needRepeat;

        for (int i = 0; i < 4; ++i) {
            do {
                needRepeat = false;
                for (int j = 0; j < 3; ++j) {
                    if (tiles[i][j] == 0) {
                        for (int k = j + 1; k < 4; ++k) {
                            if (tiles[i][k] > 0) {
                                tiles[i][j] = tiles[i][k];
                                tiles[i][k] = 0;
                                needRepeat = true;
                                result = true;
                                break;
                            }
                        }
                    }
                }
            } while (needRepeat);
        }

        return result;
    }
}