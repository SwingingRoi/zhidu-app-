package com.cpd.soundbook.Controller.BGMController;

import com.cpd.soundbook.HttpUtils.HttpUtils;
import com.cpd.soundbook.Service.ServiceImpl.ChapterService;
import com.mongodb.gridfs.GridFSDBFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLDecoder;

@RestController
public class GetBGMController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private HttpUtils httpUtils;

    @RequestMapping("/audiobook/getBGM")
    public void getBGM(@RequestParam("filename") String filename, HttpServletResponse response){
        try{
            //System.out.println("begin get bgm");
            String fileName = URLDecoder.decode(filename,"UTF-8");
            GridFSDBFile bgm = chapterService.getBGM(fileName);
            //System.out.println("begin transfer bgm");
            if(bgm==null){
                httpUtils.writeStringBack(response,null);
            }
            else {
                OutputStream outputStream = response.getOutputStream();
                bgm.writeTo(outputStream);
                outputStream.flush();
                outputStream.close();
            }
            //System.out.println("done");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
