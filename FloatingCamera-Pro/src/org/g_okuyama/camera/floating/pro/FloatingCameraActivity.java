package org.g_okuyama.camera.floating.pro;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FloatingCameraActivity extends Activity {
    private static final String TAG = "FloatingCamera";
    
    static final int MENU_DISP_GALLERY = 1;
    static final int MENU_DISP_SETTING = 2;

    static final int REQUEST_CODE = 1;
    static final int RESPONSE_COLOR_EFFECT = 1;
    static final int RESPONSE_SCENE_MODE = 2;
    static final int RESPONSE_WHITE_BALANCE = 3;
    static final int RESPONSE_PICTURE_SIZE = 4;
    static final int RESPONSE_SHOOT_NUM = 5;
    static final int RESPONSE_INTERVAL = 6;
    static final int RESPONSE_PREVIEW_SIZE = 7;
    
    private int mCount = 0;
    private TextView mText;
    private Context mContext;
    private CameraPreview mPreview = null;
    SurfaceView mSurface;

    //�B�e�����ۂ��i0:��~���A1�F�B�e���j
    public int mMode = 0;
    private boolean mSleepFlag = false;
    //�s���`�Ɣ��f���邩�̃t���O
    private boolean mPinchFlag = false;
    //�s���`���t���O
    public boolean isPinch = false;
    //�s���`���I�������Ƃ��̃t���O
    private boolean mScaleEndFlag = false;
    
    private ImageButton mButton = null;
    private ImageButton mFocusButton = null;
    private ImageButton mGalleryButton = null;
    private ImageButton mSettingButton = null;
    private ContentResolver mResolver;
    private SeekBar mSeekBar = null;
    
    //�S�̂̉�ʃT�C�Y
    int mWidth = 0;
    int mHeight = 0;
    
    //surfaceview�̍���̍��W
    float mSurfaceX;
    float mSurfaceY;
    //surfaceview�̃T�C�Y
    float mSurfaceWidth;
    float mSurfaceHeight;
    
    ScaleGestureDetector mScaleGesture;
    float mSpanPrev = 0.0f;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);
        mContext = getApplicationContext();
        
        mResolver = getContentResolver();
        
        //�ݒ�l�̎擾
        String effect = FloatingCameraPreference.getCurrentEffect(this);
        String scene = FloatingCameraPreference.getCurrentSceneMode(this);
        String white = FloatingCameraPreference.getCurrentWhiteBalance(this);
        String size = FloatingCameraPreference.getCurrentPictureSize(this);
        
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();
        mWidth = disp.getWidth();
        mHeight = disp.getHeight();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mSurface = (SurfaceView)findViewById(R.id.camera);
        SurfaceHolder holder = mSurface.getHolder();
        
        //�v���r���[�T�C�Y�ݒ�
        int idx = Integer.parseInt(FloatingCameraPreference.getCurrentPreviewSize(this));
        setPreviewSize(idx);
        
        mPreview = new CameraPreview(this);
        mPreview.setField(effect, scene, white, size, mWidth, mHeight);
        holder.addCallback(mPreview);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mText = (TextView)findViewById(R.id.text1);
    	//mText.setText(/*mNum + System.getProperty("line.separator") + */"0" + " ");

    	//�A�ʖ����ݒ�
        String num = FloatingCameraPreference.getCurrentShootNum(this);
        if(!num.equals("0")){
            mPreview.setShootNum(Integer.valueOf(num));
        }
        
        //�A�ʊԊu�ݒ�
        String interval = FloatingCameraPreference.getCurrentInterval(this);
        if(!interval.equals("0")){
            mPreview.setInterval(Integer.valueOf(interval));
        }

        //register UI Listener
    	setListener();    	
    }
    
    private void setListener(){
        mScaleGesture = new ScaleGestureDetector(this, new MySimpleOnScaleGestureListener());
        //SurfaceView��Ń^�b�`�����Ƃ��̂�View�𓮂�����
        mSurface.setOnTouchListener(new MyOnTouchListener());        

        mButton = (ImageButton)findViewById(R.id.imgbtn);
        mButton.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                if(mPreview != null){
                    if(mMode == 0){
                        mPreview.resumeShooting();
                        mMode = 1;
                        //�B�e���͑��̃{�^���������Ȃ�����
                        //mFocusButton.setVisibility(View.INVISIBLE);
                        //mGalleryButton.setVisibility(View.INVISIBLE);
                        //mSettingButton.setVisibility(View.INVISIBLE);
                        mGalleryButton.setVisibility(View.GONE);
                        mSettingButton.setVisibility(View.GONE);
                        mText.setVisibility(View.VISIBLE);
                        if(mPreview.isZoomSupported()){
                            FrameLayout zoom = (FrameLayout)findViewById(R.id.zoom_layout);
                            zoom.setVisibility(View.INVISIBLE);
                        }
                    }
                    else{
                        mPreview.stopShooting();
                        mMode = 0;
                        //���̃{�^����������悤�ɂ���
                        //mFocusButton.setVisibility(View.VISIBLE);
                        mGalleryButton.setVisibility(View.VISIBLE);
                        mSettingButton.setVisibility(View.VISIBLE);
                        mCount = 0;
                        mText.setText("0" + " ");
                        mText.setVisibility(View.INVISIBLE);
                        if(mPreview.isZoomSupported()){
                            FrameLayout zoom = (FrameLayout)findViewById(R.id.zoom_layout);
                            zoom.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
        
        mFocusButton = (ImageButton)findViewById(R.id.focusbtn);
        mFocusButton.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                if(mPreview != null){
                        mPreview.doAutoFocus();
                }
            }
        });
        
        mGalleryButton = (ImageButton)findViewById(R.id.gallery);
        mGalleryButton.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                if(mPreview != null){
                    startGallery();
                }
            }
        });
        
        mSettingButton = (ImageButton)findViewById(R.id.setting);
        mSettingButton.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                if(mPreview != null){
                    displaySettings();
                }
            }
        });
        
        //seekbar
        mSeekBar = (SeekBar)findViewById(R.id.zoom_seek);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

            public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                //Log.d(TAG, "progress = " + progress);
                if(mPreview != null){
                    mPreview.setZoom(progress);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
    
    /*
     * �v���r���[�T�C�Y�ݒ�
     * ��=4/5, ��=2/3(default), ��=1/3, ����=1*1
     */
    private void setPreviewSize(int idx){
        int hide_width = 0;
        int hide_height = 0;
        switch(idx){
            case 1:
                hide_width = mWidth * 4 / 5;
                hide_height = hide_width / 3 * 4;
                break;

            case 2:
                hide_width = mWidth * 2 / 3;
                hide_height = hide_width / 3 * 4;
                break;

            case 3:
                hide_width = mWidth / 3;
                hide_height = hide_width / 3 * 4;
                break;
                
            case 4:
                hide_height = 1;
                hide_width = 1;            
                break;                
        }
        
        //Log.d(TAG, "idx = " + idx);
        //Log.d(TAG, "width = " + hide_width + ",height = " + hide_height);

        mSurface.setLayoutParams(new FrameLayout.LayoutParams(hide_width, hide_height, Gravity.CENTER));
    }
    
    class MyOnTouchListener implements OnTouchListener{
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            //Log.d(TAG, "onTouch");

            //�w�̐��ɂ���ČĂ΂�郊�X�i��ύX
            int num = event.getPointerCount();
            if(num == 1){
                mGesture.onTouchEvent(event);
            }
            else if(num == 2){
                mScaleGesture.onTouchEvent(event);

                /*
                if(!mPinchFlag){
                    return true;
                }
                */
                /*
                Log.d(TAG, "width=" + view.getWidth() + ",height=" + view.getHeight());
                Log.d(TAG, "x1=" + event.getX(0) + ",y1=" + event.getY(0));
                Log.d(TAG, "x2=" + event.getX(1) + ",y2=" + event.getY(1));
                Log.d(TAG, "rawx=" + event.getRawX() + ",rawy=" + event.getRawY());
                */

                //�^�b�`�̍��W�ƌ��݂�View�̑傫�����獶��̍��W�����߂�(view.getX/getY��API���x��11����̂��ߑ��̕��@�ōs��)
                //float curWidth = view.getWidth();
                //float curHeight = view.getHeight();
                mSurfaceWidth = Math.abs(event.getX(1) - event.getX(0));
                mSurfaceHeight = Math.abs(event.getY(1) - event.getY(0));
                if(event.getX(1) - event.getX(0) >= 0){
                    if(event.getY(1) - event.getY(0) >= 0){
                        //getRaw������
                        mSurfaceX = event.getRawX();
                        mSurfaceY = event.getRawY();                        
                    }
                    else{
                        //getRaw������
                        mSurfaceX = event.getRawX();
                        mSurfaceY = event.getRawY() - mSurfaceHeight;
                    }
                }
                else{
                    if(event.getY(1) - event.getY(0) >= 0){
                        //getRaw���E��
                        mSurfaceX = event.getRawX() - mSurfaceWidth;
                        mSurfaceY = event.getRawY();
                    }
                    else{
                        //getRaw���E��
                        mSurfaceX = event.getRawX() - mSurfaceWidth;
                        mSurfaceY = event.getRawY() - mSurfaceHeight;                        
                    }
                }
                //Log.d(TAG, "curWidth=" + curWidth + ",curHeight=" + curHeight);
                //Log.d(TAG, "mSurfaceX=" + mSurfaceX + ",mSurfaceY=" + mSurfaceY);
                //Log.d(TAG, "mSurfaceWidth=" + mSurfaceWidth + ",mSurfaceHeight=" + mSurfaceHeight);
            }

            //return event.getPointerCount() == 1 ? mGesture.onTouchEvent(event) : mScaleGesture.onTouchEvent(event);
            return true;
        }
    }
    
    //�h���b�O�A���h�h���b�v�p
    GestureDetector mGesture = new GestureDetector(mContext, new OnGestureListener(){
        int offsetX;
        int offsetY;

        @Override
        public boolean onDown(MotionEvent e) {
            offsetX = (int) e.getRawX();
            offsetY = (int) e.getRawY();
            //Log.d(TAG, "offset = " + offsetX + "," + offsetY);
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            //�s���`���I��������onScroll��1���΂�
            if(mScaleEndFlag){
                mScaleEndFlag = false;
                return true;
            }

            int x = (int) e2.getRawX();
            int y = (int) e2.getRawY();
            int diffX = offsetX - x;
            int diffY = offsetY - y;
            
            /*
            Log.d(TAG, "offset2 = " + offsetX + "," + offsetY);
            Log.d(TAG, "cur1 = " + x + "," + y);
            Log.d(TAG, "cur2 = " + (int)e1.getRawX() + "," + (int)e1.getRawY());
            Log.d(TAG, "diff = " + diffX + "," + diffY);
            Log.d(TAG, "size = " + mSurface.getWidth() + "," + mSurface.getHeight());
            */
            
            //SurfaceView�̍���̍��W�����߂�
            int curX = (int)(x - e2.getX());
            int curY = (int)(y - e2.getY());

            curX -= diffX;
            curY -= diffY;
            mSurface.layout(curX, curY,
                    curX + mSurface.getWidth(),
                    curY + mSurface.getHeight());
            //Log.d(TAG, "layout = " + curX + "," + curY + "," + mSurface.getWidth() + "," + mSurface.getHeight());            

            offsetX = x;
            offsetY = y;
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return false;
        }

    });

    class MySimpleOnScaleGestureListener extends SimpleOnScaleGestureListener {   
        @Override  
        public boolean onScale(ScaleGestureDetector detector) {   
            //Log.d(TAG, "onScale");
            // 2�̎w�̋������g���ėV�т���������i���̋����ȉ��Ȃ�s���`�Ƃ݂Ȃ��Ȃ��j
            /*
            float spanCurr = detector.getCurrentSpan();
            if (Math.abs(spanCurr - mSpanPrev) < 30) {   
                return false;   
            }
            */
            
            //mPinchFlag = true;

            mSurface.layout(
                    (int)mSurfaceX,
                    (int)mSurfaceY,
                    (int)mSurfaceX + (int)mSurfaceWidth,
                    (int)mSurfaceY + (int)mSurfaceHeight);
            //Log.d(TAG, "layoutScale = " + mSurfaceX + "," + mSurfaceY + "," + mSurfaceX+mSurfaceWidth + "," + mSurfaceY+mSurfaceHeight);            

            //mSpanPrev = spanCurr; 
            return super.onScale(detector);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            //Log.d(TAG, "onScaleBegin");
            isPinch = true;
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isPinch = false;
            /*
             * �s���`���I��������Ƃ������B
             * ���̌��GestureDetector��onScroll���Ă΂�鎞������A
             * ���C�A�E�g������Ă��܂����ۂ��������邽�߁A���̃t���O�𗧂āA
             * �����onScroll�ł̓��C�A�E�g��ύX���Ȃ��悤�ɂ���B
             */
            mScaleEndFlag = true;
            //Log.d(TAG, "onScaleEnd");
        }
    }
    
    public void onStart(){
    	//Log.d(TAG, "enter ContShooting#onStart");
    	
        super.onStart();
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            new AlertDialog.Builder(this)
            .setTitle(R.string.sc_alert_title)
            .setMessage(getString(R.string.sc_alert_sd))
            .setPositiveButton(R.string.sc_alert_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(RESULT_OK);
                }
            })
            .show();
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        //�I�v�V�������j���[(�M�������[)
        MenuItem prefGallery = menu.add(0, MENU_DISP_GALLERY, 0, R.string.sc_menu_gallery);
        prefGallery.setIcon(android.R.drawable.ic_menu_gallery);

        //�I�v�V�������j���[(�ݒ�)
        MenuItem prefSetting = menu.add(0, MENU_DISP_SETTING, 0, R.string.sc_menu_setting);
        prefSetting.setIcon(android.R.drawable.ic_menu_preferences);

        return true;
    }
    
    //�I�v�V�������j���[�I�����̃��X�i
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DISP_GALLERY:
        	startGallery();
            break;
            
        case MENU_DISP_SETTING:
            displaySettings();
        	break;
            
        default:
            //�������Ȃ�
        }

        return true;
    }
    
    private void startGallery(){
        new AlertDialog.Builder(FloatingCameraActivity.this)
        .setTitle(R.string.sc_alert_title)
        .setMessage(mContext.getString(R.string.sc_alert_gallery))
        .setPositiveButton(R.string.sc_alert_gallery_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //�����Iintent���ƃJ������������ꍇ�����������߁A�Ö�intent�ɕύX
                Intent intent = new Intent();
                intent.setType("image/*");
                //intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setAction(Intent.ACTION_PICK);
                startActivity(intent);
            }
        })
        .show();

        /*
    	// �M�������[�\��
    	Intent intent = null;
    	try{
    	    // for Honeycomb
    	    intent = new Intent();
    	    intent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.Gallery");
    	    startActivity(intent);
    	    return;
    	}
    	catch(Exception e){
    	    try{
    	        // for Recent device
    	        intent = new Intent();
    	        intent.setClassName("com.cooliris.media", "com.cooliris.media.Gallery");
    	        startActivity(intent);
    	    }
    	    catch(ActivityNotFoundException e1){
    	        try
    	        {
    	            // for Other device except HTC
    	            intent = new Intent(Intent.ACTION_VIEW);
    	            intent.setData(Uri.parse("content://media/external/images/media"));
    	            startActivity(intent);
    	        }
    	        catch (ActivityNotFoundException e2){
    	        	try{
    	        		// for HTC
    	        		intent = new Intent();
    	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumTabSwitchActivity");
    	        		startActivity(intent);
    	        	}
    	        	catch(ActivityNotFoundException e3){
        	        	try{
        	        		// for HTC
        	        		intent = new Intent();
        	        		intent.setClassName("com.htc.album", "com.htc.album.AlbumMain.ActivityMainDropList");
        	        		startActivity(intent);
        	        	}
        	        	catch(ActivityNotFoundException e4){
        	            	Toast.makeText(this, R.string.sc_menu_gallery_ng, Toast.LENGTH_SHORT).show();
        	        	}
    	        	}
    	        }
    	    }
    	}
    	*/
    }
    
    private void displaySettings(){
        Intent pref_intent = new Intent(this, FloatingCameraPreference.class);

        //�F�����ݒ�̃��X�g���쐬����
        List<String> effectList = null;
        if(mPreview != null){
            effectList = mPreview.getEffectList();
        }
        if(effectList != null){
            pref_intent.putExtra("effect", (String[])effectList.toArray(new String[0]));
        }

        //�V�[��
        List<String> sceneList = null;
        if(mPreview != null){
            sceneList = mPreview.getSceneModeList();
        }
        if(sceneList != null){
            pref_intent.putExtra("scene", (String[])sceneList.toArray(new String[0]));
        }

        //�z���C�g�o�����X
        List<String> whiteList = null;
        if(mPreview != null){
            whiteList = mPreview.getWhiteBalanceList();
        }
        if(whiteList != null){
            pref_intent.putExtra("white", (String[])whiteList.toArray(new String[0]));
        }
        
        //�摜�T�C�Y
        List<String> sizeList = null;
        if(mPreview != null){
            sizeList = mPreview.getSizeList();
        }
        if(sizeList != null){
            pref_intent.putExtra("size", (String[])sizeList.toArray(new String[0]));
        }
        
        startActivityForResult(pref_intent, REQUEST_CODE);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(data == null){
            return;
        }
        
        if(requestCode == REQUEST_CODE){
            if(resultCode == RESPONSE_COLOR_EFFECT){
                if(mPreview != null){
                    mPreview.setColorValue(data.getStringExtra("effect"));
                }            	
            }
            if(resultCode == RESPONSE_SCENE_MODE){
            	if(mPreview != null){
                    mPreview.setSceneValue(data.getStringExtra("scene"));
                }            	            	
            }
            if(resultCode == RESPONSE_WHITE_BALANCE){
            	if(mPreview != null){
                    mPreview.setWhiteValue(data.getStringExtra("white"));
                }            	
            }
            if(resultCode == RESPONSE_PICTURE_SIZE){
                if(mPreview != null){
                    mPreview.setSizeValue(data.getIntExtra("size", 0));
                }
            }
            if(resultCode == RESPONSE_SHOOT_NUM){
                if(mPreview != null){
                    mPreview.setShootNum(data.getIntExtra("shoot", 0));
                }
            }
            if(resultCode == RESPONSE_INTERVAL){
                if(mPreview != null){
                    mPreview.setInterval(data.getIntExtra("interval", 0));
                }
            }
            if(resultCode == RESPONSE_PREVIEW_SIZE){
                int idx = data.getIntExtra("preview_size", 0);
                setPreviewSize(idx);
            }
        }
    }
    
    public void count(){
    	mText.setText(Integer.toString(++mCount) + " ");
    }
    
    public void displayStart(){
    	mButton.setImageResource(R.drawable.start);
    }
    
    public void displayStop(){
        mButton.setImageResource(R.drawable.stop);
    }
    
    void invisibleZoom(){
        FrameLayout zoom = (FrameLayout)findViewById(R.id.zoom_layout);
        zoom.setVisibility(View.INVISIBLE);
    }
    
    public void saveGallery(ContentValues values){
		mResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
    }
    
    public void setMode(int mode){
        mMode = mode;
    }
    
    protected void onPause(){
        //Log.d(TAG, "enter ContShooting#onPause");
    	super.onPause();
    	
        mCount = 0;
        mText.setText("0" + " ");
        mText.setVisibility(View.INVISIBLE);
        mFocusButton.setVisibility(View.VISIBLE);
        mGalleryButton.setVisibility(View.VISIBLE);
        mSettingButton.setVisibility(View.VISIBLE);
        if(mPreview.isZoomSupported()){
            FrameLayout zoom = (FrameLayout)findViewById(R.id.zoom_layout);
            zoom.setVisibility(View.VISIBLE);
        }
        
    	if(mSleepFlag){
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mSleepFlag = false;
    	}
    	
        if(mPreview != null){
            mPreview.release();
        }
        
        //�A�v���̃L���b�V���폜
        //deleteCache(getCacheDir());
    }
    
    protected void onResume(){
        super.onResume();
        
        //Log.d(TAG, "onResume");
                
        if(FloatingCameraPreference.isSleepMode(this)){
            if(!mSleepFlag){
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mSleepFlag = true;                
            }
        }
        else{
            if(mSleepFlag){
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mSleepFlag = false;                
            }
        }
    }
    
    protected void onDestroy(){
    	super.onDestroy();
    }
    
    protected void onRestart(){
    	super.onRestart();
    }
    
    public void finish(){
		System.exit(RESULT_OK);
    }
    
    public static boolean deleteCache(File dir) {
        if(dir==null) {
            return false;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteCache(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}