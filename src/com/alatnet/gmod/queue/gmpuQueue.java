/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod.queue;

import com.alatnet.gmod.GmodPublishingUtility;
import com.alatnet.gmod.gmpuJob;
import java.util.logging.Level;
import com.alatnet.gmod.tableModels.*;
import java.util.logging.Logger;
/**
 *
 * @author alatnet
 */
public class gmpuQueue extends gmpuQueueRoot {
    private tableModelQueue modelQueue;
    private tableModelAddons modelAddon;

    public gmpuQueue(tableModelQueue modelQueue, tableModelAddons modelAddon){
        this.modelQueue = modelQueue;
        this.modelAddon = modelAddon;
    }

    @Override
    public void run(){
        boolean refreshAddons = false;
        int exitStatus = 0;

        GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Executing queue.");

        if (GmodPublishingUtility.Q_autoRemoveCompleted){
            while (this.modelQueue.getRowCount()>0 && !this.stopQueue){
                exitStatus = 0;
                this.modelQueue.setStatus(0, "Running");
                gmpuJob job = (gmpuJob) this.modelQueue.getJob(0);
                if (job.getJobType() == gmpuJob.CREATE) refreshAddons = true;
                try {
                    exitStatus = job.call();
                } catch (Exception ex) {
                    Logger.getLogger(gmpuQueue.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (exitStatus==0) this.modelQueue.setStatus(0, "Done!");
                else this.modelQueue.setStatus(0, "Error! Exit Status("+exitStatus+")");
                this.modelQueue.removeRow(0);
            }
        }else{
            for (int i=0;i<this.modelQueue.getRowCount();i++){
                exitStatus = 0;
                this.modelQueue.setStatus(i, "Running");
                gmpuJob job = (gmpuJob) this.modelQueue.getJob(i);
                if (job.getJobType() == gmpuJob.CREATE) refreshAddons = true;
                try {
                    exitStatus = job.call();
                } catch (Exception ex) {
                    Logger.getLogger(gmpuQueue.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (exitStatus==0) this.modelQueue.setStatus(i, "Done!");
                else this.modelQueue.setStatus(i, "Error! Exit Status("+exitStatus+")");
                if (this.stopQueue) break;
            }
        }

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
            //DefaultTableModel addonTblModel = (DefaultTableModel) tblAddonList.getModel();
            //while (this.modelAddon.getRowCount()>0) this.modelAddon.removeRow(0); //clear the addon list
            this.modelAddon.clearData();
            for (int i=0;i<listItems.listReturn.size();i++){
                gmpuJob.listEntry listEntry = listItems.listReturn.get(i);
                this.modelAddon.addRow(listEntry.ID,listEntry.Name);
            }
            GmodPublishingUtility.gmpuLogger.log(Level.INFO, "Finished refreshing addon list.");
        }
        
        if (this.queueBtn!=null) this.queueBtn.setText("Execute");
    }
}
