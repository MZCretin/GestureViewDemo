package com.example.cretin.secondtest.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.example.cretin.secondtest.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by cretin on 16/7/1.
 */
public class GestureView extends View {
    public static final int STATE_REGISTER = 101;
    public static final int STATE_LOGIN = 100;
    private static int panelHeight = 300;
    private int mPanelWidth;
    private Bitmap selectedBitmap;
    private Bitmap unSelectedBitmap;
    private Bitmap selectedBitmapSmall;
    private Bitmap unSelectedBitmapSmall;
    private float pieceWidth;
    private float pieceWidthSmall;
    private float mLineHeight;
    private Paint mPaint;
    private float currX;
    private float currY;
    private List<GestureBean> listDatas;
    private List<GestureBean> listDatasCopy;
    private GestureBean lastGestrue = null;
    private int tryCount;
    private Vibrator vibrate;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private boolean mError;
    private String message = "请绘制手势";
    //失败尝试次数
    private int tempCount = 5;

    //剩余等待时间
    private int leftTime = 30;

    //记录是否尝试次数超过限制
    private boolean mTimeout;

    private int minPointNums = 4;

    //设置一个参数记录当前是出于初始化阶段还是使用阶段
    private int stateFlag = STATE_LOGIN;

    private GestureCallBack gestureCallBack;

