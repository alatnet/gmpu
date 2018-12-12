/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod;

import com.alatnet.gmod.logHandlers.SystemOutHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alatnet
 */

/*
gmpublsh create -addon <file> -icon <file.jpg>
gmpublush update -addon <file> -id <id> -changes <string>
gmpublish list

gmad.exe create -folder <folder> -out <file>
gmad.exe extract -file <file> [-out <folder>]
*/

public class GmodPublishingUtility {
    // <editor-fold defaultstate="collapsed" desc="Settings variables">
    //paths
    public static String gmadPath="";
    public static String gmad="gmad.exe";
    public static String gmpublishPath="";
    public static String gmpublish="gmpublish.exe";
    
    //gma creation
    public static boolean convertFilenames=false;
    
    //queue
    public static boolean Q_autoRemoveCompleted=false;
    
    //smart queue
    public static int SQ_uploadDelay=300; //5 minutes
    public static int SQ_uploadRetryTimes=5;
    public static int SQ_numThreads=5;
    public static boolean SQ_autoRemoveCompleted=false;
    public static boolean useSmartQueue=false;
    public static boolean SQ_uploadRetryDelayE=false;
    public static int SQ_uploadRetryDelayI=30;
    
    //misc
    public static boolean openBrowserOnCreate=false;
    public static boolean useJavaLAF=false;
    public static String javaLAF="Nimbus";
    
    //testing system
    public static boolean simulateSystem=false;
    public static boolean simulateSystemError=false;
    public static int simulateSystemWait=3000;
    // </editor-fold>
    
    //logger system
    public static final Logger gmpuLogger=Logger.getLogger("gmpuLog");
    private static final Level gmpuLoggerLevelOrig = Level.ALL;//gmpuLogger.getLevel();
    
    public static boolean unixSys=false;
    
    //hack? to see if on a unix system.
    static {
        if (File.separatorChar == '/'){
            gmad="gmad";
            gmpublish="gmpublish";
            unixSys=true;
        }
    }
    
    private static MainFrame mainFrame;
    
    //public static boolean enableLogging=true;
    
