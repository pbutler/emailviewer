// vim: ts=4 et sw=4

package edu.vt.Miner;


import org.json.JSONObject;
import org.json.JSONException;

import edu.vt.Miner.ForceDirectedView;
import edu.vt.Miner.RadialGraphView;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.ServerSocket;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.event.EventConstants;
import prefuse.data.io.GraphMLReader;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphLib;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;
import prefuse.util.display.ItemBoundsListener;
import prefuse.util.force.ForceSimulator;
import prefuse.util.io.IOLib;
import prefuse.util.ui.JForcePanel;
import prefuse.util.ui.JValueSlider;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;

import java.io.File;
import javax.swing.*;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.*;
import prefuse.data.io.GraphMLWriter;

/**
 * @author <a href="mailto:pbutler killertux org">Patrick Butler</a>
 */
public class SocialViewer extends JFrame {

	private static final String LABEL = "label";
	private static final String SIZE = "size";
	private static final String LOC  = "location";

	private Visualization m_vis;
	private Graph m_graph;
	private Table m_dates;
	private int node_cnt;

	ForceDirectedView forceView;
	RadialGraphView radialView;
	GraphView curView;

	public SocialViewer(String file) { 
		super("E-Mail GraphMiner"); //, new BorderLayout());
		node_cnt = 0;


		m_graph = new Graph();
		m_graph.addColumn(LABEL, String.class);
		m_graph.addColumn(LOC, String.class);
		m_graph.addColumn(SIZE, int.class);

        //m_dates = new Table();
        ////TODO HERE
        //m_dates.addColumn
        
		
		Node n0 = m_graph.addNode();
		n0.setString(LABEL, "Waiting For Data");
		
		forceView = new ForceDirectedView(m_graph, LABEL);
		radialView = new RadialGraphView(m_graph, LABEL);
		curView = forceView;
		//curView = radialView;
		getContentPane().add(curView);
		//curView.setVisible(true);

		JMenu fileView = new JMenu("File");
		JMenuItem openItem = new JMenuItem("Save as...");
		fileView.add( openItem );
		openItem.addActionListener(
			new ActionListener(){ 
				public void actionPerformed(ActionEvent e) { 
					JFileChooser fc = new JFileChooser(); 
					FileFilter filter = new FileFilter() {  
						@Override
						public boolean accept(File f) { 
							return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"); 
						} 
						@Override 
						public String getDescription() { 
							return "*.xml";
						}
					};
					fc.addChoosableFileFilter(filter);
					int returnVal = fc.showSaveDialog(SocialViewer.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) { 
						File file = fc.getSelectedFile(); 
						try {
                            FileOutputStream os = new FileOutputStream(file);
                            GraphMLWriter mlWriter = new GraphMLWriter();
                            mlWriter.writeGraph(m_graph, os);
                            os.close();
                        } catch (prefuse.data.io.DataIOException ex) {
                            System.err.println("error: " + ex.getMessage() );
                        } catch (java.io.IOException ex) {
                            System.err.println("error: " + ex.getMessage() );
                        }
					   
					} 
				}
		});

		JMenu menuView = new JMenu("View" );

		JMenuItem fdItem = new JMenuItem("Force Directed View");
		menuView.add( fdItem );
		fdItem.addActionListener(
					new ActionListener(){ 
						public void actionPerformed(ActionEvent e) { 
							changeView(forceView);
						} 
					});

		JMenuItem radialItem  = new JMenuItem("Radial View");
		menuView.add( radialItem );
		radialItem.addActionListener(
					new ActionListener(){ 
						public void actionPerformed(ActionEvent e) { 
							changeView(radialView);
					   } 
					});


		JMenuBar bar = new JMenuBar();
		bar.add( fileView );
		bar.add( menuView );
		setJMenuBar( bar );

		pack();
		setVisible(true);
		
		addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				curView.focus(true);
			}
			public void windowDeactivated(WindowEvent e) {
				curView.focus(false);
			}
		});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		ServerSocket server;
		Socket client;
		BufferedReader in;
		PrintWriter out;
		try {
			if (file == null ) {
				server = new ServerSocket(4321); 
			} else {
				server = new ServerSocket();
			}
		} catch (IOException e) {
			System.out.println("Failed to create socket");
			return;
		}

		int run = 0;
		while(file == null || run == 0) {
			run++;
			try { 
				if (file == null ) {
					client = server.accept(); 
					in = new BufferedReader(new InputStreamReader( 
											client.getInputStream())); 
					out = new PrintWriter(client.getOutputStream(), true);
				} else {
					in = new BufferedReader(
						   new InputStreamReader( getClass( ).getResourceAsStream("/dump")));
					out = new PrintWriter(System.err, true);
				}
				String input = "";
				int i = 3;
				while(true) {
					input = in.readLine();
					if ( file == null && input == null) 
						break;
					try {
						JSONObject jobj = new JSONObject(input);
						String cmd = jobj.getString("cmd");
						if (cmd.equals("q")) {
							break;
						} else if (cmd.equals("na")) {
							String name = jobj.getString("name");
							String location = jobj.getString("loc");
							int id = addNode( name, location );
							out.println( id );
							//System.out.println("node add " + name) ;
						} else if (cmd.equals("ea")) {
							int frm = jobj.getInt("from");
							int to = jobj.getInt("to");
							addEdge( frm, to );
							//System.out.println("edge add " + String.valueOf(frm) + " " + String.valueOf(to) );
						}
					} catch (JSONException e) {
					}
				}

			} catch (IOException e) {
				System.out.println("Connection closed, waiting for a new connection");
			}
		}

	}
   
	public void changeView(GraphView view) {
		curView.setVisible(false);
		getContentPane().remove(curView);
		curView = view;
		getContentPane().add(curView);
		curView.setVisible(true);
		pack();
	}

   public int  addNode(String label, String group) {
		Node n;
		synchronized(curView.lock) {
			curView.updating();
		if (node_cnt == 0 ) {
			n = m_graph.getNode(0);
		} else { 
			n = m_graph.addNode();
		}
		node_cnt++;
		n.setString(LABEL, label);
		n.setString(LOC, group);
		n.setInt(SIZE, 40);
		System.out.println(label + ": " + group);
		curView.update();
		}
		return node_cnt - 1;
	}

	public void addEdge(int frm, int to) {
		//TODO: Pause rendering while updating
		//curView.updating()
		curView.focus(false);
		m_graph.addEdge(frm, to);
		curView.update();
		curView.focus(true);
	}
 
	// ------------------------------------------------------------------------
	// Main and demo methods
	
	public static void main(String[] args) {
		UILib.setPlatformLookAndFeel();
		/////////
		SocialViewer view;
		if (args.length >= 1 && args[0].equals("-t") || true) {
			view = new SocialViewer("/dump");
		} else {
			view = new SocialViewer(null);
		}
	}
	
				
} // end of class SocialViewer
