package ogp.com.gpstoggler3.controls;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Switch;

import ogp.com.gpstoggler3.apps.AppStore;


public class ThreeStateSwitch extends Switch {
    private AppStore.AppState state = AppStore.AppState.DISABLED;

    public ThreeStateSwitch(Context context) {
        super(context);

        init();
    }


    public ThreeStateSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }


    public ThreeStateSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ThreeStateSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    public void setState(AppStore.AppState appState) {
        state = appState;

        setNewState();
    }

    public AppStore.AppState getState() {
        return state;
    }


    @Override
    public void setChecked(boolean checked) {
    }


    private void init() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    onClickListener();
                }

                return false;
            }
        });
    }


    private void onClickListener() {
        switch (state) {
            case DISABLED:
                state = AppStore.AppState.FOREGROUND;
                break;
            case FOREGROUND:
                state = AppStore.AppState.BACKGROUND;
                break;
            case BACKGROUND:
                state = AppStore.AppState.DISABLED;
                break;
            default:
                state = AppStore.AppState.DISABLED;
                break;
        }

        setNewState();
    }


    private void setNewState() {
        int thumbColor;
        int trackColor;

        switch (state) {
            case FOREGROUND:
                super.setChecked(true);

                thumbColor = Color.argb(255, 255, 80, 80);
                trackColor = thumbColor;
                break;

            case BACKGROUND:
                super.setChecked(true);

                thumbColor = Color.argb(255, 80, 80, 255);
                trackColor = thumbColor;
                break;

            default:
                super.setChecked(false);

                thumbColor = Color.argb(255, 236, 236, 236);
                trackColor = Color.argb(255, 0, 0, 0);
                break;
        }

        try {
            getThumbDrawable().setColorFilter(thumbColor, PorterDuff.Mode.MULTIPLY);
            getTrackDrawable().setColorFilter(trackColor, PorterDuff.Mode.MULTIPLY);
        }
        catch (NullPointerException ignored) {
        }
    }
}
