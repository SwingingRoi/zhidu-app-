package com.cpd.soundbook.Controller.SpeechController;

import com.cpd.soundbook.HttpUtils.HttpUtils;
import com.cpd.soundbook.Service.ServiceInterface.ChapterService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class TextToSpeechController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private HttpUtils httpUtils;

    @RequestMapping("/audiobook/textToSpeech")
    public void textToSpeech(HttpServletRequest request, HttpServletResponse response){
        try{
            //httpUtils.writeStringBack(response,"hello there!");
            JSONObject text = new JSONObject(httpUtils.getStringParam(request));
            //System.out.println("text" + text.getString("text"));
            httpUtils.writeStringBack(response,chapterService.textToSpeech(text.getString("text")));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
