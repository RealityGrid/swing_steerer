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

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import javax.swing.text.JTextComponent;

public abstract class SteererParameter {

  private SteererPanel steerer;
  private int handle;

  private String min;
  private String max;
  private boolean steered;

  // interface stuff
  private JLabel label;
  private JTextField value;
  protected JComponent editor;

  private SteererParameter() {}

  protected SteererParameter(SteererPanel steerer, int handle, Object value, String label, boolean steered, String min, String max) {
    this.steerer = steerer;
    this.handle = handle;
    this.steered = steered;
    this.min = min;
    this.max = max;

    this.label = new JLabel(label + ":", JLabel.TRAILING);
    this.value = new JTextField("" + value);
    this.value.setEditable(false);
  }

  public String getLabel() {
    String l = label.getText();
    if(l.endsWith(":")) {
      l = l.substring(0, (l.length() - 1));
    }
    return l;
  }

  public JLabel getJLabel() {
    return label;
  }

  public JComponent getEditor() {
    return editor;
  }

  public boolean isSteered() {
    return steered;
  }

  public JTextField getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value.setText("" + value);
  }

  public void steerValue(Object value) {
    steerer.steerParam(handle, "" + value);
  }

  public static SteererParameter createReadOnly(SteererPanel steerer, int handle, Object value, String label, String min, String max) {
    ReadOnly sp = new ReadOnly(steerer, handle, value, label);

    return sp;
  }

  public static SteererParameter createReadWrite(SteererPanel steerer, int handle, Object value, String label, String min, String max) {

    SteererParameter sp;

    if(min.equals("0") && max.equals("1")) {
      sp = new BoolParameter(steerer, handle, value, label, min, max);
    }
    else {
      sp = new ReadWrite(steerer, handle, value, label, min, max);  
    }

    return sp;
  }
}

class ReadOnly extends SteererParameter {
  public ReadOnly(SteererPanel steerer, int handle, Object value, String label) {
    super(steerer, handle, value, label, false, "", "");
    editor = null;
  }
}

class ReadWrite extends SteererParameter {

  public ReadWrite(SteererPanel steerer, int handle, Object value, String label, String min, String max) {
    super(steerer, handle, value, label, true, min, max);
    JTextField e = new JTextField();
    e.addActionListener(new ParamChangeAction(this));
    e.setToolTipText(createTooltip(min, max));
    editor = e;
  }

  protected String createTooltip(String min, String max) {
    StringBuilder result = new StringBuilder();

    if(!min.equals("--")) result.append(min + " <= ");
    result.append("x");
    if(!max.equals("--")) result.append(" <= " + max);

    return result.toString();
  }

  class ParamChangeAction extends AbstractAction {
    ReadWrite param;

    protected ParamChangeAction(ReadWrite param) {
      this.param = param;
    }

    public void actionPerformed(ActionEvent e) {
      param.steerValue(((JTextField) param.editor).getText());
      ((JTextField) param.editor).setText("");
    }
  }
}

class BoolParameter extends ReadWrite {
  
  public BoolParameter(SteererPanel steerer, int handle, Object value, String label, String min, String max) {
    super(steerer, handle, value, label, min, max);

    JCheckBox cb = new JCheckBox();
    cb.addItemListener(new ParamChangeAction(this));
    cb.setToolTipText(createTooltip(min, max));
    editor = cb;
  }

  class ParamChangeAction implements ItemListener {
    BoolParameter param;

    protected ParamChangeAction(BoolParameter param) {
      this.param = param;
    }

    public void itemStateChanged(ItemEvent e) {
      if(e.getStateChange() == ItemEvent.SELECTED) {
	param.steerValue("1");
      }
      else {
	param.steerValue("0");
      }
    }
  }
}
