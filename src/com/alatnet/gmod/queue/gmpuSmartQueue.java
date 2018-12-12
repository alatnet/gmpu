/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod.queue;

import com.alatnet.gmod.GmodPublishingUtility;
import com.alatnet.gmod.gmpuJob;
import com.alatnet.gmod.tableModels.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 *
 * @author alatnet
 */
public class gmpuSmartQueue extends gmpuQueueRoot implements ActionListener {
    private tableModelQueue modelQueue;
    private tableModelAddons modelAddon;
    private boolean refreshAddons;
    
    private final Object waitNotifyObj=new Object();

    public gmpuSmartQueue(tableModelQueue model, tableModelAddons modelAddon){
        this.modelQueue = model;
        this.modelAddon = modelAddon;
    }
    
    public void alert(){
        synchronized(waitNotifyObj){
            waitNotifyObj.notify();
        }
    }
    
    @Override
    public void stopQueue(){
        super.stopQueue();
        this.alert();
    }

    @Override
    public void run(){
        timerActionListener secondTimerActionListener = new timerActionListener(GmodPublishingUtility.SQ_uploadDelay,this);
        Timer secondTimer = new Timer(1000,secondTimerActionListener);
        Timer threadWakeup = new Timer(250,this);
        boolean priorityWaiting = false;
        gmpuJob priorityJobWaiting = null;
        
        singleQueueThread activeThreads[] = new singleQueueThread[GmodPublishingUtility.SQ_numThreads];
        Deque<gmpuJob> priorityJobs=new ArrayDeque<>();
        Deque<gmpuJob> standardJobs=new ArrayDeque<>();
        
        
        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Executing queue.");
        
        //populate lists
        for (int i=0;i<activeThreads.length;i++) activeThreads[i] = null;
        
        for (int i=0;i<this.modelQueue.getRowCount();i++){
            gmpuJob job = this.modelQueue.getJob(i);
            switch (job.getJobType()){
                case gmpuJob.CREATE:
                    refreshAddons=true;
                case gmpuJob.UPDATE:
                    priorityJobs.add(job);
                    break;
                default:
                    standardJobs.add(job);
                    break;
            }
        }
        
        if (!priorityJobs.isEmpty()){ //has priority jobs.
            activeThreads[0] = new singleQueueThread(priorityJobs.pop(),this.modelQueue,this);
            if (!standardJobs.isEmpty())
                for (int i=1;i<activeThreads.length;i++)
                    if (!standardJobs.isEmpty())
                        activeThreads[i] = new singleQueueThread(standardJobs.pop(),this.modelQueue,this);
        }else{ //no priority jobs
            if (!standardJobs.isEmpty())
                for (int i=0;i<activeThreads.length;i++)
                    if (!standardJobs.isEmpty())
                        activeThreads[i] = new singleQueueThread(standardJobs.pop(),this.modelQueue,this);
        }
        
        for (int i=0;i<activeThreads.length;i++) if (activeThreads[i]!=null) activeThreads[i].start();

        boolean threadsDone = false;
        int prevWaitTime = -1;
        threadWakeup.start();
        while(!this.stopQueue && !threadsDone){
            threadsDone = true;
            if (priorityJobWaiting != null){ //if there is a priority job, update it's wait status
                int currTime = secondTimerActionListener.getTime();
                if (prevWaitTime != currTime){
                    modelQueue.setStatus(priorityJobWaiting, "Waiting: "+currTime);
                    prevWaitTime=currTime;
                } else if (currTime == 0) modelQueue.setStatus(priorityJobWaiting, "Waiting for Slot.");
            }
            for (int i=0;i<activeThreads.length;i++){
                if (activeThreads[i] != null){ //there is a thread
                    threadsDone=false;
                    if (!activeThreads[i].isAlive()){ //thread is done
                        if (activeThreads[i].isUploadThread() && !priorityWaiting){ //is it a priority job?
                            if (!priorityJobs.isEmpty()){ //has a priority job
                                priorityJobWaiting = priorityJobs.pop();
                                priorityWaiting = true;
                                secondTimer.start();
                                prevWaitTime=-1;
                            }else{ //no more priority jobs.
                                priorityJobWaiting = null;
                                priorityWaiting = false;
                                secondTimer.stop();
                                if (!standardJobs.isEmpty()){ //has a standard job
                                    activeThreads[i] = new singleQueueThread(standardJobs.pop(),this.modelQueue,this);
                                    activeThreads[i].start();
                                }else{ //no more jobs
                                    activeThreads[i] = null;
                                }
                            }
                        }else if (priorityWaiting && secondTimerActionListener.getTime() <= 0){ //is there a priority job waiting?
                            //add a priority thread
                            if (priorityJobWaiting != null){ //has a priority job
                                activeThreads[i] = new singleQueueThread(priorityJobWaiting,this.modelQueue,this);
                                priorityJobWaiting = null;
                                activeThreads[i].start();
                            }else{ //no more priority jobs
                                if (!standardJobs.isEmpty()){ //has a standard job
                                    activeThreads[i] = new singleQueueThread(standardJobs.pop(),this.modelQueue,this);
                                    activeThreads[i].start();
                                }else{ //no more jobs
                                    activeThreads[i] = null;
                                }
                            }
                            secondTimer.stop();
                            secondTimerActionListener.reset();
                            priorityWaiting = false;
                        }else{ //create a standard job
                            if (!standardJobs.isEmpty()){ //has a standard job
                                activeThreads[i] = new singleQueueThread(standardJobs.pop(),this.modelQueue,this);
                                activeThreads[i].start();
                            }else{ //no more jobs
                                activeThreads[i] = null;
                            }
                        }
                    }
                }
                if (threadsDone && priorityWaiting){
                    if (secondTimerActionListener.getTime() <= 0){
                        activeThreads[0] = new singleQueueThread(priorityJobWaiting,this.modelQueue,this);
                        activeThreads[0].start();
                        priorityJobWaiting = null;
                        secondTimer.stop();
                        secondTimerActionListener.reset();
                        priorityWaiting = false;
                    }
                    threadsDone = false;
                }
                if (threadsDone && !priorityJobs.isEmpty()){
                    priorityJobWaiting = priorityJobs.pop();
                    priorityWaiting = true;
                    threadsDone = false;
                    secondTimer.start();
                }
            }
            if (!threadsDone && !this.stopQueue){
                try {
                    synchronized(waitNotifyObj){
                        waitNotifyObj.wait();
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(gmpuSmartQueue.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        secondTimer.stop();
        threadWakeup.stop();
        
        for (int i=0;i<activeThreads.length;i++)
            if (activeThreads[i]!=null)
                activeThreads[i].stopExecution();
        
        if (this.stopQueue) GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Queue was stopped.");
        else GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Finished running queue.");
        
        if (refreshAddons){
            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Refreshing addon list.");
            gmpuJob listItems = new gmpuJob(gmpuJob.LIST);
            try {
                listItems.call();
            } catch (Exception ex) {
                Logger.getLogger(gmpuQueue.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.modelAddon.clearData();
            for (int i=0;i<listItems.listReturn.size();i++){
                gmpuJob.listEntry listEntry = listItems.listReturn.get(i);
                this.modelAddon.addRow(listEntry.ID,listEntry.Name);
            }
            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Finished refreshing addon list.");
        }
        
        if (this.queueBtn!=null) this.queueBtn.setText("Execute");
    }

    @Override
    public void actionPerformed(ActionEvent e) { this.alert(); }
    
    private class singleQueueThread extends Thread implements ActionListener {
        private boolean _stopExecution = false;
        private tableModelQueue _modelQueue;
        private gmpuJob job;
        private gmpuSmartQueue sq;
        private int retryCountdown=GmodPublishingUtility.SQ_uploadRetryDelayI;
        
        private final Object waitNotifyObj = new Object();
        
        public singleQueueThread(gmpuJob job, tableModelQueue modelQueue, gmpuSmartQueue sq){
            this.job = job;
            this._modelQueue = modelQueue;
            this.sq = sq;
        }
        
        public boolean isUploadThread(){
            switch(this.job.getJobType()){
                case gmpuJob.CREATE:
                case gmpuJob.UPDATE:
                    return true;
            }
            return false;
        }
        
        public void stopExecution(){
            this._stopExecution = true;
            synchronized(this.waitNotifyObj){
                this.waitNotifyObj.notify();
            }
        }
        
        @Override
        public void run() {
            int errorCode = 0;
            this._modelQueue.setStatus(job, "Running");
            this.sq.alert();
            Timer waitThread = new Timer(1000,this);
            
            if (job.getJobType() == gmpuJob.CREATE || job.getJobType() == gmpuJob.UPDATE){
                try {
                    errorCode = this.job.call();
                } catch (Exception ex) {
                    Logger.getLogger(gmpuSmartQueue.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (errorCode!=0){
                    if (GmodPublishingUtility.SQ_uploadRetryTimes != 0){
                        if (!this._stopExecution) for (int i=0;i<=GmodPublishingUtility.SQ_uploadRetryTimes-1;i++){
                            if (errorCode==0) break;
                            else{
                                waitThread.start();
                                if (GmodPublishingUtility.SQ_uploadRetryDelayE){
                                    while(this.retryCountdown>0){
                                        this._modelQueue.setStatus(job, "Retrying "+(i+1)+" of "+GmodPublishingUtility.SQ_uploadRetryTimes + ": "+this.retryCountdown);
                                        this.sq.alert();
                                        synchronized(this.waitNotifyObj){
                                            try {
                                                this.waitNotifyObj.wait();
                                            } catch (InterruptedException ex) {
                                                Logger.getLogger(gmpuSmartQueue.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                        if (this._stopExecution) break;
                                    }
                                    waitThread.stop();
                                    this.retryCountdown=GmodPublishingUtility.SQ_uploadRetryDelayI;
                                    this._modelQueue.setStatus(job, "Retrying "+(i+1)+" of "+GmodPublishingUtility.SQ_uploadRetryTimes);
                                    this.sq.alert();
                                }else{
                                    this._modelQueue.setStatus(job, "Retrying "+(i+1)+" of "+GmodPublishingUtility.SQ_uploadRetryTimes);
                                    this.sq.alert();
                                }
                                if (!this._stopExecution){
                                    try {
                                        errorCode = this.job.call();
                                    } catch (Exception ex) {
                                        Logger.getLogger(gmpuSmartQueue.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }

                            if (_stopExecution) break;
                            Thread.yield();
                        }
                    }
                }
            }else{
                try {
                    errorCode = this.job.call();
                } catch (Exception ex) {
                    Logger.getLogger(gmpuSmartQueue.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if (this._stopExecution){
                this._modelQueue.setStatus(job, "Stopped!");
                this.sq.alert();
            }
            else if (errorCode==0){
                this._modelQueue.setStatus(job, "Done!");
                this.sq.alert();
            } else {
                if (job.getJobType() == gmpuJob.CREATE){
                    if (!job.errorNumber.isEmpty()){
                        int gmpublishErrorCode = Integer.parseInt(job.errorNumber);
                        switch (gmpublishErrorCode){
                            case 16:
                                this._modelQueue.setStatus(job, "Timed Out! Exit Status("+errorCode+") GMPublish Error Code: "+gmpublishErrorCode);
                                this.sq.alert();
                                break;
                            default:
                                this._modelQueue.setStatus(job, "Error! Exit Status("+errorCode+") GMPublish Error Code: "+gmpublishErrorCode);
                                this.sq.alert();
                                break;
                        }
                    } else {
                        this._modelQueue.setStatus(job, "Error! Exit Status("+errorCode+")");
                        this.sq.alert();
                    }
                } else {
                    this._modelQueue.setStatus(job, "Error! Exit Status("+errorCode+")");
                    this.sq.alert();
                }
            }
            if (GmodPublishingUtility.SQ_autoRemoveCompleted) modelQueue.removeJob(this.job);
            this.sq.alert();
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (GmodPublishingUtility.SQ_uploadRetryDelayE){
                this.retryCountdown--;
                if (this.retryCountdown < 0) this.retryCountdown=0;
                synchronized(this.waitNotifyObj){
                    this.waitNotifyObj.notify();
                }
            }
        }
    }
    
    private class timerActionListener implements ActionListener {
        private int time=0,setTime=0;
        private final Object timeAccess = new Object();
        private gmpuSmartQueue sq;
        
        public timerActionListener(int time, gmpuSmartQueue sq){
            this.time=this.setTime=time;
            this.sq=sq;
        }
        
        public int getTime(){
            synchronized(timeAccess){
                return time;
            }
        }
        
        public int reset(){
            synchronized(timeAccess){
                int prev=time;
                time=setTime;
                return prev;
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized(timeAccess){
                time--;
                if (this.time<=0) this.time=0;
            }
            this.sq.alert();
        }
    }
}
