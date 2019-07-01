package com.cpd.soundbook.Controller.BrowseController;

import com.cpd.soundbook.HttpUtils;
import com.cpd.soundbook.Service.ServiceImpl.UserBrowseBookService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class DeleteRecordController {

    @Autowired
    private UserBrowseBookService userBrowseBookService;

    @Autowired
    private HttpUtils httpUtils;

    @RequestMapping("/audiobook/deleteHistory")
    public void deleteRecord(HttpServletRequest request){
        try{
            JSONArray ids = new JSONArray(httpUtils.getStringParam(request));
            userBrowseBookService.deleteRecords(ids);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}