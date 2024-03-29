package com.example.myapplication.Activity.Work;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.myapplication.InternetUtils.GetServer;
import com.example.myapplication.InternetUtils.HttpUtils;
import com.example.myapplication.R;
import com.example.myapplication.MyComponent.MyToast;
import com.example.myapplication.MyComponent.MySpinner;
import com.example.myapplication.Activity.Book.BookActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.example.myapplication.PicUtils.GetPicture;

public class PersonalWorkActivity extends AppCompatActivity {

    private LinearLayout normal;
    private LinearLayout loadView;
    private LinearLayout bookTable;
    private LinearLayout manageBox;
    private ScrollView scrollView;
    private SwipeRefreshLayout swipeRefreshLayout;//下拉刷新控件
    private MySpinner menu;
    private View pullDown;//请求文字提示
    private boolean firstIn = true;//是否是第一次进入该页面
    private boolean hasClickMenu = false;//是否点击菜单
    private boolean ismanaging = false;//是否处于管理模式
    private boolean isRequesting = false;//当前是否在向后端请求书本信息
    private boolean isInNight = false;//是否处于夜间模式
    private float preY;//用户触摸屏幕时的手指纵坐标
    private float nowY;//用户手指离开屏幕时的纵坐标

    private JSONArray works;

    private int from=0;//当前获取的works从第几本书开始
    final private int PAGESIZE=10;

    private String account;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("UserState", MODE_PRIVATE);
            account = sharedPreferences.getString("Account", "");
            isInNight = sharedPreferences.getBoolean("night",false);//是否处于夜间模式

            super.onCreate(savedInstanceState);
            if(isInNight){
                setContentView(R.layout.activity_personal_work_night);
            }else {
                setContentView(R.layout.activity_personal_work);
            }


            works = new JSONArray();

            bookTable = findViewById(R.id.BookTable);
            if(isInNight){
                pullDown = LayoutInflater.from(this).inflate(R.layout.pull_down_night, null);
            }else {
                pullDown = LayoutInflater.from(this).inflate(R.layout.pull_down, null);
            }

            bookTable.addView(pullDown);

