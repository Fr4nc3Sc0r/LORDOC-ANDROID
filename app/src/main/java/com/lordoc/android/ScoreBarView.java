package com.lordoc.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.lordoc.model.ExamResult;

/** Barra del punteggio (300-1000) con la linea tratteggiata della soglia di superamento. */
final class ScoreBarView extends View {

    private final int score;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    ScoreBarView(Context c, int score) {
        super(c);
        this.score = score;
        setMinimumHeight(Ui.dp(c, 56));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Ui.dp(getContext(), 56));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float barH = Ui.dp(getContext(), 20);
        float radius = Ui.dp(getContext(), 8);
        float top = Ui.dp(getContext(), 8);
        int min = ExamResult.MIN_SCORE;
        int max = ExamResult.MAX_SCORE;

        paint.setStyle(Paint.Style.FILL);
        paint.setPathEffect(null);
        paint.setColor(0xFFE4E8EE);
        canvas.drawRoundRect(new RectF(0, top, w, top + barH), radius, radius, paint);

        float fill = Math.max((float) (score - min) / (max - min) * w, radius * 2);
        paint.setColor(score >= ExamResult.PASSING_SCORE ? Ui.OK : Ui.KO);
        canvas.drawRoundRect(new RectF(0, top, fill, top + barH), radius, radius, paint);

        float passX = (float) (ExamResult.PASSING_SCORE - min) / (max - min) * w;
        paint.setColor(0xFF000000);
        paint.setStrokeWidth(Ui.dp(getContext(), 2));
        paint.setPathEffect(new DashPathEffect(new float[] {Ui.dp(getContext(), 4), Ui.dp(getContext(), 3)}, 0));
        canvas.drawLine(passX, top - Ui.dp(getContext(), 4), passX, top + barH + Ui.dp(getContext(), 4), paint);

        paint.setPathEffect(null);
        paint.setTextSize(Ui.dp(getContext(), 12));
        String label = "Soglia " + ExamResult.PASSING_SCORE;
        float lw = paint.measureText(label);
        float baseline = top + barH + Ui.dp(getContext(), 18);
        canvas.drawText(label, Math.min(passX - lw / 2, w - lw), baseline, paint);
        canvas.drawText(String.valueOf(min), 0, baseline, paint);
    }
}