    private Context mContext;

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            leftTime--;
            Log.e("HHHHHHHH", "lefttime" + leftTime);
            if ( leftTime == 0 ) {
                if ( mTimer != null )
                    mTimerTask.cancel();
                mTimeout = false;
                message = "请绘制手势";
                mError = false;
                invalidate();
                reset();
                return;
            }
            message = "尝试次数达到最大," + leftTime + "s后重试";
            mError = true;
            invalidate();
        }

    };

    public GestureView(Context context) {
        super(context);
        init(context);
    }

    public GestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GestureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi( Build.VERSION_CODES.LOLLIPOP )
    public GestureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        try {
            gestureCallBack = ( GestureCallBack ) context;
        } catch ( final ClassCastException e ) {
            throw new ClassCastException(context.toString() + " must implement GestureCallBack");
        }

        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#7ec059"));
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(20);
        mPaint.setStyle(Paint.Style.STROKE);
        setBackgroundColor(Color.parseColor("#349B23"));
        listDatas = new ArrayList<>();
        listDatasCopy = new ArrayList<>();

        selectedBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_finger_selected);
        unSelectedBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_finger_unselected);
        selectedBitmapSmall = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_finger_selected);
        unSelectedBitmapSmall = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_finger_unselected);

        //获取振动器
        vibrate = ( Vibrator ) context.getSystemService(Service.VIBRATOR_SERVICE);
        mTimer = new Timer();
        stateFlag = getState();
        if ( stateFlag == STATE_REGISTER ) {
            message = "请设置手势密码";
        } else {
            message = "请输入手势密码以解锁";
        }
    }

    public void setGestureCallBack(GestureCallBack gestureCallBack) {
        this.gestureCallBack = gestureCallBack;
    }

    //重置一些操作
    private void reset() {
        leftTime = 30;
        tempCount = 5;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = Math.min(widthSize, heightSize);
        if ( widthMode == MeasureSpec.UNSPECIFIED ) {
            width = heightSize;
        } else if ( heightMode == MeasureSpec.UNSPECIFIED ) {
            width = widthSize;
        }

        mLineHeight = width / 3;
        setMeasuredDimension(width, width + panelHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPanelWidth = Math.min(w, h);

        pieceWidth = ( int ) (mLineHeight * 0.6f);
        pieceWidthSmall = ( int ) (mLineHeight * 0.15f);
        selectedBitmap = Bitmap.createScaledBitmap(selectedBitmap, ( int ) pieceWidth, ( int ) pieceWidth, false);
        unSelectedBitmap = Bitmap.createScaledBitmap(unSelectedBitmap, ( int ) pieceWidth, ( int ) pieceWidth, false);
        selectedBitmapSmall = Bitmap.createScaledBitmap(selectedBitmap, ( int ) pieceWidthSmall, ( int ) pieceWidthSmall, false);
        unSelectedBitmapSmall = Bitmap.createScaledBitmap(unSelectedBitmap, ( int ) pieceWidthSmall, ( int ) pieceWidthSmall, false);
    }

    private boolean saveState() {
        SharedPreferences sp = mContext.getSharedPreferences("STATE_DATA", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt("state", stateFlag);
        return edit.commit();
    }

    private int getState() {
        SharedPreferences mSharedPreference = mContext.getSharedPreferences("STATE_DATA", Activity.MODE_PRIVATE);
        return mSharedPreference.getInt("state", STATE_REGISTER);
    }

    public boolean clearCache() {
        SharedPreferences sp = mContext.getSharedPreferences("STATE_DATA", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt("state", STATE_REGISTER);
        stateFlag = STATE_REGISTER;
        invalidate();
        return edit.commit();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        GestureBean firstGestrue = null;
        GestureBean currGestrue = null;

        if ( stateFlag == STATE_REGISTER ) {
            //绘制上面的提示点
            drawTipsPoint(canvas);
        } else {
            drawTipsText(canvas);
        }

        for ( int i = 0; i < 3; i++ ) {
            for ( int j = 0; j < 3; j++ ) {
                canvas.drawBitmap(unSelectedBitmap, ( float ) (mLineHeight * (j + 0.5) - pieceWidth / 2), ( float ) (mLineHeight * (i + 0.5) - pieceWidth / 2 + panelHeight), mPaint);
            }
        }
        if ( !listDatas.isEmpty() ) {
            firstGestrue = listDatas.get(0);
            for ( int i = 1; i < listDatas.size(); i++ ) {
                currGestrue = listDatas.get(i);
                canvas.drawLine(( float ) (mLineHeight * (firstGestrue.getX() + 0.5)), ( float ) (mLineHeight * (firstGestrue.getY() + 0.5) + panelHeight), ( float ) (mLineHeight * (currGestrue.getX() + 0.5)), ( float ) (mLineHeight * (currGestrue.getY() + 0.5) + panelHeight), mPaint);
                firstGestrue = currGestrue;
            }

            lastGestrue = listDatas.get(listDatas.size() - 1);
            canvas.drawLine(( float ) (mLineHeight * (lastGestrue.getX() + 0.5)), ( float ) (mLineHeight * (lastGestrue.getY() + 0.5) + panelHeight), currX, currY, mPaint);
            for ( GestureBean bean : listDatas ) {
                canvas.drawBitmap(selectedBitmap, ( float ) (mLineHeight * (bean.getX() + 0.5) - pieceWidth / 2), ( float ) (mLineHeight * (bean.getY() + 0.5) + panelHeight - pieceWidth / 2), mPaint);
            }
        }
    }

    //绘制提示语
    private void drawTipsText(Canvas canvas) {
        float widthMiddleX = mPanelWidth / 2;
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.FILL);
        //设置文字的大小
        mPaint.setTextSize(24 * getScale());
        int widthStr1 = ( int ) mPaint.measureText(message);
        if ( mError ) {
            mPaint.setColor(Color.parseColor("#FF0000"));
        } else {
            mPaint.setColor(Color.parseColor("#FFFFFF"));
        }
        float baseX = widthMiddleX - widthStr1 / 2;
        float baseY = panelHeight / 2 + 24 * getScale();
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float fontTotalHeight = fontMetrics.bottom - fontMetrics.top;
        float offY = fontTotalHeight / 2 - fontMetrics.bottom - 30;
        float newY = baseY + offY;
        canvas.drawText(message, baseX, newY, mPaint);
        mPaint.setColor(Color.parseColor("#7ec059"));
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(20);
    }

    private float getScale() {
        TextView tv = new TextView(mContext);
        tv.setTextSize(1);
        return tv.getTextSize();
    }

    private void drawMessage(Canvas canvas, String message, boolean errorFlag) {
        float widthMiddleX = mPanelWidth / 2;
        float firstY = ( float ) (panelHeight / 2 - pieceWidthSmall / 2 + pieceWidthSmall * 1.25 + 90);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.FILL);
        //设置文字的大小
        mPaint.setTextSize(24 * getScale());
        int widthStr1 = ( int ) mPaint.measureText(message);
        if ( errorFlag ) {
            mPaint.setColor(Color.parseColor("#FF0000"));
        } else {
            mPaint.setColor(Color.parseColor("#FFFFFF"));
        }
        float baseX = widthMiddleX - widthStr1 / 2;
        float baseY = firstY + 40;
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float fontTotalHeight = fontMetrics.bottom - fontMetrics.top;
        float offY = fontTotalHeight / 2 - fontMetrics.bottom - 30;
        float newY = baseY + offY;
        canvas.drawText(message, baseX, newY, mPaint);
        mPaint.setColor(Color.parseColor("#7ec059"));
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(20);
    }

    //绘制提示点
    private void drawTipsPoint(Canvas canvas) {
        float widthMiddleX = mPanelWidth / 2;
        float firstX = widthMiddleX - pieceWidthSmall / 4 - pieceWidthSmall / 2 - pieceWidthSmall;
        float firstY = panelHeight / 2 - pieceWidthSmall / 2 - pieceWidthSmall - pieceWidthSmall / 4 - 10;
        for ( int i = 0; i < 3; i++ ) {
            for ( int j = 0; j < 3; j++ ) {
                canvas.drawBitmap(unSelectedBitmapSmall, ( float ) (firstX + j * (pieceWidthSmall * 1.25)), ( float ) (firstY + i * (pieceWidthSmall * 1.25)), mPaint);
            }
        }

        if ( listDatasCopy != null && !listDatasCopy.isEmpty() ) {
            for ( GestureBean bean : listDatasCopy ) {
                canvas.drawBitmap(selectedBitmapSmall, ( float ) (firstX + bean.getX() * (pieceWidthSmall * 1.25)), ( float ) (firstY + bean.getY() * (pieceWidthSmall * 1.25)), mPaint);
            }
        } else if ( listDatas != null && !listDatas.isEmpty() ) {
            for ( GestureBean bean : listDatas ) {
                canvas.drawBitmap(selectedBitmapSmall, ( float ) (firstX + bean.getX() * (pieceWidthSmall * 1.25)), ( float ) (firstY + bean.getY() * (pieceWidthSmall * 1.25)), mPaint);
            }
        }

        drawMessage(canvas, message, mError);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ( mTimeout ) {
            return true;
        }
        if ( event.getY() >= 0 ) {
            int x = ( int ) ((event.getY() - panelHeight) / mLineHeight);
            int y = ( int ) (event.getX() / mLineHeight);
            currX = event.getX();
            currY = event.getY();
            switch ( event.getAction() ) {
                case MotionEvent.ACTION_DOWN:
                    lastGestrue = null;
                    if ( currX >= 0 && currX <= mPanelWidth && currY >= panelHeight && currY <= panelHeight + mPanelWidth ) {
                        if ( currY <= (x + 0.5) * mLineHeight + pieceWidth / 2 + panelHeight && currY >= (x + 0.5) * mLineHeight - pieceWidth / 2 + panelHeight &&
                                currX <= (y + 0.5) * mLineHeight + pieceWidth / 2 && currX >= (y + 0.5) * mLineHeight - pieceWidth / 2 ) {
                            if ( !listDatas.contains(new GestureBean(y, x)) ) {
                                listDatas.add(new GestureBean(y, x));
                                vibrate.vibrate(50);//震半秒钟
                            }
                        }
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if ( currX >= 0 && currX <= mPanelWidth && currY >= panelHeight && currY <= panelHeight + mPanelWidth ) {
                        //缩小响应范围 在此处需要注意的是 x跟currX在物理方向上是反的哦
                        if ( currY <= (x + 0.5) * mLineHeight + pieceWidth / 2 + panelHeight && currY >= (x + 0.5) * mLineHeight - pieceWidth / 2 + panelHeight &&
                                currX <= (y + 0.5) * mLineHeight + pieceWidth / 2 && currX >= (y + 0.5) * mLineHeight - pieceWidth / 2 ) {
                            if ( !listDatas.contains(new GestureBean(y, x)) ) {
                                listDatas.add(new GestureBean(y, x));
                                vibrate.vibrate(50);//震半秒钟
                            }
                        }
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    if ( lastGestrue != null ) {
                        currX = ( float ) ((lastGestrue.getX() + 0.5) * mLineHeight);
                        currY = ( float ) ((lastGestrue.getY() + 0.5) * mLineHeight);
                    }
                    if ( stateFlag == STATE_LOGIN ) {
                        if ( listDatas.equals(loadSharedPrefferenceData()) ) {
                            mError = false;
                            message = "手势验证成功";
                            postListener(true);
                            invalidate();
                            listDatas.clear();
                            return true;
                        } else {
                            if ( --tempCount == 0 ) {
                                mError = true;
                                message = "尝试次数达到最大,30s后重试";
                                mTimeout = true;
                                listDatas.clear();
                                mTimerTask = new MyTimerTask(handler);
                                mTimer.schedule(mTimerTask, 0, 1000);
                                invalidate();
                                return true;
                            }
                            mError = true;
                            message = "手势错误,还可以再输入" + (tempCount) + "次";
                            listDatas.clear();
                        }
                    } else if ( stateFlag == STATE_REGISTER ) {
                        if ( listDatasCopy == null || listDatasCopy.isEmpty() ) {
                            if ( listDatas.size() < minPointNums ) {
                                listDatas.clear();
                                mError = true;
                                message = "点数不能小于" + minPointNums + "个";
                                invalidate();
                                return true;
                            }
                            listDatasCopy.addAll(listDatas);
                            saveToSharedPrefference(listDatas);
                            listDatas.clear();
                            mError = false;
                            message = "请再一次绘制";
                        } else {
                            loadSharedPrefferenceData();
                            if ( listDatas.equals(listDatasCopy) ) {
                                mError = false;
                                message = "手势设置成功";
                                stateFlag = STATE_LOGIN;
                                postListener(true);
                                saveState();
                            } else {
                                mError = true;
                                message = "两次手势绘制不一致,请重新设置";
                            }
                            listDatas.clear();
                            listDatasCopy.clear();
                            invalidate();
                            return true;
                        }
                    }
                    invalidate();
                    break;
            }
        }
        return true;
    }

    //给接口传递数据
    private void postListener(boolean success) {
        if ( gestureCallBack != null ) {
            gestureCallBack.gestureVerifySuccessListener(stateFlag, listDatas, message, success);
        }
    }

    private boolean saveToSharedPrefference(List<GestureBean> data) {
        SharedPreferences sp = mContext.getSharedPreferences("GESTURAE_DATA", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt("data_size", data.size()); /*sKey is an array*/
        for ( int i = 0; i < data.size(); i++ ) {
            edit.remove("data_" + i);
            edit.putString("data_" + i, data.get(i).getX() + " " + data.get(i).getY());
        }

        return edit.commit();
    }

    public List<GestureBean> loadSharedPrefferenceData() {
        List<GestureBean> list = new ArrayList<>();
        SharedPreferences mSharedPreference = mContext.getSharedPreferences("GESTURAE_DATA", Activity.MODE_PRIVATE);
        int size = mSharedPreference.getInt("data_size", 0);

        for ( int i = 0; i < size; i++ ) {
            String str = mSharedPreference.getString("data_" + i, "0 0");
            list.add(new GestureBean(Integer.parseInt(str.split(" ")[0]), Integer.parseInt(str.split(" ")[1])));
        }
        return list;
    }

    class MyTimerTask extends TimerTask {
        Handler handler;

        public MyTimerTask(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            handler.sendMessage(handler.obtainMessage());
        }

    }

    public class GestureBean {
        private int x;
        private int y;

        @Override
        public String toString() {
            return "GestureBean{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }

        public GestureBean(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            return (( GestureBean ) o).getX() == x && (( GestureBean ) o).getY() == y;
        }
    }

    public int getMinPointNums() {
        return minPointNums;
    }

    public void setMinPointNums(int minPointNums) {
        if ( minPointNums <= 3 )
            this.minPointNums = 3;
        if ( minPointNums >= 9 )
            this.minPointNums = 9;
    }

    public interface GestureCallBack {
        void gestureVerifySuccessListener(int stateFlag, List<GestureBean> data, String message, boolean success);
    }
}
