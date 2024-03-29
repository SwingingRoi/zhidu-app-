package com.cpd.soundbook.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.springframework.stereotype.Repository;

import java.io.File;

@Repository
public class MongoDBImpl implements MongoDBInter{
    private final String HOST_NAME="localhost";
    private final int PORT=27017;
    private MongoClient client =new MongoClient(HOST_NAME,PORT);

    @Override
    public void saveFile(File file) {
        try {
            GridFS gridFS = new GridFS(client.getDB("audiobook"));
            GridFSInputFile gridFSInputFile = gridFS.createFile(file);
            gridFSInputFile.put("filename",file.getName());
            gridFSInputFile.save();
            file.delete();//删除临时文件
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public GridFSDBFile getFileByName(String filename) {
        GridFSDBFile gridFSDBFile = new GridFSDBFile();
        try{
            GridFS gridFS = new GridFS(client.getDB("audiobook"));
            DBObject dbObject = new BasicDBObject("filename",filename);
            gridFSDBFile= gridFS.findOne(dbObject);
        }catch (Exception e){
            e.printStackTrace();
        }
        return gridFSDBFile;
    }



    @Override
    public void deleteFile(String filename) {
        try{
            GridFS gridFS = new GridFS(client.getDB("audiobook"));
            DBObject dbObject = new BasicDBObject("filename",filename);
            gridFS.remove(dbObject);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public GridFSDBFile getBGMByLevel(int level) {
        GridFSDBFile gridFSDBFile = new GridFSDBFile();
        try{
            GridFS gridFS = new GridFS(client.getDB("bgm"));
            DBObject dbObject = new BasicDBObject("level",level);
            gridFSDBFile= gridFS.findOne(dbObject);
        }catch (Exception e){
            e.printStackTrace();
        }
        return gridFSDBFile;
    }

    @Override
    public GridFSDBFile getBGMByName(String name) {
        GridFSDBFile gridFSDBFile = new GridFSDBFile();
        try{
            GridFS gridFS = new GridFS(client.getDB("bgm"));
            DBObject dbObject = new BasicDBObject("filename",name);
            gridFSDBFile= gridFS.findOne(dbObject);
        }catch (Exception e){
            e.printStackTrace();
        }
        return gridFSDBFile;
    }

    @Override
    public GridFSDBFile getEffectByName(String name) {
        GridFSDBFile gridFSDBFile = new GridFSDBFile();
        try{
            GridFS gridFS = new GridFS(client.getDB("effects"));
            DBObject dbObject = new BasicDBObject("filename",name);
            gridFSDBFile= gridFS.findOne(dbObject);
        }catch (Exception e){
            e.printStackTrace();
        }
        return gridFSDBFile;
    }
}
