package com.example.myapplication.Activity.Work;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.myapplication.InternetUtils.GetServer;
import com.example.myapplication.InternetUtils.HttpUtils;
import com.example.myapplication.Activity.Work.EditChapterActivity;
import com.example.myapplication.MyComponent.MySpinner;
import com.example.myapplication.MyComponent.MyToast;
import com.example.myapplication.R;
import com.example.myapplication.AudioUtils.MilliToHMS;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ChapterActivity extends AppCompatActivity {

    private LinearLayout loadingView;
    private LinearLayout normal;
    private SeekBar seekBar;//进度条
    private MySpinner playMode;//播放模式选择
    private MySpinner menu;

    private boolean hasReachedEnd = false;//是否已到达顺序播放的结尾
    private int chapterID;
    private int bookid;
    private String account;
    private String speechPath;
    private String bgmPath;
    private boolean hasPre = false;
    private JSONArray chapterIDs;//对应bookid的所有chapterID
    private int now_chapter_index = 0;
    private int EXIT_TIME_CHOICE = 0;//退出时间的选择
    final private int WORDS_EACH_LINE = 22;//每行的字符数目
    private CountDownTimer countDownTimer;//定时退出计时器

    final private int SINGLE_LOOP = 1;//单曲循环
    final private int SEQUENCE = 2;//顺序播放(没有循环)
    final private int LOOP = 3;//列表循环
    private int NOW_MODE = SINGLE_LOOP;//默认为单曲循环
    private boolean hasClickPlayMode = false;//是否选择了播放模式
    private boolean hasClickMenu = false;//是否点击了菜单
    private boolean hasPlayerReset = false;
    private boolean isInNight = false;//是否处于夜间模式


    private File speechFile = null;
    private File bgm = null;
    private String MP3_LOCATION;
    private String BGM_LOCATION;

    private MediaPlayer speech_player;//音频播放
    private MediaPlayer bgm_player;//bgm播放

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserState",MODE_PRIVATE);
        account = sharedPreferences.getString("Account","");
        isInNight = sharedPreferences.getBoolean("night",false);//是否处于夜间模式

        super.onCreate(savedInstanceState);
        if(isInNight){
            setContentView(R.layout.activity_chapter_night);
        }else {
            setContentView(R.layout.activity_chapter);
        }

        MP3_LOCATION = this.getCacheDir().getAbsolutePath()+"/temp.mp3";
        BGM_LOCATION = this.getCacheDir().getAbsolutePath()+"/bgm.mp3";

        Intent intent = getIntent();
        chapterID = intent.getIntExtra("chapterId",-1);
        bookid = intent.getIntExtra("bookid",-1);
        hasPre = intent.getBooleanExtra("hasPre",false);//是否有权限修改章节


        TextView content = findViewById(R.id.content);
        content.setMovementMethod(ScrollingMovementMethod.getInstance());

        final String[] choice = new String[]{"定时退出","修改章节"};
        ArrayAdapter arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,choice);
        menu = findViewById(R.id.menu);//默认只显示定时退出选项
        if(hasPre){//有修改章节的权限,menu显示修改章节和定时退出
            menu.setAdapter(arrayAdapter);
        }
        menu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!hasClickMenu){
                    hasClickMenu = true;
                    return;
                }//取消spinner的默认选择

                String select = parent.getItemAtPosition(position).toString();

                switch (select){
                    case "定时退出":
                        exitTimerStart();
                        break;
                    case "修改章节":
                        releasePlayer();
                        ChapterActivity.this.finish();
                        Intent intent = new Intent(ChapterActivity.this, EditChapterActivity.class);
                        intent.putExtra("id", chapterID);
                        startActivity(intent);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        playMode = findViewById(R.id.playMode);
        playMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!hasClickPlayMode){
                    hasClickPlayMode = true;
                    return;
                }//取消spinner的默认选择

                String select = parent.getItemAtPosition(position).toString();

                switch (select){
                    case "单曲循环":
                        NOW_MODE = SINGLE_LOOP;
                        if(ChapterActivity.this.isFinishing()) return;
                        ChapterActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               playMode.setBackground(getResources().getDrawable(R.drawable.singleloop));
                            }
                        });
                        break;
                    case "顺序播放":
                        NOW_MODE = SEQUENCE;
                        if(ChapterActivity.this.isFinishing()) return;
                        ChapterActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                playMode.setBackground(getResources().getDrawable(R.drawable.sequence));
                            }
                        });
                        break;
                    case "列表循环":
                        NOW_MODE = LOOP;
                        if(ChapterActivity.this.isFinishing()) return;
                        ChapterActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                playMode.setBackground(getResources().getDrawable(R.drawable.loop));
                            }
                        });
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        normal = findViewById(R.id.normal);

        loadingView = findViewById(R.id.Loading);
        loadingView.setVisibility(View.VISIBLE);
        normal.setVisibility(View.INVISIBLE);

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

                    ChapterActivity.this.runOnUiThread(new Runnable() {
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

        speech_player = new MediaPlayer();
        bgm_player = new MediaPlayer();

        speech_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //当前音频播放完时，有三种情况
                resetPlayer();

               try {
                    switch (NOW_MODE) {
                        case SINGLE_LOOP:
                            new Thread(prepareSpeech).start();
                            break;

                        case SEQUENCE:
                            if (now_chapter_index < chapterIDs.length() - 1) {//播放下一首
                                playNextSpeech();
                            }else if(now_chapter_index == chapterIDs.length() - 1){
                                hasReachedEnd = true;
                            }
                            break;

                        case LOOP:
                            playNextSpeech();
                            break;
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        new Thread(addRecord).start();//添加浏览记录
        refresh();
    }

    private void setRotateAnime(){
        ImageView rotation = findViewById(R.id.rotation);
        Animation rotate = AnimationUtils.loadAnimation(this,R.anim.image_rotate);
        LinearInterpolator linearInterpolator = new LinearInterpolator();
        rotate.setInterpolator(linearInterpolator);
        rotation.setAnimation(rotate);
        rotation.startAnimation(rotate);
    }

    private void clearRotateAnime(){
        ImageView rotation = findViewById(R.id.rotation);
        rotation.clearAnimation();
    }

    //无文本,显示唱片，隐藏文本框
    private void setRecord(){
        TextView content = findViewById(R.id.content);
        LinearLayout rotate = findViewById(R.id.rotateLayout);
        content.setVisibility(View.GONE);
        rotate.setVisibility(View.VISIBLE);
    }

    //有文本，隐藏唱片，显示文本框
    private void setTextView(){
        TextView content = findViewById(R.id.content);
        LinearLayout rotate = findViewById(R.id.rotateLayout);
        content.setVisibility(View.VISIBLE);
        rotate.setVisibility(View.GONE);
    }

    /*
    time:定时退出的秒数
     */
    public void exitTimerStart(){
        final String[] timeChoice = new String[]{"1分钟","10分钟","30分钟","1小时","5小时","取消定时"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("确定",null);
        builder.setNegativeButton("取消",null);
        builder.setTitle("定时退出");

        //设置选择监听事件
        final int formerChoice = EXIT_TIME_CHOICE;//之前的退出时间
        builder.setSingleChoiceItems(timeChoice, EXIT_TIME_CHOICE,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EXIT_TIME_CHOICE = which;
                    }
                });

        final AlertDialog dialog = builder.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                int time = 0;
                switch (EXIT_TIME_CHOICE){
                    case 0:
                        time = 60;
                        break;
                    case 1:
                        time = 600;
                        break;
                    case 2:
                        time = 1800;
                        break;
                    case 3:
                        time = 3600;
                        break;
                    case 4:
                        time = 18000;
                        break;
                    default:
                        break;
                }

                if(countDownTimer != null) countDownTimer.cancel();

                if(time > 0) {
                    countDownTimer = new CountDownTimer(time * 1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            releasePlayer();
                            ChapterActivity.super.onBackPressed();
                        }
                    };
                    countDownTimer.start();
                }
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                EXIT_TIME_CHOICE = formerChoice;
            }
        });
    }

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

    public void onBackPressed(View view){
        onBackPressed();
    }

    @Override
    public void onBackPressed(){
        hasPlayerReset = true;
        releasePlayer();
        super.finish();
    }

    //重置播放状态
    private void resetPlayer(){
        hasPlayerReset = true;

        if(speech_player != null){
            speech_player.reset();
        }

        if(bgm_player != null){
            bgm_player.reset();
        }

        if(ChapterActivity.this.isFinishing()) return;
        ChapterActivity.this.runOnUiThread(new Runnable() {
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

    //控制音乐的播放与暂停
    public void controlSpeech(View view){
        if(hasReachedEnd){
            hasReachedEnd = false;
            new Thread(prepareSpeech).start();
        }
        else new Thread(controlSpeech).start();
    }

    private void playNextSpeech(){
        try {
            resetPlayer();

            if (now_chapter_index == chapterIDs.length() - 1) now_chapter_index = 0;
            else now_chapter_index++;

            chapterID = chapterIDs.getJSONObject(now_chapter_index).getInt("id");

            ChapterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingView = findViewById(R.id.Loading);
                    loadingView.setVisibility(View.VISIBLE);
                    normal.setVisibility(View.INVISIBLE);
                }
            });
            new Thread(getChapter).start();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void playNextSpeech(View view){
        resetPlayer();
        playNextSpeech();
    }

    private void playPreSpeech(){
        try {
            resetPlayer();

            if (now_chapter_index == 0) now_chapter_index = chapterIDs.length() - 1;
            else now_chapter_index--;

            chapterID = chapterIDs.getJSONObject(now_chapter_index).getInt("id");

            ChapterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingView = findViewById(R.id.Loading);
                    loadingView.setVisibility(View.VISIBLE);
                    normal.setVisibility(View.INVISIBLE);
                }
            });
            new Thread(getChapter).start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void playPreSpeech(View view) {
            playPreSpeech();
    }

    //刷新界面
    public void refresh(View view){
        refresh();
    }

    public void refresh(){

        loadingView.setClickable(false);

        CountDownTimer countDownTimer = new CountDownTimer(5000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                loadingView.setClickable(true);
            }
        };//防止用户高频率点击
        countDownTimer.start();

        loadingView.setVisibility(View.VISIBLE);//加载画面
        findViewById(R.id.loadinggif).setVisibility(View.VISIBLE);
        findViewById(R.id.Remind).setVisibility(View.INVISIBLE);

        new Thread(getChapter).start();
        new Thread(getChapterIDs).start();
    }

    //准备并播放音频
    Runnable prepareSpeech = new Runnable() {
        @Override
        public void run() {
            try {
                //没有音频或音频尚未转换成功
                if(speechFile == null){
                    if(ChapterActivity.this.isFinishing()) return;
                    new MyToast(ChapterActivity.this,"语音文件不存在!");
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
                                        if(ChapterActivity.this.isFinishing()) break;
                                        if(speech_player == null || hasPlayerReset) break;
                                        seekBar.setProgress(speech_player.getCurrentPosition());
                                        ChapterActivity.this.runOnUiThread(new Runnable() {
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

                        /*//文本高亮设置
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while(!Thread.currentThread().isInterrupted()){
                                    try{
                                        if(speech_player == null || hasPlayerReset){
                                            break;
                                        }
                                        if(ChapterActivity.this.isFinishing()) break;

                                        ChapterActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                TextView textView = ChapterActivity.this.findViewById(R.id.content);
                                                String text = textView.getText().toString();


                                                int index = text.length() * speech_player.getCurrentPosition() / speech_player.getDuration();
                                                int preIndex = 0,afterIndex = 0;

                                                for(int i=index;i>=0;i--){
                                                    if(text.charAt(i) == '\n' || i==0){
                                                        preIndex = i;
                                                        break;
                                                    }
                                                }

                                                for(int i=index;i<text.length();i++){
                                                    if(text.charAt(i) == '\n' || i==text.length()-1){
                                                        afterIndex = i;
                                                        break;
                                                    }
                                                }

                                                SpannableString spannableString = new SpannableString(text);
                                                spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FF0000")),preIndex,afterIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                                textView.setText(spannableString);
                                            }
                                        });

                                        Thread.sleep(200);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).start();*/

                        if(ChapterActivity.this.isFinishing()) return;
                        ChapterActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView playButton = findViewById(R.id.PlayButton);
                                playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pause));
                                setRotateAnime();
                            }
                        });
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable controlSpeech = new Runnable() {
        @Override
        public void run() {
            if(speech_player == null || bgm_player == null) return;

            if(speech_player.isPlaying()) {
                speech_player.pause();
                bgm_player.pause();
                if(ChapterActivity.this.isFinishing()) return;
                ChapterActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView playButton = findViewById(R.id.PlayButton);
                        playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.play));
                        clearRotateAnime();
                    }
                });
            }
            else {
                speech_player.start();
                bgm_player.start();
                if(ChapterActivity.this.isFinishing()) return;
                ChapterActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView playButton = findViewById(R.id.PlayButton);
                        playButton.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.pause));
                        setRotateAnime();
                    }
                });
            }
        }
    };

    Runnable addRecord = new Runnable() {
        @Override
        public void run() {
            try{

                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/addrecord";

                JSONObject params = new JSONObject();
                params.put("account",account);
                params.put("id",bookid);
                params.put("time",String.valueOf(System.currentTimeMillis()));

                byte[] param = params.toString().getBytes();

                HttpUtils httpUtils = new HttpUtils(url);
                httpUtils.doHttp(param, "POST", "application/json");
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable getChapterIDs = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/getchapterIDs?bookid="
                        + bookid;

                HttpUtils httpUtils = new HttpUtils(url);
                ByteArrayOutputStream outputStream = httpUtils.doHttp(null, "GET",
                        "application/json");

                if (outputStream == null) {//请求超时
                    if(ChapterActivity.this.isFinishing()) return;
                    ChapterActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(ChapterActivity.this, getResources().getString(R.string.HttpTimeOut));

                            findViewById(R.id.loadinggif).setVisibility(View.INVISIBLE);
                            findViewById(R.id.Remind).setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                final String result = new String(outputStream.toByteArray(),
                        StandardCharsets.UTF_8);

                chapterIDs = new JSONArray(result);

                for(int i=0;i<chapterIDs.length();i++){
                    //获取当前播放的chapter在chapterIDs中的index
                    if(chapterIDs.getJSONObject(i).getInt("id") == chapterID){
                        now_chapter_index = i;
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    Runnable getChapter = new Runnable() {
        @Override
        public void run() {
            try{
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/getchapterbyid?id="
                        + chapterID;

                HttpUtils httpUtils = new HttpUtils(url);
                ByteArrayOutputStream outputStream = httpUtils.doHttp(null, "GET",
                        "application/json");

                if (outputStream == null) {//请求超时
                    if(ChapterActivity.this.isFinishing()) return;
                    ChapterActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(ChapterActivity.this, getResources().getString(R.string.HttpTimeOut));

                            findViewById(R.id.loadinggif).setVisibility(View.INVISIBLE);
                            findViewById(R.id.Remind).setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                final String result = new String(outputStream.toByteArray(),
                        StandardCharsets.UTF_8);

                final JSONObject chapter = new JSONObject(result);

                if(ChapterActivity.this.isFinishing()) return;
                ChapterActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TextView title = findViewById(R.id.title);
                            title.setText(chapter.getString("title"));

                            String text = chapter.getString("content");
                            if(text.length() == 0){
                                setRecord();
                                setRotateAnime();//设置唱片旋转动画
                            }
                            else {
                                setTextView();
                                TextView content = findViewById(R.id.content);
                                /*String textAfter = "";
                                for(int i=0;i<text.length();i+=WORDS_EACH_LINE){
                                    int endIndex = (i + WORDS_EACH_LINE < text.length()) ? (i + WORDS_EACH_LINE) : text.length();
                                    textAfter += (endIndex == text.length()) ? text.substring(i,endIndex) : text.substring(i,endIndex) + "\n";
                                }*/
                                content.setText(text);
                            }

                            speechPath = chapter.getString("speechPath");
                            bgmPath = chapter.getString("bgmPath");

                            TextView end = findViewById(R.id.end);
                            end.setText(chapter.getString("length"));

                            new Thread(getSpeech).start();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
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
                    if(ChapterActivity.this.isFinishing()) return;
                    ChapterActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(ChapterActivity.this, getResources().getString(R.string.HttpTimeOut));

                            findViewById(R.id.loadinggif).setVisibility(View.INVISIBLE);
                            findViewById(R.id.Remind).setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                if(ChapterActivity.this.isFinishing()) return;
                ChapterActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            speechFile = new File(MP3_LOCATION);//speechFile保存后端语音
                            if (!speechFile.exists()) speechFile.createNewFile();
                            OutputStream outputStream = new FileOutputStream(speechFile);

                            resultStream.writeTo(outputStream);

                            seekBar.setProgress(0);

                            TextView begin = findViewById(R.id.begin);
                            begin.setText(getResources().getString(R.string.initial));

                            outputStream.close();

                            new Thread(getBgm).start();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
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

                if(ChapterActivity.this.isFinishing()) return;
                ChapterActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            bgm = new File(BGM_LOCATION);//speechFile保存后端语音
                            if (!bgm.exists()) bgm.createNewFile();
                            OutputStream outputStream = new FileOutputStream(bgm);

                            resultStream.writeTo(outputStream);
                            outputStream.close();


                            loadingView.setVisibility(View.INVISIBLE);
                            loadingView.setClickable(false);
                            normal.setVisibility(View.VISIBLE);
                            new Thread(prepareSpeech).start();

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };
}
