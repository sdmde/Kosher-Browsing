package org.shield.browser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;

/** Draws both the button and its symbol directly; no image or tint drawable is used. */
public final class IconButton extends View {
    public static final int MENU=-1, SHIELD=-2, HISTORY=-3, DOWNLOAD=-4, SHARE=-5,
        SEARCH=-6, CHEVRON=-7, GRID=-8, PAGE=-9, FULLSCREEN=-10, DESKTOP=-11;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;
    private final int surface, border;
    private int symbol, foreground, count = -1;

    public IconButton(Context context, int symbol, int surface, int foreground, int border) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        this.symbol = symbol;
        this.surface = surface;
        this.foreground = foreground;
        this.border = border;
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setSymbol(int value) { symbol = value; invalidate(); }
    public void setIconColor(int value) { foreground = value; invalidate(); }
    public void setCount(int value) { count = value; invalidate(); }
    @Override public CharSequence getAccessibilityClassName() { return Button.class.getName(); }
    @Override protected void drawableStateChanged() { super.drawableStateChanged(); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(surface);
        RectF bounds = new RectF(2*density,4*density,getWidth()-2*density,getHeight()-4*density);
        canvas.drawRoundRect(bounds,8*density,8*density,paint);
        if (isPressed() || isFocused()) {
            paint.setColor(0x301a73e8);
            canvas.drawRoundRect(bounds,8*density,8*density,paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(density);
        paint.setColor(border);
        canvas.drawRoundRect(bounds,8*density,8*density,paint);

        float size = Math.min(26*density, Math.min(getWidth()-8*density,getHeight()-12*density));
        if (size <= 0) return;
        int save = canvas.save();
        canvas.translate((getWidth()-size)/2f,(getHeight()-size)/2f);
        canvas.scale(size/24f,size/24f);
        if(getLayoutDirection()==View.LAYOUT_DIRECTION_RTL&&(symbol==R.drawable.ic_back||symbol==R.drawable.ic_forward||symbol==CHEVRON)){canvas.translate(24,0);canvas.scale(-1,1);}
        paint.setColor(foreground);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.35f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        path.reset();

        if (count >= 0) {
            canvas.drawRoundRect(new RectF(3,2,21,22),3,3,paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(count < 100 ? 10 : 7);
            canvas.drawText(count > 99 ? "99+" : String.valueOf(count),12,16,paint);
        } else if (symbol == MENU) {
            paint.setStyle(Paint.Style.FILL);for(int i=0;i<3;i++)canvas.drawCircle(12,5+i*7,1.8f,paint);
        } else if(symbol == SHIELD) {
            path.moveTo(12,2);path.lineTo(21,6);path.lineTo(20,14);path.quadTo(18,20,12,23);path.quadTo(6,20,4,14);path.lineTo(3,6);path.close();canvas.drawPath(path,paint);
            path.reset();path.moveTo(8,12);path.lineTo(11,15);path.lineTo(16,9);canvas.drawPath(path,paint);
        } else if(symbol == HISTORY) {
            canvas.drawArc(new RectF(4,4,21,21),-80,300,false,paint);path.moveTo(2,4);path.lineTo(2,10);path.lineTo(8,10);path.moveTo(12,7);path.lineTo(12,13);path.lineTo(16,15);canvas.drawPath(path,paint);
        } else if(symbol == DOWNLOAD) {
            path.moveTo(12,2);path.lineTo(12,15);path.moveTo(7,11);path.lineTo(12,16);path.lineTo(17,11);path.moveTo(4,17);path.lineTo(4,21);path.lineTo(20,21);path.lineTo(20,17);canvas.drawPath(path,paint);
        } else if(symbol == SHARE) {
            path.moveTo(7,11);path.lineTo(17,5);path.moveTo(7,13);path.lineTo(17,19);canvas.drawPath(path,paint);canvas.drawCircle(5,12,3,paint);canvas.drawCircle(19,4,3,paint);canvas.drawCircle(19,20,3,paint);
        } else if(symbol == SEARCH) {
            canvas.drawCircle(10,10,7,paint);path.moveTo(15,15);path.lineTo(22,22);canvas.drawPath(path,paint);
        } else if(symbol == CHEVRON) {
            path.moveTo(9,5);path.lineTo(16,12);path.lineTo(9,19);canvas.drawPath(path,paint);
        } else if(symbol == GRID) {
            for(int y=3;y<20;y+=11)for(int x=3;x<20;x+=11)canvas.drawRoundRect(new RectF(x,y,x+7,y+7),1,1,paint);
        } else if(symbol == PAGE) {
            path.moveTo(5,2);path.lineTo(15,2);path.lineTo(21,8);path.lineTo(21,22);path.lineTo(5,22);path.close();path.moveTo(15,2);path.lineTo(15,8);path.lineTo(21,8);path.moveTo(9,13);path.lineTo(17,13);path.moveTo(9,17);path.lineTo(17,17);canvas.drawPath(path,paint);
        } else if(symbol == FULLSCREEN) {
            path.moveTo(3,9);path.lineTo(3,3);path.lineTo(9,3);path.moveTo(15,3);path.lineTo(21,3);path.lineTo(21,9);path.moveTo(21,15);path.lineTo(21,21);path.lineTo(15,21);path.moveTo(9,21);path.lineTo(3,21);path.lineTo(3,15);canvas.drawPath(path,paint);
        } else if(symbol == DESKTOP) {
            canvas.drawRoundRect(new RectF(2,3,22,17),2,2,paint);path.moveTo(12,17);path.lineTo(12,22);path.moveTo(7,22);path.lineTo(17,22);canvas.drawPath(path,paint);
        } else if (symbol == R.drawable.ic_home) {
            path.moveTo(2.5f,11);path.lineTo(12,3);path.lineTo(21.5f,11);
            path.moveTo(5,9.5f);path.lineTo(5,21);path.lineTo(10,21);path.lineTo(10,15);
            path.lineTo(14,15);path.lineTo(14,21);path.lineTo(19,21);path.lineTo(19,9.5f);
            canvas.drawPath(path,paint);
        } else if (symbol == R.drawable.ic_back || symbol == R.drawable.ic_forward) {
            boolean back = symbol == R.drawable.ic_back;
            path.moveTo(back?20:4,12);path.lineTo(back?4:20,12);
            path.moveTo(back?11:13,5);path.lineTo(back?4:20,12);path.lineTo(back?11:13,19);
            canvas.drawPath(path,paint);
        } else if (symbol == R.drawable.ic_reload) {
            canvas.drawArc(new RectF(4,4,20,20),35,285,false,paint);
            path.moveTo(20,3);path.lineTo(20,10);path.lineTo(13,10);
            canvas.drawPath(path,paint);
        } else if (symbol == R.drawable.ic_settings) {
            for (int i=0;i<32;i++) {
                double angle=Math.toRadians(-90+i*11.25);
                float radius=(i%4==1||i%4==2)?10.5f:8;
                float x=12+(float)Math.cos(angle)*radius,y=12+(float)Math.sin(angle)*radius;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            path.close();canvas.drawPath(path,paint);canvas.drawCircle(12,12,3.5f,paint);
        } else if (symbol == R.drawable.ic_add) {
            path.moveTo(12,4);path.lineTo(12,20);path.moveTo(4,12);path.lineTo(20,12);
            canvas.drawPath(path,paint);
        } else if (symbol == R.drawable.ic_close) {
            path.moveTo(6,6);path.lineTo(18,18);path.moveTo(18,6);path.lineTo(6,18);
            canvas.drawPath(path,paint);
        } else if (symbol == R.drawable.ic_bookmark || symbol == R.drawable.ic_bookmark_saved) {
            for(int i=0;i<10;i++) {
                double angle=Math.toRadians(-90+i*36);
                float radius=i%2==0?10:4.3f;
                float x=12+(float)Math.cos(angle)*radius,y=12+(float)Math.sin(angle)*radius;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            path.close();
            if(symbol==R.drawable.ic_bookmark_saved)paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawPath(path,paint);
        }
        canvas.restoreToCount(save);
    }
}
