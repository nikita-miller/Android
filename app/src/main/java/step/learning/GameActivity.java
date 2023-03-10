package step.learning;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private final int[][] tiles = new int[4][4];
    private final TextView[][] tvTiles = new TextView[4][4];
    private final Random random = new Random();
    private Animation spawnTileAnimation;
    private int score;
    private TextView tvScore;
    private int bestScore;
    private TextView tvBestScore;
    private final String bestScoreFilename = "best_score.txt";
    private boolean isFreeMode;

    @SuppressLint("DiscouragedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        score = 0;
        tvScore = findViewById(R.id.tv_score);
        bestScore = loadBestScore();
        tvBestScore = findViewById(R.id.tv_best_score);
        tvBestScore.setText(getString(R.string.game_best_score_pattern, bestScore));
        isFreeMode = false;

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
                        if (moveRight()) {
                            spawnTile();
                            return;
                        }
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Can't move right",
                                        Toast.LENGTH_SHORT
                                )
                                .show();
                    }

                    @Override
                    public void onSwipeTop() {
                        if (moveUp()) {
                            spawnTile();
                            return;
                        }
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Can't move up",
                                        Toast.LENGTH_SHORT
                                )
                                .show();
                    }

                    @Override
                    public void onSwipeBottom() {
                        if (moveDown()) {
                            spawnTile();
                            return;
                        }
                        Toast
                                .makeText(
                                        GameActivity.this,
                                        "Can't move down",
                                        Toast.LENGTH_SHORT
                                )
                                .show();
                    }
                });

        spawnTile();
        spawnTile();
    }

    private void saveBestScore() {
        try (FileOutputStream fos = openFileOutput(bestScoreFilename, Context.MODE_PRIVATE)) {
            DataOutputStream writer = new DataOutputStream(fos);
            writer.writeInt(bestScore);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Log.d("saveBestScore", ex.getMessage());
        }

    }

    private int loadBestScore() {
        int bestScore = 0;
        try (FileInputStream fis = openFileInput(bestScoreFilename)) {
            DataInputStream reader = new DataInputStream(fis);
            bestScore = reader.readInt();
            reader.close();
            return bestScore;
        } catch (IOException ex) {
            Log.d("loadBestScore", ex.getMessage());
        }

        return bestScore;
    }

    private boolean isWin() {
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                if (tiles[i][j] == 2048) {
                    return true;
                }
            }

        return false;
    }

    private boolean isGameOver() {
        int current;
        int leftNeighbor;
        int rightNeighbor;
        int topNeighbor;
        int bottomNeighbor;

        // ???????????? ???? ?????????? ???????? ?? ?????????????? ???????????? ?????????? ?????? ?????????????????? ??????????????
        // ???????????? ???????????? ?????????????????????? ???? ?????????????????????? ?????????????? ?? ?????????? ???? ????????????????
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                if (tiles[i][j] == 0) {
                    return false;
                }

                try {
                    leftNeighbor = tiles[i][j - 1];
                } catch (Exception ignored) {
                    leftNeighbor = -1;
                }

                try {
                    rightNeighbor = tiles[i][j + 1];
                } catch (Exception ignored) {
                    rightNeighbor = -1;
                }

                try {
                    topNeighbor = tiles[i - 1][j];
                } catch (Exception ignored) {
                    topNeighbor = -1;
                }

                try {
                    bottomNeighbor = tiles[i + 1][j];
                } catch (Exception ignored) {
                    bottomNeighbor = -1;
                }

                current = tiles[i][j];

                if (current == leftNeighbor
                        || current == rightNeighbor
                        || current == topNeighbor
                        || current == bottomNeighbor) {
                    return false;
                }
            }

        return true;
    }

    private void showWinDialog() {
        new AlertDialog.Builder(
                GameActivity.this,
                androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle(R.string.game_victory_title)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(R.string.game_victory_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_continue, (dialog, which) -> isFreeMode = true)
                .setNegativeButton(R.string.btn_exit, (dialog, which) -> finish())
                .setNeutralButton(R.string.btn_new_game, (dialog, which) -> startNewGame())
                .show();
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(
                GameActivity.this,
                androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle(R.string.game_over_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.game_over_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_new_game, (dialog, which) -> startNewGame())
                .setNegativeButton(R.string.btn_exit, (dialog, which) -> finish())
                .show();
    }

    private void startNewGame() {
        score = 0;
        isFreeMode = false;
        emptyField();
        spawnTile();
        spawnTile();
    }

    private void emptyField() {
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                tiles[i][j] = 0;
            }
    }

    @SuppressLint("DiscouragedApi")
    private void showField() {
        if (isGameOver()) {
            showGameOverDialog();
        }
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
        tvScore.setText(getString(R.string.game_score_pattern, score));
        if (score > bestScore) {
            bestScore = score;
            saveBestScore();
            tvBestScore.setText(getString(R.string.game_best_score_pattern, bestScore));
        }
        if (!isFreeMode) {
            if (isWin()) {
                showWinDialog();
            }
        }
    }

    private void spawnTile() {
        List<Integer> emptyTileIndexes = new ArrayList<>();
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                if (tiles[i][j] == 0) {
                    emptyTileIndexes.add(i * 10 + j);
                }
            }

        int count = emptyTileIndexes.size();
        if (count == 0) {
            return;
        }

        int randIndex = random.nextInt(count);
        int x = emptyTileIndexes.get(randIndex) / 10;
        int y = emptyTileIndexes.get(randIndex) % 10;
        tiles[x][y] = random.nextInt(10) < 9 ? 2 : 4;
        tvTiles[x][y].startAnimation(spawnTileAnimation);
        showField();
    }

    private boolean moveLeft() {
        boolean result = false;
        boolean needRepeat;
        boolean isTileEmpty;
        boolean tilesEqual;

        for (int i = 0; i < 4; ++i) {
            do {
                needRepeat = false;
                for (int j = 0; j < 3; ++j) {
                    isTileEmpty = tiles[i][j] == 0;
                    if (isTileEmpty) {
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

            for (int j = 0; j < 3; ++j) {
                isTileEmpty = tiles[i][j] == 0;
                tilesEqual = tiles[i][j] == tiles[i][j + 1];
                if (!isTileEmpty && tilesEqual) {
                    tiles[i][j] *= 2;
                    for (int k = j + 1; k < 3; ++k) {
                        tiles[i][k] = tiles[i][k + 1];
                    }
                    tiles[i][3] = 0;
                    result = true;
                    score += tiles[i][j];
                }
            }
        }

        return result;
    }

    private boolean moveRight() {
        boolean result = false;
        boolean needRepeat;
        boolean isTileEmpty;
        boolean tilesEqual;

        for (int i = 0; i < 4; ++i) {
            do {
                needRepeat = false;
                for (int j = 3; j > 0; --j) {
                    isTileEmpty = tiles[i][j] == 0;
                    if (isTileEmpty) {
                        for (int k = j - 1; k > -1; --k) {
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

            for (int j = 3; j > 0; --j) {
                isTileEmpty = tiles[i][j] == 0;
                tilesEqual = tiles[i][j] == tiles[i][j - 1];
                if (!isTileEmpty && tilesEqual) {
                    tiles[i][j] *= 2;
                    for (int k = j - 1; k > 0; --k) {
                        tiles[i][k] = tiles[i][k - 1];
                    }
                    tiles[i][0] = 0;
                    result = true;
                    score += tiles[i][j];
                }
            }
        }

        return result;
    }

    private boolean moveUp() {
        boolean result = false;
        boolean needRepeat;
        boolean isTileEmpty;
        boolean tilesEqual;

        for (int i = 0; i < 4; ++i) {
            do {
                needRepeat = false;
                for (int j = 0; j < 3; ++j) {
                    isTileEmpty = tiles[j][i] == 0;
                    if (isTileEmpty) {
                        for (int k = j + 1; k < 4; ++k) {
                            if (tiles[k][i] > 0) {
                                tiles[j][i] = tiles[k][i];
                                tiles[k][i] = 0;
                                needRepeat = true;
                                result = true;
                                break;
                            }
                        }
                    }
                }
            } while (needRepeat);

            for (int j = 0; j < 3; ++j) {
                isTileEmpty = tiles[j][i] == 0;
                tilesEqual = tiles[j][i] == tiles[j + 1][i];
                if (!isTileEmpty && tilesEqual) {
                    tiles[j][i] *= 2;
                    for (int k = j + 1; k < 3; ++k) {
                        tiles[k][i] = tiles[k + 1][i];
                    }
                    tiles[3][i] = 0;
                    result = true;
                    score += tiles[j][i];
                }
            }
        }

        return result;
    }

    private boolean moveDown() {
        boolean result = false;
        boolean needRepeat;
        boolean isTileEmpty;
        boolean tilesEqual;

        for (int i = 0; i < 4; ++i) {
            do {
                needRepeat = false;
                for (int j = 3; j > 0; --j) {
                    isTileEmpty = tiles[j][i] == 0;
                    if (isTileEmpty) {
                        for (int k = j - 1; k >= 0; --k) {
                            if (tiles[k][i] > 0) {
                                tiles[j][i] = tiles[k][i];
                                tiles[k][i] = 0;
                                needRepeat = true;
                                result = true;
                                break;
                            }
                        }
                    }
                }
            } while (needRepeat);

            for (int j = 3; j > 0; --j) {
                isTileEmpty = tiles[j][i] == 0;
                tilesEqual = tiles[j][i] == tiles[j - 1][i];
                if (!isTileEmpty && tilesEqual) {
                    tiles[j][i] *= 2;
                    for (int k = j - 1; k > 0; --k) {
                        tiles[k][i] = tiles[k - 1][i];
                    }
                    tiles[0][i] = 0;
                    result = true;
                    score += tiles[j][i];
                }
            }
        }

        return result;
    }
}