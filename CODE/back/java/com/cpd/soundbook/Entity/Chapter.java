package com.cpd.soundbook.Entity;

import javax.persistence.*;

@Entity
@Table(name = "chapters")
public class Chapter {
    private int bookid;
    private String content;
    private String title;
    private String speechpath;
    private String time;
    private String bgmpath;
    private boolean type;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    public Chapter(){}

    public void setBookid(int bookid) {
        this.bookid = bookid;
    }

    public int getBookid() {
        return bookid;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setSpeechPath(String speechPath) {
        this.speechpath = speechPath;
    }

    public String getSpeechPath() {
        return speechpath;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public void setBgmpath(String bgmpath) {
        this.bgmpath = bgmpath;
    }

    public String getBgmpath() {
        return bgmpath;
    }

    public void setType(boolean type) {
        this.type = type;
    }

    public boolean isType() {
        return type;
    }
}
