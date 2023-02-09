package step.learning;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CalcActivity extends AppCompatActivity {
    private TextView tvHistory;
    private TextView tvResult;
    private String minusSign;
    private String decimalSign;
    private boolean resultClearNeeded;
    private boolean historyClearNeeded;
    private double leftOperand;
    private String operation;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("history", tvHistory.getText());
        outState.putCharSequence("result", tvResult.getText());
        Log.d(CalcActivity.class.getName(), "Data saved");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tvHistory.setText(savedInstanceState.getCharSequence("history"));
        tvResult.setText(savedInstanceState.getCharSequence("result"));
        Log.d(CalcActivity.class.getName(), "Data restored");
    }

    @SuppressLint("DiscouragedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        tvHistory = findViewById(R.id.tv_history);
        tvHistory.setText("");

        tvResult = findViewById(R.id.tv_result);
        tvResult.setText("0");

        minusSign = getString(R.string.minus_sign);
        decimalSign = getString(R.string.decimal_sign);

        for (int i = 0; i < 10; ++i) {
            findViewById(
                    getResources()
                            .getIdentifier(
                                    "button_digit_" + i,
                                    "id",
                                    getPackageName()
                            )
            ).setOnClickListener(this::digitClick);
        }

        findViewById(R.id.button_negate)
                .setOnClickListener(this::negateClick);
        findViewById(R.id.button_dot)
                .setOnClickListener(this::dotClick);
        findViewById(R.id.button_backspace)
                .setOnClickListener(this::backspaceClick);
        findViewById(R.id.button_inverse)
                .setOnClickListener(this::inverseClick);
        findViewById(R.id.button_sqrt)
                .setOnClickListener(this::sqrtClick);

        findViewById(R.id.button_clear_entry)
                .setOnClickListener(this::clearEntryClick);
        findViewById(R.id.button_clear)
                .setOnClickListener(this::clearClick);

        findViewById(R.id.button_plus)
                .setOnClickListener(this::fnButtonClick);
        findViewById(R.id.button_minus)
                .setOnClickListener(this::fnButtonClick);
        findViewById(R.id.button_multiply)
                .setOnClickListener(this::fnButtonClick);
        findViewById(R.id.button_divide)
                .setOnClickListener(this::fnButtonClick);

        findViewById(R.id.button_equals)
                .setOnClickListener(this::equalsClick);
    }

    private void negateClick(View v) {
        String result = tvResult.getText().toString();
        if (result.equals("0")) {
            return;
        }
        if (result.startsWith(minusSign)) {
            result = result.substring(1);
        } else {
            result = minusSign + result;
        }

        tvResult.setText(result);
    }

    private void digitClick(View v) {
        String result = tvResult.getText().toString();
        if (resultClearNeeded) {
            resultClearNeeded = false;
            result = "0";
        }
        if (result.length() >= 10) {
            return;
        }
        String digit = ((Button) v).getText().toString();

        if (result.equals("0")) {
            result = digit;
        } else {
            result += digit;
        }

        if (historyClearNeeded) {
            tvHistory.setText("");
            historyClearNeeded = false;
        }
        tvResult.setText(result);
    }

    private void dotClick(View v) {
        String result = tvResult.getText().toString();
        if (result.contains(",")) {
            return;
        }

        result += ',';
        tvResult.setText(result);
    }

    private void backspaceClick(View v) {
        if (historyClearNeeded) {
            tvHistory.setText("");
            historyClearNeeded = false;
        }
        if (resultClearNeeded) {
            resultClearNeeded = false;
        }
        String result = tvResult.getText().toString();
        int length = result.replace(minusSign, "").length();

        if (length == 1) {
            result = "0";
        } else {
            result = result.substring(0, length - 1);
        }

        tvResult.setText(result);
    }

    private void inverseClick(View v) {
        String result = tvResult.getText().toString();
        double arg = parseResult(result);
        if (arg == 0) {
            alert(R.string.division_by_zero);
            return;
        }
        tvHistory.setText(String.format("1/(%s) = ", result));
        showResult(1 / arg);
    }

    private double parseResult(String result) {
        return Double.parseDouble(
                result
                        .replace(minusSign, "-")
                        .replace(decimalSign, ".")
        );
    }

    private void showResult(double arg) {
        String result = String.valueOf(arg);
        int maxLength = 10;
        if (result.startsWith("-")) {
            ++maxLength;
        }
        if (result.contains(".")) {
            ++maxLength;
        }
        if (result.length() >= maxLength) {
            result = result.substring(0, maxLength);
        }

        tvResult.setText(
                result
                        .replace("-", minusSign)
                        .replace(".", decimalSign)
        );
    }

    private void sqrtClick(View v) {
        String result = tvResult.getText().toString();
        double arg = parseResult(result);
        if (arg < 0) {
            alert(R.string.invalid_sqrt);
            return;
        }

        tvHistory.setText(String.format("√(%s) = ", result));
        showResult(Math.sqrt(arg));
    }

    private void alert(int stringId) {
        Toast
                .makeText(
                        CalcActivity.this,
                        stringId,
                        Toast.LENGTH_SHORT
                )
                .show();
        Vibrator vibrator;
        long[] vibrationPattern = {0, 200, 100, 200};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager)
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                    VibrationEffect
                            .createWaveform(vibrationPattern, -1)
            );
        } else {
            vibrator.vibrate(vibrationPattern, -1);
        }
    }

    private void clearEntryClick(View v) {
        tvResult.setText("0");
    }

    private void clearClick(View v) {
        tvHistory.setText("");
        tvResult.setText("0");
    }

    private void fnButtonClick(View v) {
        String fn = ((Button) v).getText().toString();
        String result = tvResult.getText().toString();
        String history = String.format("%s %s", result, fn);
        tvHistory.setText(history);
        resultClearNeeded = true;
        operation = fn;
        leftOperand = parseResult(result);
    }

    private void equalsClick(View v) {
        String result = tvResult.getText().toString();
        String history = tvHistory.getText().toString();
        tvHistory.setText(String.format("%s %s =", history, result));
        double rightOperand = parseResult(result);
        if (operation.equals(getString(R.string.plus_sign))) {
            showResult(leftOperand + rightOperand);
        }
        resultClearNeeded = true;
        historyClearNeeded = true;
    }
}