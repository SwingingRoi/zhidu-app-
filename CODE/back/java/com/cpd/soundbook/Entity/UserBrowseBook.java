package com.cpd.soundbook.Entity;

import com.mongodb.util.JSON;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "user_browse_book")
public class UserBrowseBook {
    private String account;
    private int bookid;
    private String time;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    public JSONObject toJSONObject(){
        JSONObject result = new JSONObject();
        try{
            result.put("id",id);

            //时间戳转化为日期
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long timStamp = Long.parseLong(time);
            Date date = new Date(timStamp);
            String t = simpleDateFormat.format(date);

            result.put("time",t);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public UserBrowseBook(){}

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    public void setBookid(int bookid) {
        this.bookid = bookid;
    }

    public int getBookid() {
        return bookid;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }
}
