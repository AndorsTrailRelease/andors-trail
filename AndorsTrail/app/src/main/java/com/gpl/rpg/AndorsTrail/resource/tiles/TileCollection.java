package com.gpl.rpg.AndorsTrail.resource.tiles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.HashMap;
import java.util.Map;

public final class TileCollection {
	private final Bitmap[] bitmaps;
	private final Map<Integer, Bitmap> flippedBitmaps;
	public final int maxTileID;

	public TileCollection(int maxTileID) {
		this.bitmaps = new Bitmap[maxTileID+1];
		this.flippedBitmaps = new HashMap<>();
		this.maxTileID = maxTileID;
	}

	public Bitmap getBitmap(int tileID) {
		return bitmaps[tileID];
	}

	public void setBitmap(int tileID, Bitmap bitmap) {
		bitmaps[tileID] = bitmap;
		flippedBitmaps.remove(tileID); // Remove cached flipped version if it exists
	}

	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint) {
		drawTile(canvas, tile, px, py, mPaint, false);
	}
	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint, boolean isFlippedX) {
		drawTile(canvas, tile, px, py, mPaint, isFlippedX, 0);
	}
	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint, boolean isFlippedX, float rotationDegrees) {
		drawTile(canvas, tile, px, py, mPaint, isFlippedX, rotationDegrees, 1f);
	}
	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint, boolean isFlippedX, float rotationDegrees, float distanceScale) {
		drawTile(canvas, tile, px, py, mPaint, isFlippedX, rotationDegrees, distanceScale, false);
	}
	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint, boolean isFlippedX, float rotationDegrees, float distanceScale, boolean mirrorAcrossAttackAxis) {
		drawTile(canvas, tile, px, py, mPaint, isFlippedX, rotationDegrees, distanceScale, mirrorAcrossAttackAxis, px + 1.5f * (bitmapWidth(tile)), py + 1.5f * (bitmapHeight(tile)), 0f);
	}
	private float bitmapWidth(int tile) { return bitmaps[tile].getWidth() / 3f; }
	private float bitmapHeight(int tile) { return bitmaps[tile].getHeight() / 3f; }
	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint, boolean isFlippedX, float rotationDegrees, float distanceScale, boolean mirrorAcrossAttackAxis, float pivotX, float pivotY, float additionalRotationDegrees) {
		drawTile(canvas, tile, px, py, mPaint, isFlippedX, rotationDegrees, distanceScale, mirrorAcrossAttackAxis, pivotX, pivotY, additionalRotationDegrees, 0f, 0f);
	}
	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint, boolean isFlippedX, float rotationDegrees, float distanceScale, boolean mirrorAcrossAttackAxis, float pivotX, float pivotY, float additionalRotationDegrees, float offsetX, float offsetY) {
		drawTile(canvas, tile, px, py, mPaint, isFlippedX, rotationDegrees, distanceScale, mirrorAcrossAttackAxis, pivotX, pivotY, additionalRotationDegrees, offsetX, offsetY, false);
	}
	public void drawTile(Canvas canvas, int tile, int px, int py, Paint mPaint, boolean isFlippedX, float rotationDegrees, float distanceScale, boolean mirrorAcrossAttackAxis, float pivotX, float pivotY, float additionalRotationDegrees, float offsetX, float offsetY, boolean mirrorAcrossVerticalAxis) {
		if (rotationDegrees != 0 || distanceScale != 1f || mirrorAcrossAttackAxis || offsetX != 0f || offsetY != 0f || mirrorAcrossVerticalAxis) {
			Bitmap bitmap = isFlippedX ? getFlippedBitmap(tile) : bitmaps[tile];
			canvas.save();
			canvas.rotate(additionalRotationDegrees, pivotX, pivotY);
			if (mirrorAcrossVerticalAxis) {
				canvas.scale(-1f, 1f, px + bitmap.getWidth() / 2f, py + bitmap.getHeight() / 2f);
			}
			canvas.rotate(rotationDegrees, px + bitmap.getWidth() / 2f, py + bitmap.getHeight() / 2f);
			if (mirrorAcrossAttackAxis) {
				canvas.rotate(-45f, px + bitmap.getWidth() / 2f, py + bitmap.getHeight() / 2f);
				canvas.scale(1f, -1f, px + bitmap.getWidth() / 2f, py + bitmap.getHeight() / 2f);
				canvas.rotate(45f, px + bitmap.getWidth() / 2f, py + bitmap.getHeight() / 2f);
			}
			float excessReach = bitmap.getWidth() / 3f * (float) Math.sqrt(2.0) * (1f - distanceScale);
			float localOffset = excessReach / (float) Math.sqrt(2.0);
			canvas.drawBitmap(bitmap, px - localOffset + offsetX, py + localOffset + offsetY, mPaint);
			canvas.restore();
			return;
		}
		if (isFlippedX) {
			canvas.drawBitmap(getFlippedBitmap(tile), px, py, mPaint);
		} else canvas.drawBitmap(bitmaps[tile], px, py, mPaint);
	}

	private Bitmap getFlippedBitmap(int tile) {
		if (flippedBitmaps.containsKey(tile)) {
			return flippedBitmaps.get(tile);
		}
		Bitmap flipped = flipBitmapX(bitmaps[tile]);
		flippedBitmaps.put(tile, flipped);
		return flipped;
	}

	private static Bitmap flipBitmapX(Bitmap source) {
		Matrix matrix = new Matrix();
		matrix.postScale(-1, 1, source.getWidth() / 2f, source.getHeight() / 2f);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

}
