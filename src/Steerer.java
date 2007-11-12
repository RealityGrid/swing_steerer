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

import org.realitygrid.steering.ReG_SteerConstants;
import org.realitygrid.steering.ReG_SteerException;
import org.realitygrid.steering.ReG_SteerParameter;
import org.realitygrid.steering.ReG_SteerRegistryEntry;
import org.realitygrid.steering.ReG_SteerSecurity;
import org.realitygrid.steering.ReG_SteerSteerside;
import org.realitygrid.steering.ReG_SteerUtilities;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.IOException;

import java.net.URL;

import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory; 
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class Steerer extends JPanel implements ReG_SteerConstants {

  // The steering library stuff
  private ReG_SteerSteerside rss;
  private String registryAddress;
  private ReG_SteerSecurity regSec;

  // lock for steering library
  private Object rssLock = new Object();

  // timer to poll for updates
  private Timer timer = null;

  // Swing stuff
  private JFrame frame = null;
  private JMenuBar menubar = null;
  private JDialog aboutBox = null;
  private JTabbedPane tabs = null;
  private JLabel logo = null;

  // internationalization stuff
  private ResourceBundle bundle = null;

  // The preferred size of the window
  private static final int PREFERRED_WIDTH = 400;
  private static final int PREFERRED_HEIGHT = 500;

  // Used only if we are an applet 
  private SteererApplet applet = null;

  // Steerer stuff
  private String registry = null;

  // Things being steered
  private Hashtable<Integer, SteererPanel> panels = new Hashtable<Integer, SteererPanel>(5);
  private int numPanels = 0;

  public Steerer(SteererApplet applet) {
    this(applet, null, null);
  }

  public Steerer(SteererApplet applet, String[] args, GraphicsConfiguration gc) {

    // should be null if not an applet
    this.applet = applet;

    if(gc != null && !isApplet()) {
      frame = createFrame(gc);
    }

    // set layout
    setLayout(new BorderLayout());
    
    // set the preferred size of the panel
    setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));

    initialiseSteerer(args);

    // Show the window. Note that this must be done on the GUI thread
    // using invokeLater.
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  // Does nothing if there's no containing frame
	  showSteerer();
	}
      });
  }

  public static void main(String[] args) {
//     try {
//       UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//     }
//     catch(UnsupportedLookAndFeelException e) {
//     }
//     catch(ClassNotFoundException e) {
//     }
//     catch(InstantiationException e) {
//     }
//     catch(IllegalAccessException e) {
//     }

    Steerer steerer = new Steerer(null, args, GraphicsEnvironment.
				  getLocalGraphicsEnvironment().
				  getDefaultScreenDevice().
				  getDefaultConfiguration());
  }

  private void initialiseSteerer(String[] args) {
    if(frame != null) {
      menubar = createMenus();
    }

    if(isApplet())
      applet.setJMenuBar(menubar);
    else {
      if(frame != null)
	frame.setJMenuBar(menubar);
    }

    String initSWS = null;
    if(args.length > 0) {
      for(String s : args) {
	if(s.indexOf("regServiceGroup") != -1) registry = s;
	if(s.indexOf("SWS") != -1) initSWS = s;
      }
    }

    tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

    logo = new JLabel(createImageIcon("logo-big.png", getString("Frame.logo_access_desc")));

    // Initialise the library
    rss = ReG_SteerSteerside.getInstance();
    try {
      rss.steererInitialize();
    }
    catch(ReG_SteerException e) {
      System.err.println(e);
      System.exit(e.getErrorCode());
    }
    
    String userConfFile = System.getProperty("user.home") + "/.realitygrid/security.conf";
    regSec = new ReG_SteerSecurity(userConfFile);

    // try to attach if got the right info
    if(initSWS != null) {
      tryAttach(initSWS, true);
    }
    else {
      add(logo, BorderLayout.CENTER);
    }
  }

  public void showSteerer() {
    if(!isApplet() && frame != null) {
      // put steerer in a frame and show it
      JFrame f = frame;
      f.setTitle(getString("Frame.title"));
      f.getContentPane().add(this, BorderLayout.CENTER);
      f.pack();
      
      Rectangle screenRect = f.getGraphicsConfiguration().getBounds();
      Insets screenInsets = Toolkit.getDefaultToolkit().
	getScreenInsets(f.getGraphicsConfiguration());
      
      // Make sure we don't place the steerer off the screen.
      int centerWidth = screenRect.width < f.getSize().width ?
	screenRect.x :
	screenRect.x + screenRect.width/2 - f.getSize().width/2;
      int centerHeight = screenRect.height < f.getSize().height ?
	screenRect.y :
	screenRect.y + screenRect.height/2 - f.getSize().height/2;
      
      centerHeight = centerHeight < screenInsets.top ?
	screenInsets.top : centerHeight;
      
      f.setLocation(centerWidth, centerHeight);
      f.setVisible(true);
    } 
  }

  public synchronized void addPanel(int handle, SteererPanel sp, String title) {
    panels.put(handle, sp);

    if(numPanels == 0) {
      remove(logo);
      add(tabs, BorderLayout.CENTER);
      startTimer(200);
    }

    if(title == null) title = "Simulation " + numPanels;
    tabs.addTab(title, null, sp);

    numPanels++;
  }

  public synchronized void addPanel(int handle, SteererPanel sp) {
    addPanel(handle, sp , null);
  }

  public synchronized SteererPanel removePanel(int handle) {
    SteererPanel sp = null;

    if(panels.containsKey(handle)) {
      sp = panels.remove(handle);

      tabs.remove(sp);
      numPanels--;

      if(numPanels == 0) {
	remove(tabs);
	stopTimer();
	add(logo, BorderLayout.CENTER);
      }
    }

    return sp;
  }

  public void cleanExit() {
    if(numPanels > 0) {
      boolean attached = false;

      // are any panels attached?
      for(SteererPanel sp : panels.values()) {
	if(sp.isAttached()) {
	  attached = true;
	  break;
	}
      }

      if(attached) {
	int quit = JOptionPane.showConfirmDialog(this, getString("Frame.exit"), "Really exit?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

	if(quit == 1)
	  return;
      }

      for(SteererPanel sp : panels.values()) {
	int handle = sp.getSimHandle();
	removePanel(handle);
	if(sp.isAttached()) {
	  synchronized(rssLock) {
	    rss.simDetach(handle);
	  }
	}
      }
    }

    // clean up steering library
    synchronized(rssLock) {
      rss.steererFinalize();
    }

    System.exit(0);
  }

  private void startTimer(int millis) {
    if(timer != null && timer.isRunning())
	stopTimer();

    timer = new Timer(millis, createSteererUpdateAction());
    timer.start();
  }

  private void stopTimer() {
    if(timer != null && timer.isRunning()) {
      timer.stop();
      timer = null;
    }
  }

  public void setTimerDelay(int millis) {
    if(timer != null && timer.isRunning())
      timer.setDelay(millis);
  }

  public int getTimerDelay() {
    if(timer != null && timer.isRunning())
      return timer.getDelay();
    else
      return 0;
  }

  public void tryAttach(String sws, boolean remote) {
    SteererPanel p = null;
    int simHandle = REG_SIM_HANDLE_NOTSET;

    if(remote) {
      if(sws == null) {
	// query registry
	String reg = getRegistry();
	if(reg == null) return;
	if((sws = getSWS()) == null) return;
      }

      try {
	simHandle = attach(sws);
      }
      catch(ReG_SteerException re) {
	JOptionPane.showMessageDialog(this, "Could not connect to SWS:\n" + sws, "Error!", JOptionPane.ERROR_MESSAGE);
      }
    }
    else { // local
      try {
	simHandle = attach("");
      }
      catch(ReG_SteerException re) {
	JOptionPane.showMessageDialog(this, "Could not connect to local simulation.\n", "Error!", JOptionPane.ERROR_MESSAGE);
      }
    }
    
    if(simHandle != REG_SIM_HANDLE_NOTSET) {
      p = new SteererPanel(this, simHandle);
      
      if(p != null) {
	addPanel(simHandle, p);
      }
    }
  }

  /***************************************************
   *
   * ReG methods
   *
   ***************************************************/

  public String getRegistry() {
    String reg = (String) JOptionPane.showInputDialog(this, "Registry address:", "Input...", JOptionPane.QUESTION_MESSAGE, null, null, registry);

    if(reg == null || reg.equals("")) reg = null;
    if(reg != null) registry = reg;

    return reg;
  }

  public String getSWS(String reg) {
    String sws = null;
    ReG_SteerRegistryEntry[] swsList;
    String password;
    String prompt;

    // check registry isn't null
    if(reg == null) reg = registry;

    // set SSL params
    if(reg.startsWith("https")) {
      // SSL
      regSec.setUsingSSL(true);
      prompt = "Please enter your certificate password:";
    }
    else {
      // no SSL
      regSec.setUsingSSL(false);
      prompt = "Please enter the Registry password:";
    }
    
    // get password for registry
    password = getPassword(prompt);
    if(password != null) {
      regSec.setPassphrase(password);
      password = null;

      synchronized(rssLock) {
	swsList = rss.getRegistryEntriesFilteredSecure(registry, regSec, "SWS");
      }
      
      // present SWS choices
      if(swsList != null && swsList.length != 0) {
	if(swsList.length == 1)
	  sws = swsList[0].getGSH();
	else {
	  ReG_SteerRegistryEntry swsC = (ReG_SteerRegistryEntry) JOptionPane.showInputDialog(this, "Please select an SWS:", "SWS", JOptionPane.QUESTION_MESSAGE, null, swsList, null);
	  if(swsC != null)
	    sws = swsC.getGSH();
	}
      }
      else {
	JOptionPane.showMessageDialog(this, "No SWS was returned.\nEither you do not have access to any SWSs in the registry\nor the password you supplied for the registry was incorrect.", "Error!", JOptionPane.ERROR_MESSAGE);
      }
    }

    return sws;
  }

  public String getSWS() {
    return getSWS(null);
  }

  public int attach(String sws) {
    int result = REG_SIM_HANDLE_NOTSET;

    if(sws == "") {
      // local attach
      synchronized(rssLock) {
	result = rss.simAttach("");
      }
    }
    else {
      // remote attach
      String password = getPassword("Enter password for this SWS:");
      if(password != null) {
	regSec.setPassphrase(password);
	password = null;

	synchronized(rssLock) {
	  result = rss.simAttachSecure(sws, regSec);
	}
      }
    }

    return result;
  }

  public int[] getSteeringMessage() {
    synchronized(rssLock) {
      return rss.getNextMessage();
    }
  }

  public void consumeCheckTypeDefs(int handle) {
    synchronized(rssLock) {
      rss.consumeChkTypeDefs(handle);
    }
  }

  public void consumeIOTypeDefs(int handle) {
    synchronized(rssLock) {
      rss.consumeIOTypeDefs(handle);
    }
  }

  public void consumeParamDefs(int handle) {
    synchronized(rssLock) {
      rss.consumeParamDefs(handle);
    }
  }

  public int consumeStatus(int handle, int[] commands) {
    synchronized(rssLock) {
      return rss.consumeStatus(handle, commands);
    }
  }

  public void emitPause(int handle) {
    synchronized(rssLock) {
      rss.emitPauseCmd(handle);
    }
  }

  public void emitResume(int handle) {
    synchronized(rssLock) {
      rss.emitResumeCmd(handle);
    }
  }

  public void emitStop(int handle) {
    synchronized(rssLock) {
      rss.emitStopCmd(handle);
    }
  }

  public void detach(int handle) {
    synchronized(rssLock) {
      rss.simDetach(handle);
    }
  }

  public void steerParam(int handle, int pHandle, String value) {
    int[] pHandles = {pHandle};
    String[] values = {value};
    synchronized(rssLock) {
      rss.setParamValues(handle, pHandles, values);
      rss.emitControl(handle, null, null);
    }
  }

  public Action createSteererUpdateAction() {
    return new AbstractAction("steering update action") {
	public void actionPerformed (ActionEvent e) {
	  if(REG_DEBUG == 1) {
	    System.out.println("ping!");
	  }

	  int message[] = getSteeringMessage();
	  int simHandle = message[0];
	  switch(message[1]) {
	  case MSG_NOTSET:
	    if(REG_DEBUG == 1) {
	      System.out.println("No message received.");
	    }
	    break;
	  case IO_DEFS:
	    if(REG_DEBUG == 1) {
	      System.out.println("Got IO definitions for " + simHandle);
	    }
	    consumeIOTypeDefs(simHandle);
	    break;
	  case CHK_DEFS:
	    if(REG_DEBUG == 1) {
	      System.out.println("Got checkpoint definitions for " + simHandle);
	    }
	    consumeCheckTypeDefs(simHandle);
	    break;
	  case PARAM_DEFS:
	    if(REG_DEBUG == 1) {
	      System.out.println("Got parameter definitions for " + simHandle);
	    }
	    consumeParamDefs(simHandle);

	    // create params
	    updateParameters(simHandle, false);
	    break;
	  case STATUS:
	    if(REG_DEBUG == 1) {
	      System.out.println("Got status message for " + simHandle);
	    }
	    int[] cmds = new int[REG_MAX_NUM_STR_CMDS];
	    int seqNum = consumeStatus(simHandle, cmds);

	    // updateParams
	    updateParameters(simHandle, true);

	    for(int i = 0; i < cmds.length; i++) {
	      switch(cmds[i]) {
	      case REG_STR_STOP:
	      case REG_STR_DETACH:
		SteererPanel spn = panels.get(simHandle);
		synchronized(rssLock) {
		  simHandle = rss.deleteSimTableEntry(simHandle);
		}
		spn.setAttached(false);
		if(REG_DEBUG == 1) {
		  System.out.println("Told to detach!");
		}
		break;
	      default:
		break;
	      }
	    }

	    if(REG_DEBUG == 1) {
	      System.out.println("Application seqNum: " + seqNum);
	    }
	    break;
	  case CONTROL:
	    if(REG_DEBUG == 1) {
	      System.out.println("Got control message.");
	    }
	    break;
	  case STEER_LOG:
	    if(REG_DEBUG == 1) {
	      System.out.println("Got log message.");
	    }
	    break;
	  case SUPP_CMDS:
	    if(REG_DEBUG == 1) {
	      System.out.println("Got supported commands list.");
	    }
	    break;
	  case MSG_ERROR:
	    if(REG_DEBUG == 1) {
	      System.out.println("Some sort of error occured!");
	    }
	    break;
	  default:
	    if(REG_DEBUG == 1) {
	      System.out.println("Unrecognised msg returned by getNextMessage.");
	    }
	    break;
	  }
	}
      };
  }
  
  private void updateParameters(int handle, boolean isUpdate) {
    SteererPanel spn = panels.get(handle);
    int numParams = rss.getParamNumber(handle, false);
    ReG_SteerParameter[] monParams = rss.getParamValues(handle, false, numParams);
    numParams = rss.getParamNumber(handle, true);
    ReG_SteerParameter[] steerParams = rss.getParamValues(handle, true, numParams);

    if(isUpdate) {
      for(ReG_SteerParameter rp : monParams) {
	spn.updateParam(rp.getLabel(), rp.getValue());
      }

      for(ReG_SteerParameter rp : steerParams) {
	spn.updateParam(rp.getLabel(), rp.getValue());
      }
    }
    else {
      for(ReG_SteerParameter rp : monParams) {
	spn.addParam(SteererParameter.createReadOnly(spn, rp.getHandle(), rp.getValue().intValue(), rp.getLabel(), rp.getMinLabel(), rp.getMaxLabel()));
      }

      for(ReG_SteerParameter rp : steerParams) {
	spn.addParam(SteererParameter.createReadWrite(spn, rp.getHandle(), rp.getValue().intValue(), rp.getLabel(), rp.getMinLabel(), rp.getMaxLabel()));
      }

      // draw params
      spn.drawParams();
    }
  }

  /***************************************************
   *
   * Util methods
   *
   ***************************************************/

  private String getPassword(String prompt) {
    PasswordPanel panel = new PasswordPanel(this, prompt);
    int ok =  JOptionPane.showConfirmDialog(this, panel, "Enter password:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

    if(ok == JOptionPane.OK_OPTION)
      return panel.getPassword();
    else
      return null;
  }

  public boolean isApplet() {
    return (applet != null);
  }

  public JFrame getFrame() {
    return frame;
  }
  
  public String getString(String key) {
    String value = null;

    try {
      value = getResourceBundle().getString(key);
    } catch (MissingResourceException e) {
      System.out.println("Missing resource: Couldn't find value for: " + key);
    }

    if(value == null) {
      value = "Could not find resource: " + key + "  ";
    }

    return value;
  }

  public char getMnemonic(String key) {
    return (getString(key)).charAt(0);
  }

  public ResourceBundle getResourceBundle() {
    if(bundle == null) {
      bundle = ResourceBundle.getBundle("resources.steerer");
    }
    return bundle;
  }

  /***************************************************
   *
   * Various builder methods (menus, panels, etc)
   *
   ***************************************************/

  private JFrame createFrame(GraphicsConfiguration gc) {
    frame = new JFrame(gc);
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    WindowListener wl = new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  cleanExit();
	}
      };
    frame.addWindowListener(wl);

    return frame;
  }

  private JMenuBar createMenus() {
    // create menubar
    JMenuBar mb = new JMenuBar();
    mb.getAccessibleContext().setAccessibleName(getString("MenuBar.access_desc"));

    // file menu
    JMenu file = (JMenu) mb.add(new JMenu(getString("FileMenu.label")));
    file.setMnemonic(getMnemonic("FileMenu.mnemonic"));
    file.getAccessibleContext().setAccessibleDescription(getString("FileMenu.access_desc"));

    createMenuItem(file, "FileMenu.la", new AttachAction(this, false));
    createMenuItem(file, "FileMenu.ga", new AttachAction(this, true));
    createMenuItem(file, "FileMenu.spi", new SteeringIntervalAction(this));
    createMenuItem(file, "FileMenu.api", null);

    if(!isApplet()) {
      file.addSeparator();
      createMenuItem(file, "FileMenu.e", new ExitAction(this));
    }

    // help menu
    JMenu help = (JMenu) mb.add(new JMenu(getString("HelpMenu.label")));
    help.setMnemonic(getMnemonic("HelpMenu.mnemonic"));
    help.getAccessibleContext().setAccessibleDescription(getString("HelpMenu.access_desc"));

    createMenuItem(help, "HelpMenu.a", new AboutAction(this));

    return mb;
  }

  private JMenuItem createMenuItem(JMenu menu, String prefix, Action action) {
    JMenuItem mi = (JMenuItem) menu.add(new JMenuItem(getString(prefix + "_label")));
    mi.setMnemonic(getMnemonic(prefix + "_mnemonic"));
    mi.getAccessibleContext().setAccessibleDescription(getString(prefix + "_access_desc"));

    mi.addActionListener(action);
    mi.setEnabled(action != null);

    return mi;
  }

  public ImageIcon createImageIcon(String filename, String description) {
    String path = "/resources/images/" + filename;
    return new ImageIcon(getClass().getResource(path), description); 
  }
  
  /***************************************************
   *
   * Panels classes, etc
   *
   ***************************************************/

  class PasswordPanel extends JPanel {
    Steerer steerer;
    JPasswordField pass;

    public PasswordPanel(Steerer steerer, String prompt) {
      this.steerer = steerer;

      JPanel panel = new JPanel(new SpringLayout());
      panel.add(new JLabel(prompt));
      pass = (JPasswordField) panel.add(new JPasswordField());
      SpringUtilities.makeCompactGrid(panel, 2, 1, 6, 6, 6, 6);

      add(panel, BorderLayout.CENTER);
    }

    public String getPassword() {
      return new String(pass.getPassword());
    }
  }

  class SliderPanel extends JPanel {
    Steerer steerer;
    JSlider slider;

    public SliderPanel(Steerer steerer, int current) {
      JPanel panel = new JPanel(new SpringLayout());
      panel.add(new JLabel("Steering interval:"));
      slider = (JSlider) panel.add(new JSlider(1, 5, current));
      slider.setMajorTickSpacing(1);
      slider.setPaintTicks(true);
      slider.setSnapToTicks(true);
      slider.setLabelTable(buildLabels());
      slider.setPaintLabels(true);
      SpringUtilities.makeCompactGrid(panel, 2, 1, 6, 6, 6, 6);
      add(panel, BorderLayout.CENTER);
    }

    public int getValue() {
      return slider.getValue();
    }

    private Hashtable buildLabels() {
      Hashtable<Integer, JLabel> h = new Hashtable<Integer, JLabel>(5);
      h.put(new Integer(1), new JLabel("100"));
      h.put(new Integer(2), new JLabel("250"));
      h.put(new Integer(3), new JLabel("500"));
      h.put(new Integer(4), new JLabel("1000"));
      h.put(new Integer(5), new JLabel("2000"));
      return h;
    }
  }

  class AboutPanel extends JPanel {
    Steerer steerer;
    
    public AboutPanel(Steerer steerer) {
      this.steerer = steerer;

      Border etched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
      Border border = BorderFactory.createTitledBorder(etched, getString("AboutBox.c_title"));
      JPanel credits = new JPanel();
      credits.setBorder(border);
      
      JEditorPane text = (JEditorPane) credits.add(new JEditorPane());
      text.setEditable(false);
      URL creditsURL = getClass().getResource("/resources/credits.html");
      if(creditsURL != null) {
	try {
	  text.setPage(creditsURL);
	}
	catch(IOException e) {
	  System.err.println("Couldn't read URL: " + creditsURL);
	}
      }
      else {
	System.err.println("Couldn't find file: /resources/credits.html");
      }

      add(credits, BorderLayout.CENTER);
    }
  }

  /***************************************************
   *
   * Actions
   *
   ***************************************************/

  class ExitAction extends AbstractAction {
    Steerer steerer;
    protected ExitAction(Steerer steerer) {
      super("ExitAction");
      this.steerer = steerer;
    }
    
    public void actionPerformed(ActionEvent e) {
      steerer.cleanExit();
    }
  }

  class AboutAction extends AbstractAction {
    Steerer steerer;
    JPanel panel;
    ImageIcon image;

    protected AboutAction(Steerer steerer) {
      super("AboutAction");
      this.steerer = steerer;

      panel = new AboutPanel(steerer);
      image = createImageIcon("logo-sm.png", steerer.getString("AboutBox.access_desc"));
    }

    public void actionPerformed(ActionEvent e) {
	JOptionPane.showMessageDialog(steerer.getFrame(), panel, getString("AboutBox.title"), JOptionPane.PLAIN_MESSAGE, image);	
    }
  }

  class OkAction extends AbstractAction {
    JDialog dialog;

    protected OkAction(JDialog dialog) {
      super("OkAction");
      this.dialog = dialog;
    }

    public void actionPerformed(ActionEvent e) {
      dialog.setVisible(false);
    }
  }

  class AttachAction extends AbstractAction {
    Steerer steerer;
    boolean remote;

    protected AttachAction(Steerer steerer, boolean remote) {
      super("AttachAction");
      this.steerer = steerer;
      this.remote = remote;
    }

    public void actionPerformed(ActionEvent e) {
      steerer.tryAttach(null, remote);
    }
  }

  class SteeringIntervalAction extends AbstractAction {
    Steerer steerer;
    
    protected SteeringIntervalAction(Steerer steerer) {
      super("SteeringIntervalAction");
      this.steerer = steerer;
    }

    public void actionPerformed(ActionEvent e) {
      SliderPanel panel = new SliderPanel(steerer, 3);
      int ok = JOptionPane.showConfirmDialog(steerer, panel, "YO!!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

      if(ok == JOptionPane.OK_OPTION) {
	System.out.println(panel.getValue());
	steerer.setTimerDelay(3000);
      }
    }
  }
}
