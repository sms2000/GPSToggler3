package ogp.com.gpstoggler3.controls;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.apps.AppStore;


public class BTStateSwitch extends TriStateSwitch {
    private AppStore.BTState btState;

    private BTStateListener stateListener;


    public interface BTStateListener {
        void onStateChanged(AppStore.BTState btState);
    }


    public BTStateSwitch(Context context) {
        super(context);
    }

    public BTStateSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public BTStateSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    public AppStore.BTState getBTState() {
        return btState;
    }


    public void setState(AppStore.BTState btState) {
        this.btState = btState;
        createDrawableState();
    }


    public void setFlashListener(BTStateListener stateListener) {
        this.stateListener = stateListener;
    }


    @Override
    protected void init() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (btState) {
                    case LEAVE_AS_IS:
                        btState = AppStore.BTState.ENABLE_WHEN_RUN;
                        break;

                    case ENABLE_WHEN_RUN:
                    default:
                        btState = AppStore.BTState.LEAVE_AS_IS;
                        break;
                }

                setState(btState);
                stateListener.onStateChanged(btState);
            }
        });

        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(getResources().getColor(R.color.colorTransparent));
        setState(AppStore.BTState.LEAVE_AS_IS);
    }


    @Override
    protected void createDrawableState() {
        if (null != stateListener) {
            switch (btState) {
                case LEAVE_AS_IS:
                    setImageResource(R.drawable.bt_leave);
                    break;

                case ENABLE_WHEN_RUN:
                    setImageResource(R.drawable.bt_watch);
                    break;
            }
        }
    }
}
