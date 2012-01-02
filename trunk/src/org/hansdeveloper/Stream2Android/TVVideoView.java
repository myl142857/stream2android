package org.hansdeveloper.Stream2Android;

import android.content.Context;
import android.widget.VideoView;
import android.util.AttributeSet;
import android.util.Log;

public class TVVideoView extends VideoView {
	private static final String TAG = "Stream2Android";

    public TVVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public TVVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public TVVideoView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see android.widget.VideoView#onMeasure(int, int)
	 */
	int mVideoWidth = 1920;
	int mVideoHeight = 1080;
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		
		Log.d(TAG, ">onMeasure widthMeasureSpec="+ widthMeasureSpec + ", heightMeasureSpec=" + heightMeasureSpec);

		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        
		Log.d(TAG, "onMeasure getDefaultSize width="+ width + ", height=" + height);
		
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mVideoWidth > 0 && mVideoHeight > 0) {

            if ( mVideoWidth * height  > width * mVideoHeight ) {
                //Log.i("@@@", "image too tall, correcting");
                height = width * mVideoHeight / mVideoWidth;
            } else if ( mVideoWidth * height  < width * mVideoHeight ) {
                //Log.i("@@@", "image too wide, correcting");
                width = height * mVideoWidth / mVideoHeight;
            } else {
                //Log.i("@@@", "aspect ratio is correct: " +
                        //width+"/"+height+"="+
                        //mVideoWidth+"/"+mVideoHeight);
            }
        }
        //Log.i("@@@@@@@@@@", "setting size: " + width + 'x' + height);
		Log.d(TAG, "onMeasure width="+ width + ", height=" + height);
        setMeasuredDimension(width, height);
		Log.d(TAG, "onMeasure setMeasuredDimension="+ getMeasuredWidth() + "x" + getMeasuredHeight());
        
        if (super.getHolder() != null)
        {
            Log.d(TAG, "onMeasure setFixedSize="+ getMeasuredWidth() + "x" + getMeasuredHeight());
        	//super.getHolder().setFixedSize(getMeasuredWidth(), getMeasuredWidth()*9/16);
            super.getHolder().setFixedSize(getMeasuredWidth(), getMeasuredHeight());
        }
	}
}
