/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.util.concurrent.Callable;

/**
 *
 * @author alatnet
 */
public class gmpuJob implements Callable<Integer>{
    public static final int LIST=0,EXTRACT=1,UPDATE=2,CREATE=3,CREATE_GMA=4;
    
    public String extractName="";
    public String extractOut="";
    
    public String createGmaFolder="";
    public String createGmaOut="";
    
    public String createIcon="";
    
    public String updateID="";
    public String updateChanges="";
    
    public addon_json addonJson=null;
    
    public String errorNumber="";
    
    private int jobType=0;
    
    public class listEntry{
        public String ID;
        public String Name;
        
        public listEntry(String ID, String Name){
            this.ID = ID;
            this.Name = Name;
        }
    }
    
    public ArrayList<listEntry> listReturn=null;
    
    public gmpuJob(int jobType){
        this.jobType=jobType;
    }
    
    public int getJobType(){ return this.jobType; }
    
    @Override
    public String toString(){
        switch (this.jobType){
            case LIST:
                return "Get addon listing.";
            case EXTRACT:
                return "Extraction of \""+this.extractName+"\".";
            case UPDATE:
                return "Update of addon id \""+this.updateID+"\".";
            case CREATE:
                String s="";
                if (!this.createGmaFolder.isEmpty()) s=this.createGmaFolder;
                else s=this.createGmaOut;
                return "Creating addon from \""+s+"\"";
            case CREATE_GMA:
                return "Creating GMA file from \""+this.createGmaFolder+"\"";
        }
        return "Unknown.";
    }
    
    private void filenames2lowercase(File f){
        if (f.isDirectory()){
            File[] files = f.listFiles();
            for (int i=0;i<files.length;i++) filenames2lowercase(files[i]);
            f.renameTo(new File(f.toString().toLowerCase()));
        }else f.renameTo(new File(f.toString().toLowerCase()));
    }
    
    private String buildExecutablePath(String path, String executable){
        if (!GmodPublishingUtility.unixSys){ //windows
            return "\"" + path + File.separatorChar + executable + "\"";
        }
        return path + File.separatorChar + executable; //linux
    }
    
