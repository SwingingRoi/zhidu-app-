package com.example.myapplication.PicUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.myapplication.GetServer;
import com.example.myapplication.HttpUtils;

import java.io.ByteArrayOutputStream;


public class GetPicture {

    public Bitmap getAvatar(String account){
        Bitmap result = null;
        try {
            GetServer getServer = new GetServer();
            String url = getServer.getIPADDRESS() + "/audiobook/getAvatar?account=" + account;

            HttpUtils httpUtils = new HttpUtils(url);
            ByteArrayOutputStream stream = httpUtils.doHttp(null, "GET", "image/*");
            if(stream!=null) {
                result = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().length);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public Bitmap getSurface(int bookid){
        Bitmap result = null;
        try {
            GetServer getServer = new GetServer();
            String url = getServer.getIPADDRESS() + "/audiobook/getSurface?id=" + bookid;

            HttpUtils httpUtils = new HttpUtils(url);
            ByteArrayOutputStream stream = httpUtils.doHttp(null, "GET", "image/*");
            if(stream!=null) {
                result = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().length);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}