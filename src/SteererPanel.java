/* ----------------------------------------------------------------------------
  This file is part of the RealityGrid Steering Library Java Wrappers.
 
  (C) Copyright 2007, University of Manchester, United Kingdom,
  all rights reserved.
 
  This software was developed by the RealityGrid project
  (http://www.realitygrid.org), funded by the EPSRC under grants
  GR/R67699/01 and GR/R67699/02.
 
  LICENCE TERMS
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:
  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 
  THIS MATERIAL IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. THE ENTIRE RISK AS TO THE QUALITY
  AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE PROGRAM PROVE
  DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR
  CORRECTION.
 
  Author........: Robert Haines
---------------------------------------------------------------------------- */

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;

import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory; 
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;

public class SteererPanel extends JPanel {

  // The parent steerer window
  Steerer steerer;

  // globals
  private int simHandle;
  private boolean remote;
  private boolean paused = false;
  private boolean attached;

  //
  private boolean gotParams = false;
  private boolean gotIOTypes = false;
  private boolean gotCheckTypes = false;

  private Hashtable<String, SteererParameter> monParams = null;
  private Hashtable<String, SteererParameter> steerParams = null;

  // components
  private JTabbedPane tabs;
  private JPanel paramPanel;
  private JPanel monPanel;
  private JPanel steerPanel;
  private JButton pause;
  private JButton stop;
  private JButton detach;
  private JButton close;

  public SteererPanel(Steerer steerer, int sim) {
    this.steerer = steerer;
    this.simHandle = sim;
    this.attached = true;

    initialisePanel();
  }

  public int getSimHandle() {
    return simHandle;
  }

  public boolean isAttached() {
    return attached;
  }

  public void setAttached(boolean attached) {
    this.attached = attached;
    if(attached) {
      pause.setEnabled(true);
      stop.setEnabled(true);
      detach.setEnabled(true);
      close.setEnabled(false);
    }
    else {
      pause.setEnabled(false);
      stop.setEnabled(false);
      detach.setEnabled(false);
      close.setEnabled(true);
    }
  }

  private void initialisePanel() {
    setLayout(new BorderLayout());
    
    // create buttons
    Action pa = new AbstractAction(steerer.getString("Panels.pause_button")) {
	public void actionPerformed(ActionEvent e) {
	  if(paused) {
	    steerer.emitResume(simHandle);
	    paused = false;
	    pause.setText(steerer.getString("Panels.pause_button"));
	    pause.setToolTipText(steerer.getString("Panels.pause_tip"));
	  }
	  else {
	    steerer.emitPause(simHandle);
	    paused = true;
	    pause.setText(steerer.getString("Panels.resume_button"));
	    pause.setToolTipText(steerer.getString("Panels.resume_tip"));
	  }
	}
      };

    Action sa = new AbstractAction(steerer.getString("Panels.stop_button")) {
	public void actionPerformed(ActionEvent e) {
	  steerer.emitStop(simHandle);
	  setAttached(false);
	}
      };

    Action da = new AbstractAction(steerer.getString("Panels.detach_button")) {
	public void actionPerformed(ActionEvent e) {
	  steerer.detach(simHandle);
	  setAttached(false);
	}
      };

    Action ca = new AbstractAction(steerer.getString("Panels.close_button")) {
	public void actionPerformed(ActionEvent e) {
	  steerer.removePanel(simHandle);
	}
      };

    // create and add buttons panel
    JPanel buttonPanel = new JPanel();
    pause = (JButton) buttonPanel.add(new JButton(pa));
    pause.setToolTipText(steerer.getString("Panels.pause_tip"));
    stop = (JButton) buttonPanel.add(new JButton(sa));
    stop.setToolTipText(steerer.getString("Panels.stop_tip"));
    detach = (JButton) buttonPanel.add(new JButton(da));
    detach.setToolTipText(steerer.getString("Panels.detach_tip"));
    close = (JButton) buttonPanel.add(new JButton(ca));
    close.setEnabled(false);
    close.setToolTipText(steerer.getString("Panels.close_tip"));
    add(buttonPanel, BorderLayout.NORTH);

    // create and add tabs
    tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    paramPanel = new JPanel();
    paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.PAGE_AXIS));

    Border etched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    Border monBorder = BorderFactory.createTitledBorder(etched, "Monitored parameters");
    Border steerBorder = BorderFactory.createTitledBorder(etched, "Steered parameters");
    monPanel = new JPanel();
    monPanel.setLayout(new SpringLayout());
    monPanel.setBorder(monBorder);
    paramPanel.add(monPanel);
    steerPanel = new JPanel();
    steerPanel.setLayout(new SpringLayout());
    steerPanel.setBorder(steerBorder);
    paramPanel.add(steerPanel);

    add(tabs, BorderLayout.CENTER);
  }

  public void addParam(SteererParameter sp) {
    if(monParams == null) {
      monParams = new Hashtable<String, SteererParameter>(10);
    }
    if(steerParams == null) {
      steerParams = new Hashtable<String, SteererParameter>(10);
    }

    if(sp.isSteered()) {
      steerParams.put(sp.getLabel(), sp);
      JLabel l = (JLabel) steerPanel.add(sp.getJLabel());
      JTextField v = (JTextField) steerPanel.add(sp.getValue());
      l.setLabelFor(v);
      steerPanel.add(sp.getEditor());
    }
    else {
      monParams.put(sp.getLabel(), sp);
      JLabel l = (JLabel) monPanel.add(sp.getJLabel());
      JTextField v = (JTextField) monPanel.add(sp.getValue());
      l.setLabelFor(v);
    }
  }

  public void updateParam(String name, Object value) {
    SteererParameter sp;
    if((sp = monParams.get(name)) != null) {
      sp.setValue(value);
    }

    if((sp = steerParams.get(name)) != null) {
      sp.setValue(value);
    }
  }

  public void drawParams() {
    if(monParams.size() > 0) {
      SpringUtilities.makeCompactGrid(monPanel, monParams.size(), 2, 6, 6, 6, 6);
      SpringLayout sl = (SpringLayout) monPanel.getLayout();
      Dimension d = sl.minimumLayoutSize(monPanel);
      monPanel.setSize(d);
    }
    if(steerParams.size() > 0) {
      SpringUtilities.makeCompactGrid(steerPanel, steerParams.size(), 3, 6, 6, 6, 6);
      SpringLayout sl = (SpringLayout) steerPanel.getLayout();
      Dimension d = sl.minimumLayoutSize(steerPanel);
      steerPanel.setSize(d);
    }

    tabs.insertTab("Parameters", null, paramPanel, "", 0);
  }

  public void steerParam(int handle, String value) {
    steerer.steerParam(simHandle, handle, value);
  }
}