    @Override
    public Integer call() throws Exception {
        int exitStatus=0,createGMAExitStatus=0;
        ProcessBuilder pb;
        Process p;
        String s="";

        BufferedReader stdout;
        String stdoutStr="";

        boolean createGMA=false;

        switch (this.jobType){
            case LIST:
                // <editor-fold defaultstate="collapsed" desc="List"> 
                try {
                    this.listReturn=new ArrayList<>();
                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Retreving list of Addons.");
                    //pb = new ProcessBuilder("\""+GmodPublishingUtility.gmpublishPath+File.separatorChar+GmodPublishingUtility.gmpublish+"\"","list");
                    pb = new ProcessBuilder(this.buildExecutablePath(GmodPublishingUtility.gmpublishPath, GmodPublishingUtility.gmpublish),"list");
                    pb.directory(new File(GmodPublishingUtility.gmpublishPath));
                    p = pb.start();
                    stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    stdoutStr = stdout.readLine();

                    Pattern idPatt = Pattern.compile("\\d+");
                    Pattern namePatt = Pattern.compile("\".+\"");
                    while (stdoutStr != null){
                        Matcher idMatch = idPatt.matcher(stdoutStr);
                        Matcher nameMatch = namePatt.matcher(stdoutStr);

                        if (idMatch.find() && nameMatch.find()){
                            this.listReturn.add(new listEntry(idMatch.group(0),nameMatch.group(0)));
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "ID: "+idMatch.group(0)+"\tName: "+nameMatch.group(0));
                        }
                        //GmodPublishingUtility.gmpuLogger.log(Level.INFO, stdoutStr);
                        stdoutStr = stdout.readLine();
                    }
                    p.waitFor();
                    stdout.close();
                    
                    exitStatus = p.exitValue();
                    if (exitStatus==0) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Retrieval Finished!");
                    else GmodPublishingUtility.gmpuLogger.log(Level.WARNING, "Retrieval Failed!");
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(gmpuJob.class.getName()).log(Level.SEVERE, null, ex);
                    GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, "Retrieval Errored!");
                    GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, null, ex);
                }
                break;
                // </editor-fold>
            case EXTRACT:
                // <editor-fold defaultstate="collapsed" desc="Extract">
                if (!GmodPublishingUtility.simulateSystem){
                    try {
                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Extracting " + this.extractName + ".");
                        //s = "\""+GmodPublishingUtility.gmadPath+File.separatorChar+GmodPublishingUtility.gmad;//+"\" extract -file"+this.extractName;
                        //s = "\""+GmodPublishingUtility.gmadPath+File.separatorChar+GmodPublishingUtility.gmad+"\"";
                        s = this.buildExecutablePath(GmodPublishingUtility.gmadPath, GmodPublishingUtility.gmad);
                        if (this.extractOut.length()!=0){
                            //s+=" -out "+this.extractOut;
                            pb = new ProcessBuilder(s,"extract","-file",this.extractName,"-out",this.extractOut);
                        }else{
                            pb = new ProcessBuilder(s,"extract","-file",this.extractName);
                        }
                        pb.directory(new File(GmodPublishingUtility.gmadPath));
                        //p = r.exec(s, null, new File(GmodPublishingUtility.gmadPath));

                        p = pb.start();

                        stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        stdoutStr = stdout.readLine();
                        while (stdoutStr != null){
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, stdoutStr);
                            stdoutStr = stdout.readLine();
                        }

                        p.waitFor();
                        stdout.close();

                        exitStatus = p.exitValue();
                        if (exitStatus==0) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Extract OK!");
                        else GmodPublishingUtility.gmpuLogger.log(Level.WARNING, "Extracting Failed!");
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(gmpuJob.class.getName()).log(Level.SEVERE, null, ex);
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, "Extracting Errored!");
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, null, ex);
                    }
                }else{
                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Extract...");
                    Thread.sleep(GmodPublishingUtility.simulateSystemWait);
                    if (GmodPublishingUtility.simulateSystemError){
                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Extract Error!");
                        exitStatus = -1;
                    }else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Extract Done!");
                }
                break;
                // </editor-fold>
            case UPDATE:
                // <editor-fold defaultstate="collapsed" desc="Update">
                /*
                 * update:
                 * createGmaFolder - only when updating from a folder.
                 * createGmaOut - required
                 * updateID - required
                 * updateChanges - optional
                 */
                if (!GmodPublishingUtility.simulateSystem){
                    try {
                        //create gma
                        //gmpublush update -addon <file> -id <id> -changes <string>

                        if (!this.createGmaFolder.isEmpty()){
                            createGMA = true;
                            if (this.addonJson != null){
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creating temporary addon.json.");
                                this.addonJson.write(this.createGmaFolder+File.separatorChar+"addon.json");
                            }

                            if (GmodPublishingUtility.convertFilenames){
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Converting filenames to lowercase...");
                                filenames2lowercase(new File(this.createGmaFolder));
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Done!");
                            }

                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creating temporary gma.");
                            this.createGmaOut="temp.gma";
                            //s = "\""+GmodPublishingUtility.gmadPath+File.separatorChar+GmodPublishingUtility.gmad + "\"";
                            s = this.buildExecutablePath(GmodPublishingUtility.gmadPath,GmodPublishingUtility.gmad);
                            pb = new ProcessBuilder(s,"create","-folder",this.createGmaFolder,"-out",this.createGmaOut);
                            pb.directory(new File(GmodPublishingUtility.gmadPath));
                            //p = r.exec("\""+GmodPublishingUtility.gmadPath+File.pathSeparator+GmodPublishingUtility.gmad + "\" create -folder "+this.createGmaFolder+ " -out "+this.createGmaOut, null, new File(GmodPublishingUtility.gmadPath));
                            p = pb.start();

                            stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            stdoutStr = stdout.readLine();
                            while (stdoutStr != null){
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, stdoutStr);
                                stdoutStr = stdout.readLine();
                            }

                            p.waitFor();
                            stdout.close();
                            createGMAExitStatus = p.exitValue();
                        }

                        if (createGMAExitStatus==0){
                            //s="\""+GmodPublishingUtility.gmpublishPath+File.pathSeparator+GmodPublishingUtility.gmpublish+"\" update -addon "+this.createGmaOut+" -id "+this.updateID;
                            //s="\""+GmodPublishingUtility.gmpublishPath+File.separatorChar+GmodPublishingUtility.gmpublish+"\"";
                            s=this.buildExecutablePath(GmodPublishingUtility.gmpublishPath, GmodPublishingUtility.gmpublish);
                            if (this.updateChanges.length()==0){
                                pb = new ProcessBuilder(s,"update","-addon",this.createGmaOut,"-id",this.updateID);
                            }else{
                                pb = new ProcessBuilder(s,"update","-addon",this.createGmaOut,"-id",this.updateID,"-changes",this.updateChanges);
                            }
                            pb.directory(new File(GmodPublishingUtility.gmpublishPath));
                            //p = r.exec(s, null, new File(GmodPublishingUtility.gmpublishPath));
                            p = pb.start();

                            stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            stdoutStr = stdout.readLine();
                            while (stdoutStr != null){
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, stdoutStr);
                                stdoutStr = stdout.readLine();
                            }

                            p.waitFor();
                            stdout.close();
                            exitStatus = p.exitValue();
                        }

                        if (createGMA && createGMAExitStatus==0){
                            //delete temporary gma file and addon.json
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Deleting temporary gma...");
                            File temp_gma = new File(GmodPublishingUtility.gmadPath+File.separatorChar+"temp.gma");
                            if (temp_gma.delete()) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Temporary gma deleted.");
                            else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Error deleting temporary gma.");
                        }
                        if (this.addonJson!=null && !this.createGmaFolder.isEmpty()){
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Deleting temporary addon.json...");
                            File json = new File(this.createGmaFolder+File.separatorChar+"addon.json");
                            if (json.delete()) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Temporary addon.json deleted.");
                            else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Error deleting temporary addon.json.");
                        }

                        if (exitStatus==0 && createGMAExitStatus==0) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Update OK!");
                        else GmodPublishingUtility.gmpuLogger.log(Level.WARNING, "Update Failed!");
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(gmpuJob.class.getName()).log(Level.SEVERE, null, ex);
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, "Update Errored!");
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, null, ex);
                    }
                }else{
                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Update...");
                    Thread.sleep(GmodPublishingUtility.simulateSystemWait);
                    if (GmodPublishingUtility.simulateSystemError){
                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Update Error!");
                        exitStatus = -1;
                    }else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Update Done!");
                }
                break;
                // </editor-fold>
            case CREATE:
                // <editor-fold defaultstate="collapsed" desc="Create">
                /*
                 * create:
                 * createGmaFolder - only when creating directly from folder
                 * createGmaOut - required
                 * createIcon - required
                 */
                if (!GmodPublishingUtility.simulateSystem){
                    try {
                        //create gma
                        //gmpublsh create -addon <file> -icon <file.jpg>
                        String logOutStr = "Creating addon from ";
                        if (this.createGmaFolder.length()!=0){
                            logOutStr+=this.createGmaFolder;
                        }else{
                            logOutStr+=this.createGmaOut;
                        }
                        logOutStr+="...";
                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, logOutStr);

                        BufferedImage image = ImageIO.read(new File(this.createIcon));
                        if (image.getHeight()==512 || image.getWidth()==512){
                            if (!this.createGmaFolder.isEmpty()){
                                createGMA = true;
                                if (this.addonJson != null){
                                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creating temporary addon.json.");
                                    this.addonJson.write(this.createGmaFolder+File.separatorChar+"addon.json");
                                }

                                if (GmodPublishingUtility.convertFilenames){
                                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Converting filenames to lowercase...");
                                    filenames2lowercase(new File(this.createGmaFolder));
                                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Done!");
                                }

                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creating temporary gma.");
                                this.createGmaOut="temp.gma";
                                //s = "\""+GmodPublishingUtility.gmadPath+File.separatorChar+GmodPublishingUtility.gmad + "\"";
                                s = this.buildExecutablePath(GmodPublishingUtility.gmadPath, GmodPublishingUtility.gmad);
                                pb = new ProcessBuilder(s,"create","-folder",this.createGmaFolder,"-out",this.createGmaOut);
                                pb.directory(new File(GmodPublishingUtility.gmadPath));
                                //p = r.exec("\""+GmodPublishingUtility.gmadPath+File.pathSeparator+GmodPublishingUtility.gmad + "\" create -folder "+this.createGmaFolder+ " -out "+this.createGmaOut, null, new File(GmodPublishingUtility.gmadPath));
                                p = pb.start();

                                stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                stdoutStr = stdout.readLine();
                                while (stdoutStr != null){
                                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, stdoutStr);
                                    stdoutStr = stdout.readLine();
                                }

                                p.waitFor();
                                stdout.close();
                                createGMAExitStatus = p.exitValue();
                            }


                            String addonID="";
                            if (createGMAExitStatus==0){
                                //s="\""+GmodPublishingUtility.gmpublishPath+File.separatorChar+GmodPublishingUtility.gmpublish+"\"";
                                s=this.buildExecutablePath(GmodPublishingUtility.gmpublishPath, GmodPublishingUtility.gmpublish);
                                pb = new ProcessBuilder(s,"create","-addon",this.createGmaOut,"-icon",this.createIcon);
                                pb.directory(new File(GmodPublishingUtility.gmpublishPath));
                                //p = r.exec("\""+GmodPublishingUtility.gmpublishPath+File.pathSeparator+GmodPublishingUtility.gmpublish+"\" create -addon "+this.createGmaOut+" -icon "+this.createIcon, null, new File(GmodPublishingUtility.gmpublishPath));
                                p = pb.start();

                                stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                stdoutStr = stdout.readLine();
                                Pattern numberPatt = Pattern.compile("\\d+");
                                
                                while (stdoutStr != null){
                                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, stdoutStr);

                                    //get addon id.
                                    if (stdoutStr.contains("UID:")){
                                        Matcher numberMatch = numberPatt.matcher(stdoutStr);
                                        if (numberMatch.find()) addonID = numberMatch.group(0);
                                    }else if (stdoutStr.contains("PublishWorkshopFile failed!")){
                                        Matcher numberMatch = numberPatt.matcher(stdoutStr);
                                        if (numberMatch.find()) errorNumber = numberMatch.group(0);
                                    }

                                    stdoutStr = stdout.readLine();
                                }

                                p.waitFor();
                                stdout.close();
                                exitStatus = p.exitValue();
                            }

                            if (createGMA && createGMAExitStatus==0){
                                //delete temporary gma file and addon.json
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Deleting temporary gma...");
                                File temp_gma = new File(GmodPublishingUtility.gmadPath+File.separatorChar+"temp.gma");
                                if (temp_gma.delete()) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Temporary gma deleted.");
                                else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Error deleting temporary gma.");
                            }
                            if (this.addonJson!=null && !this.createGmaFolder.isEmpty()){
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Deleting temporary addon.json...");
                                File json = new File(this.createGmaFolder+File.separatorChar+"addon.json");
                                if (json.delete()) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Temporary addon.json deleted.");
                                else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Error deleting temporary addon.json.");
                            }
                            if (exitStatus==0){
                                GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creation OK!");
                                if (GmodPublishingUtility.openBrowserOnCreate)
                                    if (!addonID.equals(""))
                                        Desktop.getDesktop().browse(java.net.URI.create("http://steamcommunity.com/sharedfiles/filedetails/?id="+addonID));
                            } else GmodPublishingUtility.gmpuLogger.log(Level.WARNING, "Creation Failed!");
                        }else GmodPublishingUtility.gmpuLogger.log(Level.WARNING, "Icon Image is not 512x512! Creation of addon aborted!");
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(gmpuJob.class.getName()).log(Level.SEVERE, null, ex);
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, "Creation Errored!");
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, null, ex);
                    }
                }else{
                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Create...");
                    Thread.sleep(GmodPublishingUtility.simulateSystemWait);
                    if (GmodPublishingUtility.simulateSystemError){
                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Create Error!");
                        exitStatus = -1;
                    }else{
                        Desktop.getDesktop().browse(java.net.URI.create("http://steamcommunity.com/"));
                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Create Done!");
                    }
                }
                break;
                // </editor-fold>
            case CREATE_GMA:
                // <editor-fold defaultstate="collapsed" desc="Create GMA">
                if (!GmodPublishingUtility.simulateSystem){
                    try {
                        if (this.addonJson != null){
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creating temporary addon.json.");
                            this.addonJson.write(this.createGmaFolder+File.separatorChar+"addon.json");
                        }

                        if (GmodPublishingUtility.convertFilenames){
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Converting filenames to lowercase...");
                            filenames2lowercase(new File(this.createGmaFolder));
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Done!");
                        }

                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creating GMA from "+this.createGmaFolder);
                        //s = "\""+GmodPublishingUtility.gmadPath+File.separatorChar+GmodPublishingUtility.gmad + "\"";
                        s = this.buildExecutablePath(GmodPublishingUtility.gmadPath, GmodPublishingUtility.gmad);
                        pb = new ProcessBuilder(s,"create","-folder",this.createGmaFolder,"-out",this.createGmaOut);
                        pb.directory(new File(GmodPublishingUtility.gmadPath));
                        //p = r.exec("\""+GmodPublishingUtility.gmadPath+File.pathSeparator+GmodPublishingUtility.gmad + "\" create -folder "+this.createGmaFolder+ " -out "+this.createGmaOut, null, new File(GmodPublishingUtility.gmadPath));
                        p = pb.start();

                        stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        stdoutStr = stdout.readLine();
                        while (stdoutStr != null){
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, stdoutStr);
                            stdoutStr = stdout.readLine();
                        }

                        p.waitFor();
                        stdout.close();

                        if (this.addonJson!=null){
                            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Deleting temporary addon.json...");
                            File json = new File(this.createGmaFolder+File.separatorChar+"addon.json");
                            if (json.delete()) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Temporary addon.json deleted.");
                            else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Error deleting temporary addon.json.");
                        }

                        exitStatus = p.exitValue();
                        if (exitStatus==0) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Creation of GMA OK!");
                        else GmodPublishingUtility.gmpuLogger.log(Level.WARNING, "Creation of GMA Failed!");
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(gmpuJob.class.getName()).log(Level.SEVERE, null, ex);
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, "Creation of GMA Errored!");
                        GmodPublishingUtility.gmpuLogger.log(Level.SEVERE, null, ex);
                    }
                }else{
                    GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Create GMA...");
                    Thread.sleep(GmodPublishingUtility.simulateSystemWait);
                    if (GmodPublishingUtility.simulateSystemError){
                        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Create GMA Error!");
                        exitStatus = -1;
                    }else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Simulating Create GMA Done!");
                }
                break;
                // </editor-fold>
        }
        
        return exitStatus;
    }
}
