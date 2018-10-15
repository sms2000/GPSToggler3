package ogp.com.gpstoggler3.controls;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.View;

import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.apps.AppStore;


public class TriStateSwitch extends AppCompatImageButton {
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


    protected void init() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (appState) {
                    case DISABLED:
                        appState = AppStore.AppState.FOREGROUND;
                        break;

                    case FOREGROUND:
                        appState = AppStore.AppState.BACKGROUND;
                        break;

                    case BACKGROUND:
                    default:
                        appState = AppStore.AppState.DISABLED;
                        break;
                }

                setState(appState);
                stateListener.onStateChanged(appState);
            }
        });

        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(getResources().getColor(R.color.colorTransparent));
        setState(AppStore.AppState.DISABLED);
    }


    protected void createDrawableState() {
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
}
