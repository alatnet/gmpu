/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author alatnet
 */

//<type>:
//"gamemode"
//"map"
//"weapon"
//"vehicle"
//"npc"
//"tool"
//"effects"
//"model"
//"servercontent"
//
//<tags> (only two):
//"fun"
//"roleplay"
//"scenic"
//"movie"
//"realism"
//"cartoon"
//"water"
//"comic"
//"build"

public class addon_json {
    public static final int
            CHK_OK=0,
            CHK_TITLE=1, //title is empty
            CHK_TYPE_A=2, //type is empty
            CHK_TYPE_B=3, //type is wrong
            CHK_TAGS_A=4, //tags are empty
            CHK_TAGS_B_1=5, //first tag is wrong
            CHK_TAGS_B_2=6, //second tag is wrong
            CHK_TAGS_B_3=7; //both tags are wrong
    private static final String[] TYPE = {"gamemode", "map","weapon","vehicle","npc","tool","effects","model","servercontent"};
    private static final String[] TAGS = {"fun","roleplay","scenic","movie","realism","cartoon","water","comic","build"};
    public String title="";
    public String type="";
    public String[] tags={"",""};
    public String ignore;
    
    public void write(String path) throws IOException{
        try (BufferedWriter BW = new BufferedWriter(new FileWriter(path))) {
            BW.write(this.toString());
        }
    }
    
    public int checkData(){
        if (this.title.isEmpty()) return CHK_TITLE;
        
        if (!this.type.isEmpty()){
            boolean typeOK=false;
            for (int i=0;i<TYPE.length;i++){
                if (this.type.equals(TYPE[i])){
                    typeOK=true;
                    break;
                }
            }
            if (!typeOK) return CHK_TYPE_B;
        }else return CHK_TYPE_A;
        
        boolean tag1OK=false,tag2OK=false;
        if (!this.tags[0].isEmpty() && !this.tags[1].isEmpty()){
            for (int i=0;i<TAGS.length;i++){
                if (tag1OK && tag2OK) break;
                if (this.tags[0].equals(TAGS[i])) tag1OK=true;
                if (this.tags[1].equals(TAGS[i])) tag2OK=true;
            }
            if (!tag1OK && tag2OK) return CHK_TAGS_B_1;
            else if (tag1OK && !tag2OK) return CHK_TAGS_B_2;
            else if (!tag1OK && !tag2OK) return CHK_TAGS_B_3;
        }else if (!this.tags[0].isEmpty() && this.tags[1].isEmpty()){
            for (int i=0;i<TAGS.length;i++){
                if (this.tags[0].equals(TAGS[i])){
                    tag1OK=true;
                    break;
                }
            }
            if (!tag1OK) return CHK_TAGS_B_1;
        }else return CHK_TAGS_A;
        
        return CHK_OK;
    }
    
    @Override
    public String toString(){
        String ret="";
        
        ret+="\"title\" : \""+this.title+"\",\n";
        ret+="\"type\" : \""+this.type+"\",\n";
        
        if (!this.tags[0].isEmpty() && !this.tags[1].isEmpty()) ret+="\"tags\" : [\""+this.tags[0] + "\",\""+this.tags[1] + "\"],\n";
        else if(!this.tags[0].isEmpty() && this.tags[1].isEmpty()) ret+="\"tags\" : \""+this.tags[0] + "\",\n";

        if (!this.ignore.isEmpty()) ret+="\"ignore\" : ["+this.ignore+"]\n";
        
        return "{\n" + ret +"}";
    }
}
