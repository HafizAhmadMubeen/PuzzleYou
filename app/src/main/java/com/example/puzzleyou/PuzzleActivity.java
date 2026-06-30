package com.example.puzzleyou;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PuzzleActivity extends AppCompatActivity {

    FrameLayout puzzleContainer;
    TextView tvMoves, tvTimer;
    Button btnReset;

    Bitmap selectedImage;
    List<PuzzlePiece> basePieces; // unshuffled reference, used for Reset

    PuzzlePiece[] livePieces = new PuzzlePiece[16];
    ImageView[] pieceViews = new ImageView[16];
    float[] slotX = new float[16];
    float[] slotY = new float[16];
    int pieceWidth, pieceHeight;
    int containerW, containerH;
    static final int GRID_GAP = 4; // px gap between pieces, creates visible grid lines

    float dX, dY, downX, downY;
    static final int TOUCH_THRESHOLD = 15;

    int moveCount = 0;
    int elapsedSeconds = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        puzzleContainer = findViewById(R.id.puzzleContainer);
        tvMoves = findViewById(R.id.tvMoves);
        tvTimer = findViewById(R.id.tvTimer);
        btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> resetPuzzle());

        String imagePath = getIntent().getStringExtra("imagePath");
        selectedImage = BitmapFactory.decodeFile(imagePath);

        if (selectedImage == null) {
            Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        timerRunnable = () -> {
            elapsedSeconds++;
            updateTimerText();
            timerHandler.postDelayed(timerRunnable, 1000);
        };

        puzzleContainer.post(() -> {
            containerW = puzzleContainer.getWidth();
            containerH = puzzleContainer.getHeight();

            basePieces = cutImageIntoPieces(selectedImage, containerW, containerH);
            List<PuzzlePiece> shuffled = shufflePieces(new ArrayList<>(basePieces));
            setupPuzzleBoard(shuffled, containerW, containerH);
            startTimer();
        });
    }

    // ---------- RESET ----------
    private void resetPuzzle() {
        if (basePieces == null) return;
        List<PuzzlePiece> shuffled = shufflePieces(new ArrayList<>(basePieces));
        setupPuzzleBoard(shuffled, containerW, containerH);

        moveCount = 0;
        updateMovesText();

        elapsedSeconds = 0;
        updateTimerText();
        stopTimer();
        startTimer();
    }

    // ---------- TIMER ----------
    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateTimerText() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        tvTimer.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    private void updateMovesText() {
        tvMoves.setText("Moves: " + moveCount);
    }

    // ---------- CUTTING ----------
    private List<PuzzlePiece> cutImageIntoPieces(Bitmap original, int targetW, int targetH) {
        List<PuzzlePiece> pieces = new ArrayList<>();

        float targetRatio = (float) targetW / targetH;
        float sourceRatio = (float) original.getWidth() / original.getHeight();

        int cropW, cropH;
        if (sourceRatio > targetRatio) {
            cropH = original.getHeight();
            cropW = (int) (cropH * targetRatio);
        } else {
            cropW = original.getWidth();
            cropH = (int) (cropW / targetRatio);
        }

        int x = (original.getWidth() - cropW) / 2;
        int y = (original.getHeight() - cropH) / 2;
        Bitmap croppedBitmap = Bitmap.createBitmap(original, x, y, cropW, cropH);

        int cellW = cropW / 4;
        int cellH = cropH / 4;
        int position = 0;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                Bitmap pieceBitmap = Bitmap.createBitmap(
                        croppedBitmap, col * cellW, row * cellH, cellW, cellH);
                pieces.add(new PuzzlePiece(pieceBitmap, position, 0));
                position++;
            }
        }
        return pieces;
    }

    // ---------- SHUFFLE ----------
    private List<PuzzlePiece> shufflePieces(List<PuzzlePiece> pieces) {
        Collections.shuffle(pieces);
        int[] possibleRotations = {0, 90, 180, 270};
        Random random = new Random();
        for (PuzzlePiece piece : pieces) {
            piece.rotation = possibleRotations[random.nextInt(4)];
        }
        return pieces;
    }

    // ---------- BUILD BOARD ----------
    private void setupPuzzleBoard(List<PuzzlePiece> pieces, int containerW, int containerH) {
        puzzleContainer.removeAllViews();

        pieceWidth = containerW / 4;
        pieceHeight = containerH / 4;

        for (int i = 0; i < 16; i++) {
            int row = i / 4;
            int col = i % 4;

            slotX[i] = col * pieceWidth;
            slotY[i] = row * pieceHeight;

            livePieces[i] = pieces.get(i);

            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            iv.setImageBitmap(rotateBitmap(livePieces[i].image, livePieces[i].rotation));

            // shrink slightly so a thin yellow grid line shows between pieces
            int ivWidth = pieceWidth - GRID_GAP;
            int ivHeight = pieceHeight - GRID_GAP;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ivWidth, ivHeight);
            iv.setLayoutParams(params);
            iv.setX(slotX[i] + GRID_GAP / 2f);
            iv.setY(slotY[i] + GRID_GAP / 2f);
            iv.setTag(i);

            iv.setOnTouchListener(pieceTouchListener);

            pieceViews[i] = iv;
            puzzleContainer.addView(iv);
        }
    }

    // ---------- TOUCH ----------
    private final View.OnTouchListener pieceTouchListener = (v, event) -> {
        int slotIndex = (int) v.getTag();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                dX = v.getX() - event.getRawX();
                dY = v.getY() - event.getRawY();
                v.bringToFront();
                return true;

            case MotionEvent.ACTION_MOVE:
                v.setX(event.getRawX() + dX);
                v.setY(event.getRawY() + dY);
                return true;

            case MotionEvent.ACTION_UP:
                float moveDist = Math.abs(event.getRawX() - downX) + Math.abs(event.getRawY() - downY);

                if (moveDist < TOUCH_THRESHOLD) {
                    rotatePiece(slotIndex);
                } else {
                    float centerX = v.getX() + pieceWidth / 2f;
                    float centerY = v.getY() + pieceHeight / 2f;
                    int targetSlot = findNearestSlot(centerX, centerY);
                    swapPieces(slotIndex, targetSlot);
                }
                moveCount++;
                updateMovesText();
                checkWinCondition();
                return true;
        }
        return false;
    };

    private int findNearestSlot(float x, float y) {
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < 16; i++) {
            float centerSlotX = slotX[i] + pieceWidth / 2f;
            float centerSlotY = slotY[i] + pieceHeight / 2f;
            double dist = Math.pow(x - centerSlotX, 2) + Math.pow(y - centerSlotY, 2);
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    private void swapPieces(int slotA, int slotB) {
        PuzzlePiece temp = livePieces[slotA];
        livePieces[slotA] = livePieces[slotB];
        livePieces[slotB] = temp;

        refreshSlot(slotA);
        refreshSlot(slotB);
    }

    private void rotatePiece(int slotIndex) {
        livePieces[slotIndex].rotation = (livePieces[slotIndex].rotation + 90) % 360;
        refreshSlot(slotIndex);
    }

    private void refreshSlot(int slotIndex) {
        ImageView iv = pieceViews[slotIndex];
        iv.setX(slotX[slotIndex] + GRID_GAP / 2f);
        iv.setY(slotY[slotIndex] + GRID_GAP / 2f);
        iv.setImageBitmap(rotateBitmap(livePieces[slotIndex].image, livePieces[slotIndex].rotation));
    }

    private Bitmap rotateBitmap(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    // ---------- WIN CHECK ----------
    private void checkWinCondition() {
        for (int i = 0; i < 16; i++) {
            if (livePieces[i].correctPosition != i || livePieces[i].rotation != 0) {
                return;
            }
        }
        stopTimer();
        showWinDialog();
    }

    private void showWinDialog() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        String summary = String.format("Solved in %d moves, %02d:%02d!", moveCount, minutes, seconds);

        new AlertDialog.Builder(this)
                .setTitle("🎉 Congratulations!")
                .setMessage(summary + "\nWant to play again?")
                .setCancelable(false)
                .setPositiveButton("Play Again", (dialog, which) -> finish())
                .setNegativeButton("Exit", (dialog, which) -> finishAffinity())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer(); // avoid leaks when leaving screen
    }
}