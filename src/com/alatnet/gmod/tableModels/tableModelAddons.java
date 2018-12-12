/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod.tableModels;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author alatnet
 */
public class tableModelAddons extends AbstractTableModel {
    public static int ID=0,NAME=1;
    private static final String[] COLUMN_NAMES = {"ID", "Name"};
    
    public static class addonsRowData {
        public String ID,Name;
        
        public addonsRowData(String ID, String Name){
            this.ID = ID;
            this.Name = Name;
        }
    }
    
    private ArrayList<addonsRowData> tableData;
    
    public tableModelAddons(){ this.tableData = new ArrayList<>(); }
    
    @Override
    public int getRowCount() { return this.tableData.size(); }

    @Override
    public int getColumnCount() { return 2; }

    @Override
    public Object getValueAt(int row, int col) {
        addonsRowData data = this.tableData.get(row);
        if (data!=null){
            switch (col){
                case 0:
                    return data.ID;
                case 1:
                    return data.Name;
            }
        }
        
        return null;
    }
    
    @Override
    public String getColumnName(int col){ return COLUMN_NAMES[col]; }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex){ return false; }
    
    public void addRow(addonsRowData data){
        this.tableData.add(data);
        fireTableRowsInserted(this.tableData.size(),this.tableData.size());
    }
    
    public void addRow(String Id, String Name){
        this.tableData.add(new addonsRowData(Id,Name));
        fireTableRowsInserted(this.tableData.size(),this.tableData.size());
    }
    
    public void clearData(){
        int maxSize = this.tableData.size();
        this.tableData.clear();
        fireTableRowsDeleted(0,maxSize-1);
    }
}
