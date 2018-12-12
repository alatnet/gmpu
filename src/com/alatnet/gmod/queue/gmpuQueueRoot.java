/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alatnet.gmod.queue;

import javax.swing.JButton;

/**
 *
 * @author alatnet
 */
public class gmpuQueueRoot extends Thread {
    protected JButton queueBtn;
    protected boolean stopQueue=false;
    
    public void stopQueue(){ this.stopQueue = true; }
    public void setQueueBtn(JButton queueBtn){ this.queueBtn = queueBtn; }

    @Override
    public void run(){}
}
