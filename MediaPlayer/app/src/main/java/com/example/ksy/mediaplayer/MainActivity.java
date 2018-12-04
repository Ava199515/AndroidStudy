package com.example.ksy.mediaplayer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    //private MediaPlayer mediaPlayer;
    private TextView startTimeText,endTimeText,nameText,authorText;
    private ImageView playImage,stopImage,backImage,fileImage;
    private CircleImageView circleImageView;
    private SeekBar seekBar;
    private MusicService musicService=new MusicService();
    private final int RESULT_LOAD_AUDIO = 1;
    private ObjectAnimator objectAnimator;
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    private IBinder musicBinder;
    private Parcel data,reply;
    private Observable<Integer> observable;
    private Observer<Integer>observer;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // musicService = ((MusicService.MyBinder)service).getService();
            Log.d("MainActivity","Connect Success");
            musicBinder = service;
            init();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //musicService = null;
            musicBinder = null;
            Log.d("MainActivity","Connect Fail");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTimeText = (TextView)findViewById(R.id.start_time);
        endTimeText = (TextView)findViewById(R.id.end_time);
        nameText = (TextView)findViewById(R.id.name);
        authorText = (TextView)findViewById(R.id.author);
        playImage = (ImageView)findViewById(R.id.play);
        stopImage = (ImageView)findViewById(R.id.stop);
        backImage = (ImageView)findViewById(R.id.back);
        fileImage = (ImageView)findViewById(R.id.file);
        circleImageView = (CircleImageView)findViewById(R.id.image) ;
        seekBar = (SeekBar)findViewById(R.id.seek_bar);
        data = Parcel.obtain();
        reply = Parcel.obtain();

        //TODO：用ObjectAnimator实现专辑图片的旋转.
        objectAnimator = ObjectAnimator.ofFloat(circleImageView,"rotation",0,360);
        objectAnimator.setDuration(5000);   //设置旋转一周的时间
        objectAnimator.setInterpolator(new LinearInterpolator());  //匀速旋转
        objectAnimator.setRepeatCount(Animation.INFINITE);         //设置重复旋转的次数
        objectAnimator.setRepeatMode(ValueAnimator.RESTART);       //设置重复模式
        objectAnimator.setCurrentPlayTime(MusicService.mediaPlayer.getCurrentPosition());//设置当前播放的时间
        if(MusicService.mediaPlayer.isPlaying()){
            playImage.setImageResource(R.drawable.pause);
            objectAnimator.start();
        }
        //TODO:ServiceConnection实例，用于连接主活动和服务。

        Intent bindIntent = new Intent(this,MusicService.class);
        startService(bindIntent);
        bindService(bindIntent,connection,BIND_AUTO_CREATE);

        observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> e)throws Exception{
                while(true){
                    e.onNext(MusicService.mediaPlayer.getCurrentPosition());
                    Thread.sleep(800);
                }
            }
        });

        observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
            }
            @Override
            public void onNext(Integer integer) {
                seekBar.setProgress(integer);
                startTimeText.setText(time.format(integer));
            }

            @Override
            public void onError(Throwable e) {
                Log.e("MainActivity", "onError : value : " + e.getMessage() + "\n" );
            }

            @Override
            public void onComplete() {
                Log.d("MainActivity", "OnComplete" );
            }
        };
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);

        //TODO:申请读取文件的权限
        try{
            if(ContextCompat. checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            }
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"There is no file",Toast.LENGTH_SHORT).show();
        }

        //TODO:播放或暂停按钮的监听事件
        playImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //musicService.playOrPause();
                int isPlayOrPause=0;
                try{
                    data.writeInterfaceToken("MusicService");
                    musicBinder.transact(MusicService.PLAYORPAUSE,data,reply,0);
                    isPlayOrPause = reply.readInt();
                }catch(RemoteException e){
                    e.printStackTrace();
                }finally {
                    data.recycle();
                    reply.recycle();
                }

                if(isPlayOrPause == 1){
                    if(!objectAnimator.isStarted()){
                        objectAnimator.start();
                    }else{
                        objectAnimator.resume();
                    }
                    playImage.setImageResource(R.drawable.pause);
                }else{
                    playImage.setImageResource(R.drawable.play);
                    objectAnimator.pause();
                }
            }
        });

        //TODO：停止播放并重置UI界面相关信息
        stopImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // musicService.stop();
                try{
                    data.writeInterfaceToken("MusicService");
                    musicBinder.transact(MusicService.STOP,data,reply,0);
                }catch(RemoteException e){
                    e.printStackTrace();
                }finally {
                    data.recycle();
                    reply.recycle();
                }
                objectAnimator.end();
                playImage.setImageResource(R.drawable.play);
                init();
            }
        });

        //TODO：退出按钮，终止服务并结束进程
        backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //musicService.stop();
                try{
                    data.writeInterfaceToken("MusicService");
                    musicBinder.transact(MusicService.STOP,data,reply,0);
                }catch(RemoteException e){
                    e.printStackTrace();
                }finally {
                    data.recycle();
                    reply.recycle();
                }
                Intent bindIntent = new Intent(MainActivity.this,MusicService.class);
                stopService(bindIntent);
                //unbindService(connection);
                finish();
                System.exit(0);
            }
        });

        //TODO：选择播放音频文件，要先申请权限
        fileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},RESULT_LOAD_AUDIO);
                }
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i,RESULT_LOAD_AUDIO);
                playImage.setImageResource(R.drawable.play);
            }
        });

        //TODO:设置SeekBar的进度条变化监听函数
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    //musicService.seekTo(progress);
                    try{
                        data.writeInterfaceToken("MusicService");
                        data.writeInt(progress);
                        musicBinder.transact(MusicService.SEEKTO,data,reply,0);
                    }catch(RemoteException e){
                        e.printStackTrace();
                    }finally {
                        data.recycle();
                        reply.recycle();
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        MusicService.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playImage.setImageResource(R.drawable.play);
                objectAnimator.pause();
                seekBar.setProgress(MusicService.mediaPlayer.getCurrentPosition());
                startTimeText.setText(String.valueOf(time.format(MusicService.mediaPlayer.getCurrentPosition())));
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent dataIntent){
        super.onActivityResult(requestCode,resultCode,dataIntent);

        if(requestCode == RESULT_LOAD_AUDIO && resultCode == RESULT_OK && dataIntent != null){
            Uri selectAudioUri = dataIntent.getData();
            String[] filePathColumn = {MediaStore.Audio.Media.DATA};
            Cursor cursor = getContentResolver().query(selectAudioUri,filePathColumn,null,null,null);
            cursor.moveToLast();
            String audioPath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            cursor.close();

            //musicService.setPlaySource(audioPath);
            try{
                data.writeInterfaceToken("MusicService");
                data.writeString(audioPath);
                musicBinder.transact(MusicService.SETMEDIAPATH,data,reply,0);
            }catch(RemoteException e){
                e.printStackTrace();
            }finally {
                data.recycle();
                reply.recycle();
            }
            objectAnimator.end();
            init();
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(connection);
    }

    //TODO: 根据服务里的MeidaPlayer对象内容初始化活动的界面
    public void init(){
        String mediaPath=null;
        try{
            data.writeInterfaceToken("MusicService");
            musicBinder.transact(MusicService.GETMEDIAPATH,data,reply,0);
            mediaPath = reply.readString();
        }catch(RemoteException e){
            e.printStackTrace();
        }finally {
            data.recycle();
            reply.recycle();
        }

        seekBar.setMax(MusicService.mediaPlayer.getDuration());
        seekBar.setProgress(MusicService.mediaPlayer.getCurrentPosition());
        startTimeText.setText(time.format(MusicService.mediaPlayer.getCurrentPosition()));
        endTimeText.setText(time.format(MusicService.mediaPlayer.getDuration()));

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(mediaPath);
        nameText.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));  //歌名
        authorText.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)); //歌手名字
        Log.d("MainActivity","ALBUM:"+mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        byte[] pictureData = mmr.getEmbeddedPicture();
        if(pictureData != null){
            Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData,0,pictureData.length);
            circleImageView.setImageBitmap(bitmap);
        }else{
            circleImageView.setImageResource(R.drawable.img);
        }

        mmr.release();
    }

    //TODO:开启一个线程，当播放音乐时更新进度条的进度，由于要涉及到UI的修改，所以用到Handler
   /* class SeekBarThread implements Runnable{
        @Override
        public void run(){
            while(musicService!=null){
                    Message message = new Message();
                    message.what = 0;
                    message.obj = MusicService.mediaPlayer.getCurrentPosition();
                    handler.sendMessage(message);
                try{
                    Thread.sleep(800);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }*/

    //TODO：Handler实例，对UI进行修改
   /* @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    seekBar.setProgress((int)msg.obj);
                    startTimeText.setText(time.format(msg.obj));
                    break;
                default:break;
            }
        }
    };*/
}
