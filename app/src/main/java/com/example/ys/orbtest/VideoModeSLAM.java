package com.example.ys.orbtest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class VideoModeSLAM extends Activity {
    Button startVideoSlam;
    VideoView mVideoView;
    int REQUEST_EXTERNAL_STORAGE = 100;
    String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private GLSurfaceView glSurfaceView;
    private static final String TAG = "VideoModeSLAM";
    private static final String VideoSource = "/sdcard/Download/SLAM/video.mp4";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    private native float[] CVTest(long matAddr);  //调用 c++代码

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //hide the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.video_mode_slam);

        checkSdReadPermission(new IcheckPermissionListener() {
            @Override
            public void hasReadPermission() {

                startVideoSlam = (Button)findViewById(R.id.start_slam);
                mVideoView = (VideoView)findViewById(R.id.video_view);
                mVideoView.setVideoPath(VideoSource);
                mVideoView.start();
                //opengl图层
                glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
                //OpenGL ES 2.0
                glSurfaceView.setEGLContextClientVersion(2);
                //设置透明背景
                glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
                glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
                final MyRender earthRender = new MyRender(VideoModeSLAM.this);
                glSurfaceView.setRenderer(earthRender);
                // 设置渲染模式为主动渲染
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                glSurfaceView.setZOrderOnTop(true);
                glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event != null) {
                            // Convert touch coordinates into normalized device
                            // coordinates, keeping in mind that Android's Y
                            // coordinates are inverted.
                            final float normalizedX = ((event.getX() / (float) v.getWidth()) * 2 - 1) * 4f;
                            final float normalizedY = (-((event.getY() / (float) v.getHeight()) * 2 - 1)) * 1.5f;

                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                glSurfaceView.queueEvent(new Runnable() {
                                    @Override
                                    public void run() {
                                        earthRender.handleTouchPress(
                                                normalizedX, normalizedY);
                                    }
                                });
                            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                                glSurfaceView.queueEvent(new Runnable() {
                                    @Override
                                    public void run() {
                                        earthRender.handleTouchDrag(
                                                normalizedX, normalizedY);
                                    }
                                });
                            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                                glSurfaceView.queueEvent(new Runnable() {
                                    @Override
                                    public void run() {
                                        earthRender.handleTouchUp(
                                                normalizedX, normalizedY);
                                    }
                                });
                            }

                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
                mmr.setDataSource(VideoSource);
                //mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM);
                //mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST);
                Log.d(TAG,"总时长："+mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION));
                //Bitmap b = mmr.getFrameAtTime(2000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST); // frame at 2 seconds
                //byte [] artwork = mmr.getEmbeddedPicture();
                mmr.release();
            }
        });


    }
    @Override
    public void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }


//
//    /**
//     * 处理图像的函数，这个函数在相机刷新每一帧都会调用一次，而且每次的输入参数就是当前相机视图信息
//     * * @param inputFrame
//     * @return
//     */
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Mat rgb = inputFrame.rgba();
//        float[] poseMatrix = CVTest(rgb.getNativeObjAddr()); //从slam系统获得相机位姿矩阵
//
//        if (poseMatrix.length != 0) {
//            double[][] pose = new double[4][4];
//            System.out.println("one posematrix is below========");
//            for (int i = 0; i < poseMatrix.length / 4; i++) {
//                for (int j = 0; j < 4; j++) {
//
//                    if (j == 3 && i != 3) {
//                        pose[i][j] = poseMatrix[i * 4 + j] * SCALE;
//                    } else {
//                        pose[i][j] = poseMatrix[i * 4 + j];
//                    }
//                    System.out.print(pose[i][j] + "\t ");
//                }
//
//                System.out.print("\n");
//            }
//
//            System.out.println("总共第" + count + "帧，缩放因子为==============" + SCALE);
//            double[][] R = new double[3][3];//提取旋转矩阵
//            double[] T = new double[3];//提取平移矩阵
//
//            for (int i = 0; i < 3; i++) {
//                for (int j = 0; j < 3; j++) {
//                    R[i][j] = pose[i][j];
//                }
//            }
//            for (int i = 0; i < 3; i++) {
//                T[i] = pose[i][3];
//            }
//            RealMatrix rotation = new Array2DRowRealMatrix(R);
//            RealMatrix translation = new Array2DRowRealMatrix(T);
//            MatrixState.set_model_view_matrix(rotation, translation);
//
//            MyRender.flag = true;
//            count++;
//
//        } else {
//            //如果没有得到相机的位姿矩阵，就不画立方体
//            MyRender.flag = false;
//        }
//
////      CVTest(rgb.getNativeObjAddr());
//        return rgb;
//    }



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        glSurfaceView.onResume();
    }
    /**
     * 用于测试java读取文件权限的函数
     **/
    void checkSdReadPermission(@NonNull  IcheckPermissionListener listener)  {
        int permission = ContextCompat.checkSelfPermission(VideoModeSLAM.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    VideoModeSLAM.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }else {
            listener.hasReadPermission();
        }
    }
    interface IcheckPermissionListener {
        void hasReadPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE){
            finish();
        }
    }
}
