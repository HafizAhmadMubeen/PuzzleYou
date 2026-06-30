package com.example.puzzleyou;

import android.graphics.Bitmap;

public class PuzzlePiece {
    public Bitmap image;
    public int correctPosition; // 0 to 15 - where it SHOULD be
    public int rotation;        // 0, 90, 180, 270

    public PuzzlePiece(Bitmap image, int correctPosition, int rotation) {
        this.image = image;
        this.correctPosition = correctPosition;
        this.rotation = rotation;
    }
}