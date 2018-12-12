/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod.logHandlers;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author alatnet
 */
public class SystemOutHandler extends Handler {

    @Override
    public void publish(LogRecord lr) {
        if (lr.getLevel() != Level.INFO){
            System.out.println("[" + lr.getLevel().getLocalizedName() + "] " + lr.getMessage());
            Throwable thrown = lr.getThrown();
            if (thrown != null){
                String tMsg = thrown.getMessage();
                if (tMsg!=null && !tMsg.isEmpty()) System.out.println(tMsg);
            }
        }else{
            System.out.println(lr.getMessage());
            Throwable thrown = lr.getThrown();
            if (thrown != null){
                String tMsg = thrown.getMessage();
                if (tMsg!=null && !tMsg.isEmpty()) System.out.println(tMsg);
            }
        }
    }

    @Override
    public void flush() { System.out.flush(); }

    @Override
    public void close() throws SecurityException {}
    
}