            scrollView = findViewById(R.id.scroll);
            scrollView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            preY = event.getY();
                            break;
                        case MotionEvent.ACTION_UP:
                            nowY = event.getY();
                            if(nowY < preY){
                                if (!isRequesting && scrollView.getChildAt(0).getMeasuredHeight() <= scrollView.getScrollY() + scrollView.getHeight()) {
                                    isRequesting = true;
                                    new Thread(getWorks).start();//向后端请求更多书本
                                }
                            }
                    }
                    return false;
                }
            });

            swipeRefreshLayout = findViewById(R.id.swipe_container);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refresh();
                }
            });
            manageBox = findViewById(R.id.manage);

            normal = findViewById(R.id.Normal);
            loadView = findViewById(R.id.Loading);
            normal.setVisibility(View.INVISIBLE);

            menu = findViewById(R.id.menu);
            menu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if(!hasClickMenu){
                        hasClickMenu = true;
                        return;
                    }//取消spinner的默认选择

                    String select = parent.getItemAtPosition(position).toString();
                    if (select.equals("新建作品")) {
                        toNewBook();
                    }
                    else {
                        if(!ismanaging){
                            toManage();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            refresh();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //用户按下返回键
    public void onBackPressed(View view){
        super.onBackPressed();
    }

    //跳转至新书创建页面
    private void toNewBook(){
        Intent intent = new Intent(this, NewBookActivity.class);
        startActivity(intent);
    }

    //开启管理模式
    private void toManage(){
        if(ismanaging) return;

        ismanaging = true;
        manageBox.setVisibility(View.VISIBLE);
        for(int i=0;i<works.length();i++){
            final View bookRow = bookTable.getChildAt(i);
            CheckBox checkBox = bookRow.findViewById(R.id.checkBox);
            checkBox.setVisibility(View.VISIBLE);
        }
    }

    //在管理模式下点击bookRow跳转至modify界面
    private void jumpToBook(int bookid){

            Intent intent = new Intent(this,BookActivity.class);
            intent.putExtra("id",bookid);
            startActivity(intent);
    }

    //退出管理
    public void cancelManage(View view){
        cancelManage();
    }

    private void cancelManage(){

        ismanaging = false;
        manageBox.setVisibility(View.GONE);
        for(int i=0;i<works.length();i++){
            final View bookRow = bookTable.getChildAt(i);
            CheckBox checkBox = bookRow.findViewById(R.id.checkBox);
            checkBox.setChecked(false);
            checkBox.setVisibility(View.INVISIBLE);
        }
    }

    //删除图书
    public void doDelete(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("确定",null);
        builder.setNegativeButton("取消",null);
        builder.setTitle("删除图书");
        builder.setMessage("确认删除这些图书吗?");
        final AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                new MyToast(PersonalWorkActivity.this,"成功删除!");

                final LinearLayout delete = findViewById(R.id.check);
                delete.setClickable(false);
                CountDownTimer countDownTimer = new CountDownTimer(5000,1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        delete.setClickable(true);
                    }
                };//防止用户高频率点击
                countDownTimer.start();

                new Thread(deleteBooks).start();
            }
        });
    }

    Runnable deleteBooks = new Runnable() {
        @Override
        public void run() {

            GetServer getServer = new GetServer();
            String url = getServer.getIPADDRESS()+"/audiobook/deletebooks";

            JSONArray ids = new JSONArray();//要删除书本的id

            try {

               final List<Integer> removes = new ArrayList<>();//存储要删除的书本在works中的index
                for (int i = 0; i < works.length(); i++) {
                    View bookRow = bookTable.getChildAt(i);
                    CheckBox checkBox = bookRow.findViewById(R.id.checkBox);
                    if (checkBox.isChecked()) {
                        removes.add(i);
                        JSONObject object = new JSONObject();
                        object.put("id", works.getJSONObject(i).getInt("id"));
                        ids.put(object);
                    }
                }

                PersonalWorkActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(PersonalWorkActivity.this.isFinishing()) return;
                        int hasRemoved=0;
                        for (int i : removes) {
                            try {
                                DeleteSurface deleteSurface = new DeleteSurface(works.getJSONObject(i - hasRemoved).getString("surface"));
                                deleteSurface.start();
                                bookTable.removeViewAt(i - hasRemoved);
                                works.remove(i - hasRemoved);//在删除的时候，i要减去前面已经删除的书本数目
                                hasRemoved++;
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                        from = from - removes.size();//更新请求书本的index
                    }
                });

                byte[] param = ids.toString().getBytes();
                HttpUtils httpUtils = new HttpUtils(url);
                httpUtils.doHttp(param, "POST", "application/json");//向后端发送删除请求

            }catch (Exception e){
                e.printStackTrace();
            }
        }

    };

    //跳转至搜索界面
    public void toSearch(View view){
        Intent intent = new Intent(this,SearchMyWorkActivity.class);
        startActivity(intent);
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

        if (isRequesting) return;

        isRequesting = true;

        from = 0;
        firstIn = true;
        if(ismanaging) cancelManage();
        ismanaging = false;
        bookTable.removeAllViews();
        works = new JSONArray();


        loadView.setVisibility(View.VISIBLE);//加载画面
        findViewById(R.id.loadinggif).setVisibility(View.VISIBLE);
        findViewById(R.id.Remind).setVisibility(View.INVISIBLE);
        normal.setVisibility(View.INVISIBLE);

        new Thread(getWorks).start();
    }

    //刷新界面
    public void refresh(View view){
        refresh();
    }

    //获取已有作品
    Runnable getWorks = new Runnable() {
        @Override
        public void run() {
            if(PersonalWorkActivity.this.isFinishing()) return;
            PersonalWorkActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView text = pullDown.findViewById(R.id.content);
                    text.setText(getResources().getString(R.string.isReq));
                }
            });

            try {

                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS()+"/audiobook/getworks?account="
                        + URLEncoder.encode(account,"UTF-8") + "&from=" + from + "&size=" + PAGESIZE;

                HttpUtils httpUtils = new HttpUtils(url);
                ByteArrayOutputStream outputStream = httpUtils.doHttp(null, "GET",
                        "application/json");

                if (outputStream == null) {//请求超时
                    if(PersonalWorkActivity.this.isFinishing()) return;
                    PersonalWorkActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new MyToast(PersonalWorkActivity.this, getResources().getString(R.string.HttpTimeOut));
                            isRequesting = false;
                            swipeRefreshLayout.setRefreshing(false);

                            findViewById(R.id.loadinggif).setVisibility(View.INVISIBLE);
                            if (firstIn) {//如果是首次进入，设置点击屏幕刷新提醒
                                findViewById(R.id.Remind).setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    return;
                }


                final String result = new String(outputStream.toByteArray(),
                        StandardCharsets.UTF_8);

                final JSONArray newWorks = new JSONArray(result);

                for (int i = 0; i < newWorks.length(); i++) {
                    works.put(newWorks.getJSONObject(i));
                }//向works中添加新请求过来的work

                if(PersonalWorkActivity.this.isFinishing()) return;
                PersonalWorkActivity.this.runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        bookTable.removeView(pullDown);
                        loadView.setVisibility(View.INVISIBLE);
                        normal.setVisibility(View.VISIBLE);
                        swipeRefreshLayout.setRefreshing(false);

                        if (works.length()==0) {//说明该用户尚没有发布作品
                                View noworkView;
                                if(isInNight) noworkView = LayoutInflater.from(PersonalWorkActivity.this).inflate(R.layout.no_work_style_night, null);
                                else noworkView = LayoutInflater.from(PersonalWorkActivity.this).inflate(R.layout.no_work_style, null);
                                if(firstIn) {
                                    firstIn = false;
                                    bookTable.addView(noworkView);
                                }

                                Button makeBtn = noworkView.findViewById(R.id.Make);
                                makeBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(PersonalWorkActivity.this, NewBookActivity.class);
                                        startActivity(intent);
                                    }
                                });
                                isRequesting = false;
                                return;
                        }

                        //依次添加作品到bookTable中
                        for (int i = 0; i < newWorks.length(); i++) {
                            try {
                                final View bookRow;
                                if(isInNight){
                                    bookRow = LayoutInflater.from(PersonalWorkActivity.this).inflate(R.layout.book_row_style_night, null);
                                }else {
                                    bookRow = LayoutInflater.from(PersonalWorkActivity.this).inflate(R.layout.book_row_style, null);
                                }


                                JSONObject work = newWorks.getJSONObject(i);

                                TextView title = bookRow.findViewById(R.id.BookName);
                                title.setText(work.getString("name"));

                                TextView viewNumber = bookRow.findViewById(R.id.viewnumber);
                                viewNumber.setText(String.valueOf(work.getInt("views")));

                                TextView chapterNumber = bookRow.findViewById(R.id.chapternumber);
                                chapterNumber.setText(work.getInt("chapters") + "章");

                                LinearLayout tagsView = bookRow.findViewById(R.id.tags);
                                String tagStr = work.getString("tags");
                                String[] tags = tagStr.split(" ");

                                TextView publishView = bookRow.findViewById(R.id.hasPublished);
                                publishView.setVisibility(View.VISIBLE);
                                if(work.getBoolean("publish")){
                                    publishView.setText(getResources().getString(R.string.published));
                                    publishView.setTextColor(Color.GREEN);
                                }
                                else {
                                    publishView.setText(getResources().getString(R.string.nopublished));
                                    publishView.setTextColor(Color.RED);
                                }

                                for(int j=0;j<tags.length;j++){
                                    String tag = tags[j];
                                    TextView t = new TextView(PersonalWorkActivity.this);
                                    t.setTextSize(10);
                                    if(isInNight){
                                        t.setTextColor(Color.WHITE);
                                    }else {
                                        t.setTextColor(Color.GRAY);
                                    }
                                    t.setText(tag);

                                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    layoutParams.setMargins(15,0,0,0);
                                    t.setLayoutParams(layoutParams);
                                    tagsView.addView(t);
                                }

                                if(ismanaging){
                                    CheckBox checkBox = bookRow.findViewById(R.id.checkBox);
                                    checkBox.setVisibility(View.VISIBLE);
                                }
                                bookTable.addView(bookRow);

                                final int id = newWorks.getJSONObject(i).getInt("id");
                                bookRow.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if(ismanaging){
                                            CheckBox checkBox = bookRow.findViewById(R.id.checkBox);
                                            checkBox.setChecked(!checkBox.isChecked());
                                        }else jumpToBook(id);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if(ismanaging){
                            toManage();
                        }

                        bookTable.addView(pullDown);
                        if(newWorks.length() < PAGESIZE){
                            TextView textView = pullDown.findViewById(R.id.content);
                            textView.setText(getResources().getString(R.string.hasEnd));
                            isRequesting = false;
                        }//说明书本已请求完毕
                        else {
                            TextView textView = pullDown.findViewById(R.id.content);
                            textView.setText(getResources().getString(R.string.pullDown));
                            isRequesting = false;
                        }
                    }
                });

                //获取封面
                for (int i = from; i < works.length(); i++) {
                    Thread thread = new GetSurface(i);
                    thread.start();
                }

                from = from + newWorks.length();//更新请求index
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private class GetSurface extends Thread
    {
        private int index;

        public GetSurface(int index){
            this.index = index;
        }

        @Override
        public void run() {
            try {
                GetPicture getPicture = new GetPicture();
                final Bitmap surface = getPicture.getSurface(works.getJSONObject(index).getInt("id"));

                if(PersonalWorkActivity.this.isFinishing()) return;
                PersonalWorkActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(surface!=null) {
                            View bookRow = bookTable.getChildAt(index);
                            ImageView surfaceView = bookRow.findViewById(R.id.surface);
                            surfaceView.setImageBitmap(surface);
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    private class DeleteSurface extends Thread{
        private String surfaceName;

        public DeleteSurface(String surfaceName){
            this.surfaceName = surfaceName;
        }

        @Override
        public void run() {
            try {
                GetServer getServer = new GetServer();
                String url = getServer.getIPADDRESS() + "/audiobook/deletesurface?filename=" + this.surfaceName;

                try{
                    HttpUtils httpUtils = new HttpUtils(url);
                    httpUtils.doHttp(null, "GET",
                            "application/json");

                }catch (Exception e){
                    e.printStackTrace();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
