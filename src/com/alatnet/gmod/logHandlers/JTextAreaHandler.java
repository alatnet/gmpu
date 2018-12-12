/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod.logHandlers;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.JTextArea;

/**
 *
 * @author alatnet
 */
public class JTextAreaHandler extends Handler {
    JTextArea jTxtArea;
    
    public JTextAreaHandler(JTextArea jTxtArea){ this.jTxtArea=jTxtArea; }

    @Override
    public void publish(LogRecord lr) {
        if (lr.getLevel() != Level.INFO){
            jTxtArea.append("[" + lr.getLevel().getLocalizedName() + "] " + lr.getMessage() + "\n");
            Throwable thrown = lr.getThrown();
            if (thrown != null){
                String tMsg = thrown.getMessage();
                if (tMsg!=null && !tMsg.isEmpty()) jTxtArea.append(tMsg + "\n");
            }
        }else{
            jTxtArea.append(lr.getMessage() + "\n");
            Throwable thrown = lr.getThrown();
            if (thrown != null){
                String tMsg = thrown.getMessage();
                if (tMsg!=null && !tMsg.isEmpty()) jTxtArea.append(tMsg + "\n");
            }
        }
        jTxtArea.setCaretPosition(jTxtArea.getText().length());
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
}
