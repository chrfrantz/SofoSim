/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// various amendments for externalized use by Christopher Frantz

package org.sofosim.forceLayout.util;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import javax.swing.JComponent;

public class UiListenerCheck extends JComponent implements WindowListener, WindowFocusListener, WindowStateListener{

	static final String newline = System.getProperty("line.separator");
    static final String space = "    ";
	private Frame frame = null;
    
    public UiListenerCheck(Frame frame){
    	this.frame = frame;
    	this.frame.addWindowFocusListener(this);
    	this.frame.addWindowListener(this);
    	this.frame.addWindowStateListener(this);
    }
    
    @Override
	public void windowClosing(WindowEvent e) {
    	displayMessage("WindowListener method called: windowClosing.");
    }
    
	public void windowClosed(WindowEvent e) {
        //This will only be seen on standard output.
        displayMessage("WindowListener method called: windowClosed.");
    }
     
    public void windowOpened(WindowEvent e) {
        displayMessage("WindowListener method called: windowOpened.");
    }
     
    public void windowIconified(WindowEvent e) {
        displayMessage("WindowListener method called: windowIconified.");
    }
     
    public void windowDeiconified(WindowEvent e) {
        displayMessage("WindowListener method called: windowDeiconified.");
    }
     
    public void windowActivated(WindowEvent e) {
        displayMessage("WindowListener method called: windowActivated.");
    }
     
    public void windowDeactivated(WindowEvent e) {
        displayMessage("WindowListener method called: windowDeactivated.");
    }
     
    public void windowGainedFocus(WindowEvent e) {
        displayMessage("WindowFocusListener method called: windowGainedFocus.");
    }
     
    public void windowLostFocus(WindowEvent e) {
        displayMessage("WindowFocusListener method called: windowLostFocus.");
    }
     
    public void windowStateChanged(WindowEvent e) {
        displayStateMessage(
                "WindowStateListener method called: windowStateChanged.", e);
    }
     
    private void displayMessage(String msg) {
        //display.append(msg + newline);
        System.out.println(msg);
    }
     
    private void displayStateMessage(String prefix, WindowEvent e) {
        int state = e.getNewState();
        int oldState = e.getOldState();
        String msg = prefix
                + newline + space
                + "New state: "
                + convertStateToString(state)
                + newline + space
                + "Old state: "
                + convertStateToString(oldState);
        displayMessage(msg);
    }
	
    private String convertStateToString(int state) {
        if (state == Frame.NORMAL) {
            return "NORMAL";
        }
        String strState = " ";
        if ((state & Frame.ICONIFIED) != 0) {
            strState += "ICONIFIED";
        }
        //MAXIMIZED_BOTH is a concatenation of two bits, so
        //we need to test for an exact match.
        if ((state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            strState += "MAXIMIZED_BOTH";
        } else {
            if ((state & Frame.MAXIMIZED_VERT) != 0) {
                strState += "MAXIMIZED_VERT";
            }
            if ((state & Frame.MAXIMIZED_HORIZ) != 0) {
                strState += "MAXIMIZED_HORIZ";
            }
        }
        if (" ".equals(strState)){
            strState = "UNKNOWN";
        }
        return strState.trim();
    }

    //Some window managers don't support all window states.
    public void checkWM(Frame frame) {
        Toolkit tk = frame.getToolkit();
        if (!(tk.isFrameStateSupported(Frame.ICONIFIED))) {
            displayMessage(
                    "Your window manager doesn't support ICONIFIED.");
        }  else displayMessage(
                "Your window manager supports ICONIFIED.");
        if (!(tk.isFrameStateSupported(Frame.MAXIMIZED_VERT))) {
            displayMessage(
                    "Your window manager doesn't support MAXIMIZED_VERT.");
        }  else displayMessage(
                "Your window manager supports MAXIMIZED_VERT.");
        if (!(tk.isFrameStateSupported(Frame.MAXIMIZED_HORIZ))) {
            displayMessage(
                    "Your window manager doesn't support MAXIMIZED_HORIZ.");
        } else displayMessage(
                "Your window manager supports MAXIMIZED_HORIZ.");
        if (!(tk.isFrameStateSupported(Frame.MAXIMIZED_BOTH))) {
            displayMessage(
                    "Your window manager doesn't support MAXIMIZED_BOTH.");
        } else {
            displayMessage(
                    "Your window manager supports MAXIMIZED_BOTH.");
        }
    }
	
}
