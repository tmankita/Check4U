package com.example.tmankita.check4u.Camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.tmankita.check4u.R;

public class DrawingView extends View {
    /**
     * Extends View. Just used to draw Rect when the screen is touched
     * for auto focus.
     *
     * Use setHaveTouch function to set the status and the Rect to be drawn.
     * Call invalidate to draw Rect. Call invalidate again after
     * setHaveTouch(false, Rect(0, 0, 0, 0)) to hide the rectangle.
     */

        private boolean haveTouch = false;
        private Rect touchArea;
        private Paint paint;

        public DrawingView(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint = new Paint();
            int color = ContextCompat.getColor(context, R.color.green);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            haveTouch = false;
        }
//0xeed7d7d7
        public void setHaveTouch(boolean val, Rect rect) {
            haveTouch = val;
            touchArea = rect;
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (haveTouch) {
//                drawingPaint.setColor(Color.BLUE);
                canvas.drawRect(
                        touchArea.left, touchArea.top, touchArea.right, touchArea.bottom,
                        paint);
            }
        }

    }

