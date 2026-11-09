package com.lordoc.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.View;

import com.lordoc.model.ExamResult;

import java.util.ArrayList;
import java.util.List;

/** Grafico a linee degli ultimi 30 punteggi, con la soglia di superamento. */
final class ScoreChartView extends View {

    private final List<ExamResult> results = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    ScoreChartView(Context c, List<ExamResult> all) {
        super(c);
        int from = Math.max(0, all.size() - 30);
        results.addAll(all.subList(from, all.size()));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Ui.dp(getContext(), 220));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Context c = getContext();
        float left = Ui.dp(c, 36);
        float right = Ui.dp(c, 8);
        float top = Ui.dp(c, 12);
        float bottom = Ui.dp(c, 24);
        float w = getWidth() - left - right;
        float h = getHeight() - top - bottom;
        int min = ExamResult.MIN_SCORE;
        int max = ExamResult.MAX_SCORE;

        canvas.drawColor(0xFFFFFFFF);
        paint.setTextSize(Ui.dp(c, 10));
        paint.setStrokeWidth(Ui.dp(c, 1));
        for (int v = min; v <= max; v += 100) {
            float y = top + h - (float) (v - min) / (max - min) * h;
            paint.setColor(0xFFEEEEEE);
            canvas.drawLine(left, y, left + w, y, paint);
            paint.setColor(0xFF888888);
            canvas.drawText(String.valueOf(v), Ui.dp(c, 2), y + Ui.dp(c, 4), paint);
        }
        float passY = top + h - (float) (ExamResult.PASSING_SCORE - min) / (max - min) * h;
        paint.setColor(Ui.OK);
        paint.setPathEffect(new DashPathEffect(new float[] {Ui.dp(c, 5), Ui.dp(c, 4)}, 0));
        canvas.drawLine(left, passY, left + w, passY, paint);
        paint.setPathEffect(null);
        canvas.drawText("soglia " + ExamResult.PASSING_SCORE, left + Ui.dp(c, 4), passY - Ui.dp(c, 4), paint);

        if (results.isEmpty()) {
            paint.setColor(0xFF888888);
            paint.setTextSize(Ui.dp(c, 13));
            canvas.drawText("Nessun esame svolto: i progressi appariranno qui.", left, top + h / 2, paint);
            return;
        }
        int n = results.size();
        float prevX = -1;
        float prevY = -1;
        for (int i = 0; i < n; i++) {
            float x = left + (n == 1 ? w / 2 : (float) i / (n - 1) * w);
            int s = results.get(i).getScaledScore();
            float y = top + h - (float) (s - min) / (max - min) * h;
            if (prevX >= 0) {
                paint.setColor(Ui.ACCENT);
                paint.setStrokeWidth(Ui.dp(c, 2.2f));
                canvas.drawLine(prevX, prevY, x, y, paint);
            }
            paint.setColor(s >= ExamResult.PASSING_SCORE ? Ui.OK : Ui.KO);
            canvas.drawCircle(x, y, Ui.dp(c, 4), paint);
            prevX = x;
            prevY = y;
        }
        paint.setColor(0xFF888888);
        paint.setTextSize(Ui.dp(c, 11));
        String label = "Ultimi " + n + " esami";
        canvas.drawText(label, left + w - paint.measureText(label), top + h + Ui.dp(c, 18), paint);
    }
}
