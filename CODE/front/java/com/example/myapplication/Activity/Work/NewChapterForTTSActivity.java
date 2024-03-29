package com.example.myapplication.Activity.Work;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;

import com.example.myapplication.AudioUtils.AudioUtils;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.myapplication.InternetUtils.GetServer;
import com.example.myapplication.InternetUtils.HttpUtils;
import com.example.myapplication.AudioUtils.MilliToHMS;
import com.example.myapplication.MyComponent.MyToast;

import com.example.myapplication.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NewChapterForTTSActivity extends AppCompatActivity {

    private LinearLayout normal;
    private LinearLayout loadView;
    private SeekBar seekBar;//进度条
    private EditText content;

    private int bookid;
    private String chapterTitle;
    private String bgmPath;
    private String speechPath;//转化后的音频在数据库中的存储路径
    private boolean isInNight = false;//是否处于夜间模式
    private boolean hasPlayerReset = false;
    //private final int WRITE_EXTERNAL_CODE = 1;


    private boolean getSpeechDone = false;//是否完成语音转换
    private boolean matchBgmDone = false;//是否完成BGM的匹配
    private File speechFile = null;
    private File bgm = null;
    private String MP3_LOCATION;
    private String BGM_LOCATION;

    private MediaPlayer speech_player;//音频播放
    private MediaPlayer bgm_player;//bgm播放

    private boolean firtstPlay = true;//是否首次播放当前音频

    private boolean textChanged = false;//同步音频与文本

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserState",MODE_PRIVATE);
        isInNight = sharedPreferences.getBoolean("night",false);//是否处于夜间模式


        super.onCreate(savedInstanceState);
        if(isInNight){
            setContentView(R.layout.activity_new_chapter_for_tts_night);
        }else {
            setContentView(R.layout.activity_new_chapter_for_tts);
        }

        MP3_LOCATION = this.getCacheDir().getAbsolutePath()+"/temp.mp3";
        BGM_LOCATION = this.getCacheDir().getAbsolutePath()+"/bgm.mp3";

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        normal = findViewById(R.id.normal);
        loadView = findViewById(R.id.Loading);

        content = findViewById(R.id.content);
        content.addTextChangedListener(watcher);
        content.setMovementMethod(ScrollingMovementMethod.getInstance());

        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(speechFile != null){
                    speech_player.seekTo(seekBar.getProgress());
                    bgm_player.seekTo(seekBar.getProgress());

                    NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView begin = findViewById(R.id.begin);
                            MilliToHMS milliToHMS = new MilliToHMS();
                            begin.setText(milliToHMS.milliToHMS(speech_player.getCurrentPosition()));
                        }
                    });
                }
            }
        });//实现拖动进度条，调整播放进度

        normal.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        bookid = intent.getIntExtra("bookid",-1);

        speech_player = new MediaPlayer();
        bgm_player = new MediaPlayer();

        speech_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                resetPlayer();

                if(NewChapterForTTSActivity.this.isFinishing()) return;
                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView playButton = findViewById(R.id.PlayButton);
                        playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.play));
                    }
                });
            }
        });

        refresh();
    }

    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            textChanged = true;//文本修改了，音频尚未修改
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void releasePlayer(){
        if(speech_player != null) {
            speech_player.reset();
            speech_player.release();
            speech_player = null;
        }
        if(bgm_player != null) {
            bgm_player.reset();
            bgm_player.release();
            bgm_player = null;
        }
    }

    @Override
    public void onBackPressed(){

        //保存草稿
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存草稿");
        builder.setMessage("是否保存为草稿?");

        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                try{
                    //保存草稿数据至后端
                    new Thread(storeDraft).start();
                    new Thread(deleteSpeech).start();

                    hasPlayerReset = true;
                    releasePlayer();

                    if(speechFile != null && speechFile.exists()) speechFile.delete();
                    if(bgm != null && bgm.exists()) bgm.delete();

                    NewChapterForTTSActivity.super.finish();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        builder.setNegativeButton("不保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                new Thread(deleteDraft).start();
                new Thread(deleteSpeech).start();

                hasPlayerReset = true;
                releasePlayer();
                if(speechFile != null && speechFile.exists()) speechFile.delete();
                if(bgm != null && bgm.exists()) bgm.delete();

                NewChapterForTTSActivity.super.finish();
            }
        });

        builder.show();
    }

    public void onBackPressed(View view){
        onBackPressed();
    }

    public void refresh(View view){
        refresh();
    }

    public void refresh(){
        loadView.setClickable(false);

        CountDownTimer countDownTimer = new CountDownTimer(5000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                loadView.setClickable(true);
            }
        };//防止用户高频率点击
        countDownTimer.start();

        loadView.setVisibility(View.VISIBLE);//加载画面
        findViewById(R.id.loadinggif).setVisibility(View.VISIBLE);
        findViewById(R.id.Remind).setVisibility(View.INVISIBLE);

        new Thread(getDraft).start();
    }

    //保存章节
    public void storeChapter(View view){
        if(textChanged){
            new MyToast(NewChapterForTTSActivity.this,getResources().getString(R.string.askpush));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final View v = LayoutInflater.from(this).inflate(R.layout.dialog_layout,null);
        TextView titleView = v.findViewById(R.id.Title);
        titleView.setText("标题");

        builder.setView(v);


        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText title = v.findViewById(R.id.Edit);
                chapterTitle = title.getText().toString();

                if(chapterTitle.length() > 20){//标题不能超过20个字
                    new MyToast(NewChapterForTTSActivity.this,getResources().getString(R.string.titlelong));
                    return;
                }

                StoreChapter storeChapter = new StoreChapter(chapterTitle,bookid,content.getText().toString(),speechPath,bgmPath);
                storeChapter.start();

                releasePlayer();

                if(speechFile != null && speechFile.exists()) speechFile.delete();
                if(bgm != null && bgm.exists()) bgm.delete();
                NewChapterForTTSActivity.super.finish();

            }
        });

        builder.setNegativeButton("取消", null);

        builder.show();
    }

    public void textToSpeech(View view){
        new Thread(textToSpeech).start();
    }

    private void resetPlayer(){
        hasPlayerReset = true;

        if(speech_player != null){
            speech_player.reset();
        }

        if(bgm_player != null){
            bgm_player.reset();
        }

        firtstPlay = true;

        if(NewChapterForTTSActivity.this.isFinishing()) return;
        NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView begin = findViewById(R.id.begin);
                begin.setText(getResources().getString(R.string.initial));
                seekBar.setProgress(0);
                ImageView playButton = findViewById(R.id.PlayButton);
                playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.play));
            }
        });
    }

    public void playSpeech(View view){

        if(firtstPlay)  {
            new Thread(prepareSpeech).start();
        }

        else {
            new Thread(controlSpeech).start();
        }
    }


    Runnable controlSpeech = new Runnable() {
        @Override
        public void run() {
            if(speech_player.isPlaying()) {
                speech_player.pause();
                bgm_player.pause();
                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView playButton = findViewById(R.id.PlayButton);
                        playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.play));
                    }
                });
            }
            else {
                speech_player.start();
                bgm_player.start();
                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView playButton = findViewById(R.id.PlayButton);
                        playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.pause));
                    }
                });
            }
        }
    };

    //试听音频
    Runnable prepareSpeech = new Runnable() {
        @Override
        public void run() {
            try {
                //没有音频或音频尚未转换成功
                if(speechFile == null){
                    new MyToast(NewChapterForTTSActivity.this,"语音文件不存在!");
                    return;
                }
                //首次播放设置数据源
                    seekBar.setProgress(0);

                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();//数据源类型

                    speech_player.setDataSource(MP3_LOCATION);
                    speech_player.setAudioAttributes(audioAttributes);

                    bgm_player.setDataSource(BGM_LOCATION);
                    bgm_player.setAudioAttributes(audioAttributes);

                    bgm_player.prepare();
                    speech_player.prepareAsync();//异步准备音源

                    speech_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {

                            seekBar.setMax(speech_player.getDuration());


                            speech_player.start();
                            bgm_player.start();

                            bgm_player.setVolume(0.2f,0.2f);//设置背景音乐音量
                            bgm_player.setLooping(true);//背景音乐循环播放

                            hasPlayerReset = false;
                            //进度条更新
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while (!Thread.currentThread().isInterrupted()){
                                        try {
                                            if(NewChapterForTTSActivity.this.isFinishing()) break;
                                            if(speech_player == null || hasPlayerReset) break;
                                            seekBar.setProgress(speech_player.getCurrentPosition());
                                            NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                 TextView begin = findViewById(R.id.begin);
                                                 MilliToHMS milliToHMS = new MilliToHMS();
                                                 if(speech_player == null || hasPlayerReset) return;
                                                 begin.setText(milliToHMS.milliToHMS(speech_player.getCurrentPosition()));
                                                }
                                            });
                                            Thread.sleep(200);
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }).start();

                            NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    firtstPlay = false;
                                    ImageView playButton = findViewById(R.id.PlayButton);
                                    playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pause));
                                }
                            });
                        }
                    });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    /*
    private void requestPermission(){
        if (ContextCompat.checkSelfPermission(NewChapterForTTSActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(NewChapterForTTSActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_CODE);
        }
        new Thread(textToSpeech).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String []permissions, int[] grantResults){
        if (requestCode == WRITE_EXTERNAL_CODE){
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    new Thread(textToSpeech).start();
                }
        }
    }*/

    Runnable textToSpeech = new Runnable() {
        @Override
        public void run() {
            try{

                //重置播放状态
                speechFile = null;
                resetPlayer();
                textChanged = false;

                if(NewChapterForTTSActivity.this.isFinishing()) return;
                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        seekBar.setProgress(0);

                        TextView begin = findViewById(R.id.begin);
                        begin.setText(getResources().getString(R.string.initial));

                        TextView end = findViewById(R.id.end);
                        end.setText(getResources().getString(R.string.initial));

                        LinearLayout translating = findViewById(R.id.translating);
                        translating.setVisibility(View.VISIBLE);

                        normal.setVisibility(View.INVISIBLE);
                    }
                });

                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/textToSpeech";

                JSONObject params = new JSONObject();
                params.put("text",content.getText().toString());

                byte[] param = params.toString().getBytes();

                HttpUtils httpUtils = new HttpUtils(url);
                final ByteArrayOutputStream resultStream = httpUtils.doHttp(param, "POST", "application/json");//向后端发送请求

                if (resultStream == null) {//请求超时
                    if(NewChapterForTTSActivity.this.isFinishing()) return;
                    NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(NewChapterForTTSActivity.this, getResources().getString(R.string.HttpTimeOut));

                            LinearLayout translating = findViewById(R.id.translating);
                            translating.setVisibility(View.INVISIBLE);

                            normal.setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                speechPath = new String(resultStream.toByteArray(),
                        StandardCharsets.UTF_8);

                firtstPlay = true;

                new Thread(getSpeech).start();
                new Thread(matchBGM).start();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable getSpeech = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/getSpeech?path=" + speechPath;

                HttpUtils httpUtils = new HttpUtils(url);
                final ByteArrayOutputStream resultStream = httpUtils.doHttp(null, "GET", "application/json");//向后端发送请求

                if (resultStream == null) {//请求超时
                    if(NewChapterForTTSActivity.this.isFinishing()) return;
                    NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(NewChapterForTTSActivity.this, getResources().getString(R.string.HttpTimeOut));

                            LinearLayout translating = findViewById(R.id.translating);
                            translating.setVisibility(View.INVISIBLE);

                            normal.setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                speechFile = new File(MP3_LOCATION);//speechFile保存后端语音
                if (!speechFile.exists()) speechFile.createNewFile();
                OutputStream outputStream = new FileOutputStream(speechFile);
                resultStream.writeTo(outputStream);
                outputStream.close();


                getSpeechDone = true;

                if(matchBgmDone){
                    getSpeechDone = false;
                    matchBgmDone = false;

                    if(NewChapterForTTSActivity.this.isFinishing()) return;
                    NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(NewChapterForTTSActivity.this, getResources().getString(R.string.translateSuccess));
                            LinearLayout translating = findViewById(R.id.translating);
                            translating.setVisibility(View.INVISIBLE);

                            normal.setVisibility(View.VISIBLE);

                            AudioUtils audioUtils = new AudioUtils();
                            MilliToHMS milliToHMS = new MilliToHMS();
                            TextView end = findViewById(R.id.end);
                            end.setText(milliToHMS.milliToHMS(audioUtils.getLength(MP3_LOCATION)));
                        }
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable deleteSpeech = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/deleteSpeech?path=" + speechPath;

                HttpUtils httpUtils = new HttpUtils(url);
                httpUtils.doHttp(null, "GET", "application/json");//向后端发送请求

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable matchBGM = new Runnable() {
        @Override
        public void run() {
            try{

                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/matchBGMByText";

                JSONObject params = new JSONObject();
                params.put("text",content.getText().toString());

                HttpUtils httpUtils = new HttpUtils(url);
                final ByteArrayOutputStream resultStream = httpUtils.doHttp(params.toString().getBytes(), "POST", "application/json");//向后端发送请求

                if (resultStream == null) {//请求超时
                    if(NewChapterForTTSActivity.this.isFinishing()) return;
                    NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(NewChapterForTTSActivity.this, getResources().getString(R.string.HttpTimeOut));

                            LinearLayout translating = findViewById(R.id.translating);
                            translating.setVisibility(View.INVISIBLE);

                            normal.setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                final String result = new String(resultStream.toByteArray(),
                        StandardCharsets.UTF_8);

                final JSONObject path = new JSONObject(result);

                bgmPath = path.getString("bgmPath");
                new Thread(getBgm).start();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable getBgm = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/getBGM?filename=" + URLEncoder.encode(bgmPath,"UTF-8");

                HttpUtils httpUtils = new HttpUtils(url);
                final ByteArrayOutputStream resultStream = httpUtils.doHttp(null, "GET", "application/json");//向后端发送请求

                bgm = new File(BGM_LOCATION);
                if (!bgm.exists()) bgm.createNewFile();
                OutputStream outputStream = new FileOutputStream(bgm);
                resultStream.writeTo(outputStream);
                outputStream.close();

                matchBgmDone = true;

                if(getSpeechDone) {
                    matchBgmDone = false;
                    getSpeechDone = false;

                    if(NewChapterForTTSActivity.this.isFinishing()) return;
                    NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                new MyToast(NewChapterForTTSActivity.this, getResources().getString(R.string.translateSuccess));
                                LinearLayout translating = findViewById(R.id.translating);
                                translating.setVisibility(View.INVISIBLE);

                                normal.setVisibility(View.VISIBLE);

                                AudioUtils audioUtils = new AudioUtils();
                                MilliToHMS milliToHMS = new MilliToHMS();
                                TextView end = findViewById(R.id.end);
                                end.setText(milliToHMS.milliToHMS(audioUtils.getLength(MP3_LOCATION)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable deleteDraft = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/deleteDraft?bookid=" + bookid;

                HttpUtils httpUtils = new HttpUtils(url);
                httpUtils.doHttp(null, "GET", "application/json");//向后端发送请求

                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(speechFile != null && speechFile.exists()) speechFile.delete();
                        if(bgm != null && bgm.exists()) bgm.delete();
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable storeDraft = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/storeDraft";

                JSONObject info = new JSONObject();
                info.put("bookid",bookid);
                info.put("draft",content.getText().toString());
                byte[] param = info.toString().getBytes();

                HttpUtils httpUtils = new HttpUtils(url);
                httpUtils.doHttp(param, "GET", "application/json");//向后端发送请求

                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new MyToast(NewChapterForTTSActivity.this,getResources().getString(R.string.draft));
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    //获取上次的草稿
    Runnable getDraft = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/getDraft?bookid=" + bookid;

                HttpUtils httpUtils = new HttpUtils(url);
                ByteArrayOutputStream outputStream = httpUtils.doHttp(null, "GET",
                        "application/json");

                if (outputStream == null) {//请求超时
                    if(NewChapterForTTSActivity.this.isFinishing()) return;
                    NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(NewChapterForTTSActivity.this, getResources().getString(R.string.HttpTimeOut));

                            findViewById(R.id.loadinggif).setVisibility(View.INVISIBLE);
                            findViewById(R.id.Remind).setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                final String draft = new String(outputStream.toByteArray(),
                        StandardCharsets.UTF_8);

                if(NewChapterForTTSActivity.this.isFinishing()) return;
                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        content.setText(draft);

                        loadView.setVisibility(View.INVISIBLE);
                        normal.setVisibility(View.VISIBLE);
                    }
                });


            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private class StoreChapter extends Thread
    {
        private String title;
        private int bookid;
        private String content;
        private String speechPath;
        private String bgmPath;

        public StoreChapter(String title,int bookid,String content,String speechPath,String bgmPath){
            this.title = title;
            this.bookid = bookid;
            this.content = content;
            this.speechPath = speechPath;
            this.bgmPath = bgmPath;
        }

        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/storeChapter";

                JSONObject info = new JSONObject();
                info.put("title",title);
                info.put("bookid",bookid);
                info.put("content",content);
                info.put("speechPath",speechPath);
                info.put("bgmPath",bgmPath);
                info.put("type",false);

                TextView endView = findViewById(R.id.end);
                info.put("length",endView.getText().toString());

                byte[]param = info.toString().getBytes();

                HttpUtils httpUtils = new HttpUtils(url);
                httpUtils.doHttp(param, "POST", "application/json");

                //if(NewChapterForTTSActivity.this.isFinishing()) return;
                NewChapterForTTSActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new MyToast(NewChapterForTTSActivity.this,"创建成功!");
                    }
                });


            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
