package ogp.com.gpstoggler3.controls;

import android.content.Context;
/*
import android.os.Build;
import android.support.annotation.RequiresApi;
*/
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.apps.AppStore;


public class TriStateSwitch extends ImageButton {
    private AppStore.AppState appState;
    private TriStateListener stateListener;


    public interface TriStateListener {
        void onStateChanged(AppStore.AppState appState);
    }


    public TriStateSwitch(Context context) {
        super(context);

        init();
    }


    public TriStateSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }


    public TriStateSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    /*
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TriStateSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init();
    }
    */

    private void init() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int next = ((appState.ordinal() + 1) % AppStore.AppState.values().length);
                setState(AppStore.AppState.values()[next]);
                performFlashClick();
            }
        });

        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(getResources().getColor(R.color.colorTransparent));
        setState(AppStore.AppState.DISABLED);
    }


    private void performFlashClick() {
        stateListener.onStateChanged(appState);
    }


    private void createDrawableState() {
        if (null != stateListener) {
            switch (appState) {
                case DISABLED:
                    setImageResource(R.drawable.b_disabled);
                    break;

                case FOREGROUND:
                    setImageResource(R.drawable.b_foreground);
                    break;

                case BACKGROUND:
                    setImageResource(R.drawable.b_background);
                    break;
            }
        }
    }


    public AppStore.AppState getState() {
        return appState;
    }

    public void setState(AppStore.AppState appState) {
        this.appState = appState;
        createDrawableState();

    }


    public void setFlashListener(TriStateListener stateListener) {
        this.stateListener = stateListener;
    }
}
