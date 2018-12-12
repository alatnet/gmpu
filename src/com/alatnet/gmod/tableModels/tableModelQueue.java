/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod.tableModels;

import com.alatnet.gmod.gmpuJob;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author alatnet
 */
public class tableModelQueue extends AbstractTableModel {
    public static int ACTION=0,JOB=1,STATUS=2;
    private static final String[] COLUMN_NAMES = {"Action", "Job", "Status"};
    private final Object tableAccess = new Object();
    
    public static class queueRowData {
        String Action;
        gmpuJob Job;
        String Status;
        
        public queueRowData(String Action, gmpuJob Job, String Status){
            this.Action = Action;
            this.Job = Job;
            this.Status = Status;
        }
    }
    
    private ArrayList<queueRowData> tableData;
    
    public tableModelQueue(){ this.tableData = new ArrayList<>(); }
    
    @Override
    public int getRowCount() { return this.tableData.size(); }

    @Override
    public int getColumnCount() { return 3; }

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= this.tableData.size()) return null;
        queueRowData data = this.tableData.get(row);
        
        switch (col){
            case 0:
                return data.Action;
            case 1:
                return data.Job;
            case 2:
                return data.Status;
        }
        
        return null;
    }
    
    public gmpuJob getJob(int row){ return this.tableData.get(row).Job; }
    
    public void setStatus(int row, String status){
        synchronized(tableAccess){
            this.tableData.get(row).Status=status;
            fireTableCellUpdated(row,2);
        }
    }
    
    public void setStatus(gmpuJob job, String status){
        synchronized(tableAccess){
            int row = this.findJob(job);
            if (row != -1){
                this.tableData.get(row).Status=status;
                fireTableCellUpdated(row,2);
            }
        }
    }
    
    public void addJob(gmpuJob job){
        String jobType;
        switch(job.getJobType()){
            case gmpuJob.CREATE:
                jobType="Create";
                break;
            case gmpuJob.CREATE_GMA:
                jobType="Create GMA";
                break;
            case gmpuJob.EXTRACT:
                jobType="Extract";
                break;
            case gmpuJob.LIST:
                jobType="List";
                break;
            case gmpuJob.UPDATE:
                jobType="Update";
                break;
            default:
                jobType="Unknown";
                break;
        }
        synchronized(tableAccess){
            this.tableData.add(new queueRowData(jobType,job,"Waiting"));
            fireTableRowsInserted(this.tableData.size(),this.tableData.size());
        }
    }
    
    public void removeRow(int row) throws ArrayIndexOutOfBoundsException {
        if (row < 0 || row >= this.tableData.size()) throw new ArrayIndexOutOfBoundsException(row);
        
        synchronized(tableAccess){
            this.tableData.remove(row);
            fireTableRowsDeleted(row,row);
        }
    }
    
    public void removeJob(gmpuJob job){
        synchronized(tableAccess){
            int i=this.findJob(job);
            if (i != -1){
                this.tableData.remove(i);
                fireTableRowsDeleted(i,i);
            }
        }
    }
    
    private int findJob(gmpuJob job){
        for (int i=0,i2=this.tableData.size()-1;i<this.tableData.size() && i2>=0;i++,i2--){ //setup variables to check in two directions.
            if (this.tableData.get(i).Job == job) return i; //look going towards max size.
            else if (this.tableData.get(i2).Job == job) return i2; //look going towards 0.
            else if (i>=i2) break; //if we cant find the data in either direction, break out.
        }
        return -1;
    }
    
    @Override
    public String getColumnName(int col){ return COLUMN_NAMES[col]; }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex){ return false; }
    
    public void clearData(){
        synchronized(tableAccess){
            int maxSize = this.tableData.size();
            this.tableData.clear();
            fireTableRowsDeleted(0,maxSize-1);
        }
    }
}