    private static void checkSettings(){
        Scanner scanner;
        try {
            scanner = new Scanner(new BufferedReader(new FileReader("gmpuSettings.settings")));
            
            //gmpu v1.0+ settings
            gmadPath = scanner.nextLine();
            gmpublishPath = scanner.nextLine();
            gmad = scanner.nextLine();
            gmpublish = scanner.nextLine();
            
            //gmpu v2.0 settings
            if (scanner.hasNext()){
                convertFilenames=scanner.nextBoolean();
                SQ_uploadDelay=scanner.nextInt();
                SQ_uploadRetryTimes=scanner.nextInt();
                SQ_numThreads=scanner.nextInt();
                SQ_autoRemoveCompleted=scanner.nextBoolean();
                Q_autoRemoveCompleted=scanner.nextBoolean();
                useSmartQueue=scanner.nextBoolean();
                openBrowserOnCreate=scanner.nextBoolean();
                useJavaLAF=scanner.nextBoolean();
                scanner.nextLine();
                javaLAF=scanner.nextLine();
            }
            
            //gmpu v2.2
            if (scanner.hasNext()){
                SQ_uploadRetryDelayE=scanner.nextBoolean();
                SQ_uploadRetryDelayI=scanner.nextInt();
            }
            
            scanner.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GmodPublishingUtility.class.getName()).log(Level.SEVERE, null, ex);
            gmpuLogger.log(Level.SEVERE, "Settings file not found.  Requesting Paths.");

            GmodPaths paths = new GmodPaths(null,true);
            paths.setVisible(true);

            if (paths.pressedOK()){
                gmadPath = paths.getGmadPath();
                gmpublishPath = paths.getGmpublishPath();
                gmad = paths.getGmadProgram();
                gmpublish = paths.getGmpublishProgram();
            }

            gmpuLogger.log(Level.INFO, "Paths set!");
        }
    }
    
    public static void saveSettings(){
        PrintWriter PW;
        try {
            PW = new PrintWriter(new BufferedWriter(new FileWriter("gmpuSettings.settings")));
            
            PW.println(gmadPath);
            PW.println(gmpublishPath);
            PW.println(gmad);
            PW.println(gmpublish);
            
            //gmpu v2.0
            PW.println(convertFilenames);
            PW.println(SQ_uploadDelay);
            PW.println(SQ_uploadRetryTimes);
            PW.println(SQ_numThreads);
            PW.println(SQ_autoRemoveCompleted);
            PW.println(Q_autoRemoveCompleted);
            PW.println(useSmartQueue);
            PW.println(openBrowserOnCreate);
            PW.println(useJavaLAF);
            PW.println(javaLAF);
            
            //gmpu v2.2
            PW.println(SQ_uploadRetryDelayE);
            PW.println(SQ_uploadRetryDelayI);

            PW.flush();
            PW.close();
            /*BW.flush();
            BW.close();*/
        } catch (IOException ex) {
            Logger.getLogger(GmodPublishingUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void checkDebug(){
        Scanner scanner;
        try {
            scanner = new Scanner(new BufferedReader(new FileReader("gmpuDebug.settings")));
            gmpuLogger.log(Level.CONFIG, "Debug settings found!");
            simulateSystem=scanner.nextBoolean();
            simulateSystemError=scanner.nextBoolean();
            simulateSystemWait=scanner.nextInt();
            scanner.close();
        } catch (FileNotFoundException ex) {
        }
    }
    
    private static void setupLogging(){
        gmpuLogger.setUseParentHandlers(false);
        gmpuLogger.setLevel(Level.ALL);
        gmpuLogger.addHandler(new SystemOutHandler());
        gmpuLogger.log(Level.CONFIG, "Console Logging Initialized.");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        setupLogging();
        checkDebug();
        checkSettings();
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        
        if (useJavaLAF){
            try {
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                    if (javaLAF.equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
                java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
        //</editor-fold>
        
        if (args.length == 0){
            mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        }else if (args[0].equals("/h") || args[0].equals("-h") || args[0].equals("--h") || args[0].equals("-help") || args[0].equals("--help")){
            // <editor-fold defaultstate="collapsed" desc="Command Line Help">
            System.out.println("Gmod Publishing Utility.");
            System.out.println("Usage:");
            System.out.println("  java -jar gmpu.jar <command> <arguments> [<command> <arguments> [<command> <arguments>[...]]]");
            System.out.println("  Commands:");
            System.out.println("    -s {gmpublish|gmad|gmpublishPath|gmadPath|paths} <file|folder>");
            System.out.println("    set {gmpublish|gmad|gmpublishPath|gmadPath|paths} <file|folder>");
            System.out.println("    -Set the program and paths to gmpublish and gmad.");
            System.out.println("    -l");
            System.out.println("    list");
            System.out.println("    -List all addons published to the workshop.");
            System.out.println("    -e <file.gma> [-o <folder>]");
            System.out.println("    extract <file.gma> [-o <folder>]");
            System.out.println("    -Extract a gma file.");
            System.out.println("    -u <folder|file.gma> <id> [-c <string>]");
            System.out.println("    update <folder|file.gma> <id> [-c <string>]");
            System.out.println("    -Update an addon in the workshop.");
            System.out.println("    -c <folder|file.gma> -icon <file.jpg>");
            System.out.println("    create <folder|file.gma> -icon <file.jpg>");
            System.out.println("    -Create and publish an addon.");
            System.out.println("    -c gma <folder> <file>");
            System.out.println("    create gma <folder> <file>");
            System.out.println("    -Create a gma file.");
            /*
             * commands:
             * set {gmpublish|gmad|gmpublishPath|gmadPath|paths} <file|folder>  *  Should work
             * extract <file.gma> [-o <folder>]     *  Works
             * list     *  Works
             * update <folder|file.gma> <id> [-c <string>]  *  Unknown
             * create <folder|file.gma> -icon <file.jpg>    *  Unknown
             * create gma <folder> <file>   *  Works
             */
            // </editor-fold>
        }else{      
            // <editor-fold defaultstate="collapsed" desc="Command Line Queue">
            Deque<gmpuJob> jobs=new ArrayDeque<>();
            
            Deque<String> argList=new ArrayDeque<>();
            for (int i=0; i<args.length;i++) argList.addLast(args[i]);
            
            boolean exitLoop=false;
            String addon;
            gmpuJob job=null;
            while (argList.size() != 0 && !exitLoop){
                switch (argList.pop()){
                    case "-l":
                    case "list":
                        // <editor-fold defaultstate="collapsed" desc="List">
                        jobs.addLast(new gmpuJob(gmpuJob.LIST));
                        break;
                        // </editor-fold>
                    case "-s":
                    case "set":
                        // <editor-fold defaultstate="collapsed" desc="Set Path">
                        switch (argList.pop()){
                            case "gmpublishPath":
                                gmpublishPath=argList.pop();
                                break;
                            case "gmadPath":
                                gmadPath=argList.pop();
                                break;
                            case "gmpublish":
                                gmpublish=argList.pop();
                                break;
                            case "gmad":
                                gmad=argList.pop();
                                break;
                            case "paths":
                                gmpublishPath=gmadPath=argList.pop();
                                break;
                            /*case "dialog":
                                break;*/
                        }
                        break;
                        // </editor-fold>
                    case "-e":
                    case "extract":
                        // <editor-fold defaultstate="collapsed" desc="Extract">
                        job = new gmpuJob(gmpuJob.EXTRACT);
                        job.extractName = argList.pop();
                        try{
                            if (argList.peek().equals("-o")){
                                argList.pop();
                                job.extractOut = argList.pop();
                            }
                        }catch(Exception e){
                        }
                        jobs.addLast(job);
                        job = null;
                        break;
                        // </editor-fold>
                    case "-u":
                    case "update":
                        // <editor-fold defaultstate="collapsed" desc="Update">
                        /*
                         * update:
                         * createGmaFolder - only when updating from a folder.
                         * createGmaOut - required
                         * updateID - required
                         * updateChanges - optional
                         */
                        job = new gmpuJob(gmpuJob.UPDATE);
                        addon = argList.pop();
                        if (addon.endsWith(".gma")){
                            job.createGmaOut = addon;
                        }else{
                            job.createGmaFolder = addon;
                            //job.createGmaOut = "temp.gma";
                        }
                        job.updateID=argList.pop();
                        if (argList.peek().equals("-c")){
                            argList.pop();
                            job.updateChanges = argList.pop();
                        }
                        jobs.addLast(job);
                        job = null;
                        break;
                        // </editor-fold>
                    case "-c":
                    case "create":
                        // <editor-fold defaultstate="collapsed" desc="Create">
                        /*
                         * create:
                         * createGmaFolder - only when creating directly from folder
                         * createGmaOut - required
                         * createIcon - required
                         */
                        try{
                            if (argList.peek().equals("gma")){
                                argList.pop();
                                job = new gmpuJob(gmpuJob.CREATE_GMA);
                                job.createGmaFolder = argList.pop();
                                job.createGmaOut = argList.pop();
                            }else{ //create <folder|file.gma> -icon <file.jpg>
                                job = new gmpuJob(gmpuJob.CREATE);
                                addon = argList.pop();
                                if (addon.endsWith(".gma")){
                                    job.createGmaOut = addon;
                                }else{
                                    job.createGmaFolder = addon;
                                    //job.createGmaOut = "temp.gma";
                                }
                                job.createIcon = argList.pop();
                            }
                        }catch(Exception ex){
                        }
                        
                        jobs.addLast(job);
                        job = null;
                        break;
                        // </editor-fold>
                }
            }
            
            // <editor-fold defaultstate="collapsed" desc="Queue Loop">
            while (jobs.size()!=0){
                gmpuJob gJob = jobs.pop();
                if (gJob.getJobType() == gmpuJob.LIST) gmpuLogger.setLevel(Level.WARNING);//gmpuLogger.setLevel(Level.OFF);
                try {
                    gJob.call();
                } catch (Exception ex) {
                    Logger.getLogger(GmodPublishingUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (gJob.getJobType() == gmpuJob.LIST){
                    gmpuLogger.setLevel(gmpuLoggerLevelOrig);
                    System.out.println("List of Addons in Workshop:");
                    for (int i=0;i<gJob.listReturn.size();i++){
                        gmpuJob.listEntry entry = gJob.listReturn.get(i);
                        System.out.println("["+i+"] ID: "+entry.ID+" -- Name: "+entry.Name);
                    }
                }
            }
            // </editor-fold>
            /*
             * commands:
             * set {gmpublish|gmad|gmpublishPath|gmadPath|paths} <file|folder>  *  Should work
             * extract <file.gma> [-o <folder>]     *  Works
             * list     *  Works
             * update <folder|file.gma> <id> [-c <string>]  *  works
             * create <folder|file.gma> -icon <file.jpg>    *  works
             * create gma <folder> <file>   *  Works
             */
            // </editor-fold>
        }
        saveSettings();
    }
}
