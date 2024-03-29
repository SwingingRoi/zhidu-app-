package com.cpd.soundbook.AudioUtils;/*
<dependency>
<groupId>com.hankcs</groupId>
<artifactId>hanlp</artifactId>
<version>portable-1.6.1</version>
</dependency>
Usage:
getKeyList return a list of terms
example:
termList = getKeyList("下雨，打雷, 狗");
Term1:{word:下雨, nature:v, offset:0}
Term2:{word:打雷, nature:v, offset:3}
Term3:{word:狗, nature:n, offset:6}
*/

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component(value = "getEffectKey")
public class GetEffectKey {

    private MongoClient mongoClient = new MongoClient("localhost", 27017);
    private DB db = mongoClient.getDB("effects");
    private GridFS gridFS = new GridFS(db, "fs");

    public List<Term> getAllWordList(String text){
        return IndexTokenizer.segment(text);
    }

    public List<Term> getAllNounList(String text){
        List<Term> nounList = new ArrayList<Term>();
        List<Term> termList = IndexTokenizer.segment(text);

        //System.out.println(termList);
        String key;
        for(Term term : termList){
            key = term.nature.toString();
            if(key.substring(0,1).equals("n")){
                nounList.add(term);
            }
        }
        return nounList;
    }

    public List<Term> getAllVerbList(String text){
        List<Term> verbList = new ArrayList<Term>();
        List<Term> termList = IndexTokenizer.segment(text);

        //System.out.println(termList);
        String key;
        for(Term term : termList){
            key = term.nature.toString();
            if(key.substring(0,1).equals("v")){
                verbList.add(term);
            }
        }
        return verbList;
    }

    public HashMap<Integer,String> getKeyList(String text){
        HashMap<Integer,String> result = new HashMap<>();


        List<Term> nounList = getAllNounList(text);
        //System.out.println(nounList);
        List<Term> verbList = getAllVerbList(text);
        //System.out.println(verbList);
        List<Term> targetList = new ArrayList<Term>();

        List<GridFSDBFile> gridFSDBFiles = gridFS.find(new BasicDBObject("contentType", null));
        //System.out.println("gridfsdbfile " + gridFSDBFiles);
        List<String> verbStringList = new ArrayList<String>();

        for(Term verb:verbList){
            verbStringList.add(verb.word);
        }

        boolean isAdd;

        //for each noun in the text
        for(Term term:nounList){

            isAdd = false;

            //noun match in the database
            for(GridFSDBFile gridFSDBFile:gridFSDBFiles){

                if(gridFSDBFile != null){

                    String jsonStr = gridFSDBFile.toString();
                    //System.out.println(gridFSDBFile);
                    JSONObject jsonObject = new JSONObject(jsonStr);

                    String noun = jsonObject.getString("noun");

                    if(term.word.contains(noun)){
                        //System.out.println("gridFSDBFile in nounList: " + gridFSDBFile);

                        JSONArray verbs = jsonObject.getJSONArray("verb");
                        //System.out.println("target verbs: " + verbs);

                        for(int i = 0; i < verbs.length(); ++i){
                            String verb = (String)verbs.get(i);
                            //System.out.println("verb " + i + " in verbs: " + verb);

                            if(term.word.contains(verb)){
                                isAdd = true;
                            }else{
                                for(String targetverb:verbStringList){
                                    if(targetverb.contains(verb)){
                                        isAdd = true;
                                        break;
                                    }
                                }
                            }

                            if(isAdd){
                                //System.out.println("exist:" + verb);
                                if(!targetList.contains(term)){
                                    targetList.add(term);
                                    result.put(term.offset, jsonObject.getString("filename"));
                                }

                                //System.out.println(targetList);
                            }
                        }
                    }

                    //System.out.println(verbs.get(0));
                    //System.out.println(jsonObject.getString("filename"));
                }

                if(isAdd){
                    break;
                }
            }
        }

        for(Term term:verbList){
            isAdd = false;

            //noun match in the database
            for(GridFSDBFile gridFSDBFile:gridFSDBFiles){

                if(gridFSDBFile != null){

                    String jsonStr = gridFSDBFile.toString();
                    //System.out.println(gridFSDBFile);
                    JSONObject jsonObject = new JSONObject(jsonStr);

                    String noun = jsonObject.getString("noun");

                    if(term.word.contains(noun)){
                        //System.out.println("verbList contains noun:" + gridFSDBFile);
                        if(term.nature.toString().equals("v") && term.word.equals(noun)){

                            targetList.add(term);
                            result.put(term.offset, jsonObject.getString("filename"));
                            isAdd = true;

                        }else{

                            JSONArray verbs = jsonObject.getJSONArray("verb");
                            //System.out.println(verbs);

                            for(int i = 0; i < verbs.length(); ++i){
                                String verb = (String)verbs.get(i);
                                //System.out.println(verb);
                                if(term.word.contains(verb)){
                                    //System.out.println("exist:" + verb);
                                    targetList.add(term);
                                    result.put(term.offset, jsonObject.getString("filename"));
                                    isAdd = true;
                                    //System.out.println(targetList);
                                    break;
                                }
                            }
                        }
                    }
                    //System.out.println(verbs.get(0));
                    //System.out.println(jsonObject.getString("filename"));
                }
                if(isAdd){
                    break;
                }
            }
        }

        return result;
    }

}