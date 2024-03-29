package com.cpd.soundbook.Controller.BookController;

import com.cpd.soundbook.HttpUtils.HttpUtils;
import com.cpd.soundbook.Service.ServiceInterface.BookService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;

@RestController
public class SearchBookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private HttpUtils httpUtils;

    @RequestMapping("/audiobook/searchbook")
    public void searchBook(@RequestParam("search") String search, @RequestParam("from") int from,
                           @RequestParam("size") int size, HttpServletResponse response){
        try{
            JSONArray result = bookService.searchBook(search,from,size);
            httpUtils.writeStringBack(response,result.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @RequestMapping("/audiobook/getbookbyid")
    public void getBookById(@RequestParam("id") int bookid, HttpServletResponse response){
        try{
            httpUtils.writeStringBack(response,bookService.searchBookById(bookid).toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @RequestMapping("/audiobook/searchwork")
    public void searchWorkByTitle(HttpServletResponse response, @RequestParam("author") String author,
                                  @RequestParam("title") String title, @RequestParam("from") int from, @RequestParam("size") int size){
        try{
            JSONArray result = bookService.searchWorkByTitle(URLDecoder.decode(author, "UTF-8"),URLDecoder.decode(title, "UTF-8"),from,size);
            httpUtils.writeStringBack(response,result.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
