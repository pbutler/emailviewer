// vim: ts=4 et sw=4

package edu.vt.Miner;

import edu.vt.Miner.GraphView;
import edu.vt.Miner.FlexibleZone;
import edu.vt.Miner.FlexibleZoneFactory;

import java.util.HashMap;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
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
import prefuse.visual.NodeItem;
import prefuse.util.force.DragForce;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;

import prefuse.render.Renderer;
import prefuse.render.ShapeRenderer;

import prefuse.data.event.GraphListener;

import profusians.zonemanager.ZoneManager;
import profusians.zonemanager.zone.Zone;
import profusians.zonemanager.action.ZoneGuardAction;
import profusians.util.force.ForceSimulatorRemovableForces;
import profusians.zonemanager.util.display.ZoneBorderDrawing;
import profusians.zonemanager.zone.shape.CircularZoneShape;
import profusians.zonemanager.zone.shape.RectangularZoneShape;
import profusians.zonemanager.zone.shape.ZoneShape;
import profusians.zonemanager.zone.colors.ZoneColors;
import profusians.zonemanager.zone.attributes.ZoneAttributes;

import prefuse.activity.SlowInSlowOutPacer;
import prefuse.action.animate.LocationAnimator;
import prefuse.action.animate.ColorAnimator;


/**
 * @author <a href="mailto:pbutler killertux org">Patrick Butler</a>
 */
public class ForceDirectedView extends GraphView {
    
    ZoneManager zoneManager;
    int [] zoneNumbers;
    HashMap groups = new HashMap();
 
    ColorAction zoneColors;   
    ColorAction zoneItemColors;   

    public ForceDirectedView(Graph g, String label) {
    	super(g, label);
        // create a new, empty visualization for our data
        // --------------------------------------------------------------------
        // set up the renderers
        
        // --------------------------------------------------------------------
        // register the data with a visualization
        
        // adds graph to visualization and sets renderer label field
        setGraph(g, label);

        initZoneManager();

        // fix selected focus nodes
        TupleSet focusGroup = m_vis.getGroup(Visualization.FOCUS_ITEMS); 
        focusGroup.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem)
            {
                for (int i = 0; i < rem.length; ++i )
                    ((VisualItem)rem[i]).setFixed(false);
                for (int i = 0; i < add.length; ++i ) {
                    ((VisualItem)add[i]).setFixed(false);
                    ((VisualItem)add[i]).setFixed(true);
                }
                if ( ts.getTupleCount() == 0 ) {
                    ts.addTuple(rem[0]);
                    ((VisualItem)rem[0]).setFixed(false);
                }
                draw();
            }
        });
        
        
        m_graph.addGraphModelListener(new GraphListener() {
            public void graphChanged(Graph g, String table, int start, int end, int col, int type) {
                
                if (! table.equals("nodes") || col == EventConstants.ALL_COLUMNS) {
                    return;
                }

                Table nodeTable = g.getNodeTable();
                if ( ! nodeTable.getColumnName(col).equals(LOC) ) {
                    return;
                }
                synchronized(lock) {
                    for(int i = start; i <= end; i++) {
                        String gname = nodeTable.getString(i, col);
                        //gname = "a";
                        int zid = getZone( gname );
                        NodeItem node = (NodeItem) m_vis.getVisualItem(nodes, nodeTable.getTuple(i));
                        zoneManager.addItemToZone(node, zid);

                        //zoneManager.recalculateFlexibility();
                    }
                }
                update();
                //draw();
            }
        });

        // --------------------------------------------------------------------
        // create actions to process the visual data

        int hops = 30;
        final GraphDistanceFilter filter = new GraphDistanceFilter(graph, hops);


        //flll nodes
        ColorAction fill = new ColorAction(nodes, 
                VisualItem.FILLCOLOR, ColorLib.rgb(200,200,255));
        fill.add(VisualItem.FIXED, ColorLib.rgb(255,100,100));
        fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255,200,125));
        
        zoneColors = zoneManager.getZoneColorAction();
        //m_vis.putAction("zones", zoneColors);

        ActionList draw = new ActionList();
        //draw.add(filter);
        draw.add(fill);

        zoneItemColors = new ColorAction(nodes, VisualItem.FILLCOLOR);
        zoneItemColors.setDefaultColor(ColorLib.gray(255));
        zoneItemColors.add("_hover", ColorLib.gray(200));
        zoneManager.addZoneItemColorMapping(zoneItemColors);

        //draw.add( zoneManager.getZoneColorAction());

        //draw.add(new ColorAction(nodes, VisualItem.STROKECOLOR, 0));
        draw.add(new ColorAction(nodes, VisualItem.TEXTCOLOR, ColorLib.rgb(0,0,0)));
        draw.add(new ColorAction(edges, VisualItem.FILLCOLOR, ColorLib.gray(200)));
        draw.add(new ColorAction(edges, VisualItem.STROKECOLOR, ColorLib.gray(200)));
        draw.add(zoneColors);
        draw.add(zoneItemColors);
        m_vis.putAction("draw", draw);

        
        ActionList animate = new ActionList(Activity.INFINITY);
        ForceDirectedLayout fdl = new ForceDirectedLayout(graph, zoneManager.getForceSimulator(), false);
        animate.add(new ZoneGuardAction(zoneManager));
        animate.add(zoneManager.getZoneLayout(ZoneManager.CONVEXHULLZONERENDERER));

        animate.add(fdl);
        //animate.add(fill);
        //animate.add( zoneManager.getZoneColorAction());
        animate.add(new RepaintAction());
        
        // finally, we register our ActionList with the Visualization.
        // we can later execute our Actions by invoking a method on our
        // Visualization, using the name we've chosen below.
        m_vis.putAction("layout", animate);

        m_vis.runAfter("draw", "layout");
   


 /*       // first set up all the color actions
        ColorAction nStroke = new ColorAction(nodes, VisualItem.STROKECOLOR);
        nStroke.setDefaultColor(ColorLib.gray(100));
        nStroke.add("_hover", ColorLib.gray(50));
    
        ColorAction nFill = new ColorAction(nodes, VisualItem.FILLCOLOR);
        nFill.setDefaultColor(ColorLib.gray(255));
        nFill.add("_hover", ColorLib.gray(200));
        zoneManager.addZoneItemColorMapping(nFill);
        
        ColorAction nEdges = new ColorAction(edges, VisualItem.STROKECOLOR);
        nEdges.setDefaultColor(ColorLib.gray(100));
        
        ColorAction aFill = zoneManager.getZoneColorAction();
        
        // bundle the color actions
        ActionList colors = new ActionList();
        colors.add(nStroke);
        colors.add(nFill);

        colors.add(aFill);
        colors.add(nEdges);



        ForceDirectedLayout fdlZone = new ForceDirectedLayout(graph,
                    zoneManager.getForceSimulator(), false);

        int duration = 2000;

        ActionList catchThem = new ActionList(duration);
        catchThem.setPacingFunction(new SlowInSlowOutPacer());
        catchThem.add(colors);
        catchThem.add(new ZoneGuardAction(zoneManager));
        catchThem.add(zoneManager.getZoneLayout(ZoneManager.CONVEXHULLZONERENDERER));
        catchThem.add(fdlZone);

        catchThem.add(new ColorAnimator(nodes));
        catchThem.add(new LocationAnimator(nodes));
        catchThem.add(new RepaintAction());


        ActionList keepThem = new ActionList(Activity.INFINITY);
        keepThem.add(new ZoneGuardAction(zoneManager));
        keepThem.add(fdlZone);
        keepThem.add(zoneManager.getZoneLayout(ZoneManager.CONVEXHULLZONERENDERER)); 
        keepThem.add(new RepaintAction()); 
        
        m_vis.putAction("keepThem", keepThem); 

        m_vis.putAction("layout", catchThem);
        m_vis.alwaysRunAfter("layout", "keepThem");*/

        // --------------------------------------------------------------------
        // set up a display to show the visualization
        
        Display display = new Display(m_vis);
        display.setSize(700,700);
        display.pan(350, 350);
        display.setForeground(Color.GRAY);
        display.setBackground(Color.WHITE);
        
        // main display controls
        display.addControlListener(new FocusControl(1));
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        //display.addControlListener(new NeighborHighlightControl());

        // overview display
        /*Display overview = new Display(m_vis);
        overview.setSize(290,290);
        overview.addItemBoundsListener(new FitOverviewListener());*/
        
        display.setForeground(Color.GRAY);
        display.setBackground(Color.WHITE);
        

        //display.addPaintListener(new ZoneBorderDrawing(zoneManager));

        // --------------------------------------------------------------------        
        // launch the visualization
        
        // create a panel for editing force values
        ForceSimulator fsim = fdl.getForceSimulator();
        JForcePanel fpanel = new JForcePanel(fsim);
        
        /*JPanel opanel = new JPanel();
        opanel.setBorder(BorderFactory.createTitledBorder("Overview"));
        opanel.setBackground(Color.WHITE);
        opanel.add(overview);*/
        
        final JValueSlider slider = new JValueSlider("Distance", 0, hops, hops);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                filter.setDistance(slider.getValue().intValue());
                draw();
            }
        });
        slider.setBackground(Color.WHITE);
        slider.setPreferredSize(new Dimension(300,30));
        slider.setMaximumSize(new Dimension(300,30));
        
        Box cf = new Box(BoxLayout.Y_AXIS);
        cf.add(slider);
        cf.setBorder(BorderFactory.createTitledBorder("Connectivity Filter"));
        fpanel.add(cf);

        //fpanel.add(opanel);
        
        fpanel.add(Box.createVerticalGlue());
        
        // create a new JSplitPane to present the interface
        JSplitPane split = new JSplitPane();
        split.setLeftComponent(display);
        split.setRightComponent(fpanel);
        split.setOneTouchExpandable(true);
        split.setContinuousLayout(false);
        split.setDividerLocation(700);
        
        // now we run our action list
        draw();
        add(split);
    }
    /*
    public void setGraph(Graph g, String label) {
        // update labeling
        DefaultRendererFactory drf = (DefaultRendererFactory)
                                                m_vis.getRendererFactory();
        ((LabelRenderer)drf.getDefaultRenderer()).setTextField(label);
        
        // update graph
        m_vis.removeGroup(graph);
        VisualGraph vg = m_vis.addGraph(graph, g);
        m_vis.setValue(edges, null, VisualItem.INTERACTIVE, Boolean.FALSE);
        VisualItem f = (VisualItem)vg.getNode(0);
        m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
        f.setFixed(false);
        m_graph = g;
    }*/
    
    //public void update() {
    //    draw();
    //}

    public void draw() {
        synchronized(lock) { m_vis.run("draw"); }
    } 

    public void focus(boolean f) { 
        if (f) { 
            synchronized(lock) { m_vis.run("layout");  }
        } else{ 
            m_vis.cancel("layout"); 
        } 
    }



    private void initZoneManager() { 
        ForceSimulator fsim = getForceSimulator(); 
        zoneManager = new ZoneManager(m_vis, fsim); 

        Renderer nodeR = new ShapeRenderer(20);

        LabelRenderer tr = new LabelRenderer();
        tr.setRoundedCorner(8, 8 );
        DefaultRendererFactory drf = new DefaultRendererFactory();
        drf.setDefaultRenderer(nodeR);
        drf.add("INGROUP('graph.nodes')", tr);

        zoneManager.addZoneRenderer(drf, ZoneManager.CONVEXHULLZONERENDERER);
        m_vis.setRendererFactory(drf);

    }

    int dr = 0;
    int dg = 1;
    int di = 1;
    public int getZone(String label) {
        Integer zone = (Integer) groups.get(label);
        
        if (zone != null ) {
            return zone.intValue();
        }

        int r = (int) (Math.random() * 244) + 10;
        int g = (int) (Math.random() * 244) + 10;
        int b = (int) (Math.random() * 216) + 30;

        float radius = 250;

        int nzones = zoneManager.getNumberOfZones();
        float x = (float)Math.cos( 2*Math.PI *dr / dg) * (float)(di-1) * radius;
        float y = (float)Math.sin( 2*Math.PI *dr / dg) * (float)(di-1) * radius;
        System.out.println("x="+x+ " y="+y+ " dr="+dr); 
        CircularZoneShape cz = new CircularZoneShape(x, y, radius);
        dr++;
        if(dr == dg) {
            di += 2;
            dg = di*di - dg;
            dr = 0;
        }

        ZoneColors zColors = new ZoneColors( ColorLib.rgb(r, g, b), ColorLib.rgba(r, g, b, 111));
        int id = zoneManager.createAndAddZone( cz, zColors, new ZoneAttributes(label));
        Zone aZone = zoneManager.getZone(id);
        System.out.println( "Adding group(" + id  +"): " + r + ", " + g + ", " + b);
        //   setFlexible(true);
        groups.put(label, new Integer(id));
        
        zoneColors.add( zoneManager.getZoneAggregatePredicate(aZone), aZone.getColors().getFillColor());
        zoneItemColors.add( zoneManager.getZoneFocusGroupPredicate(aZone), aZone.getColors().getItemColor());

        /*int nzones = zoneManager.getNumberOfZones();
        int side = (int) Math.ceil(  Math.sqrt(nzones) );
        int z = 0;
        for(int i = 0; i < side; i++) {
            for(int j = 0; j < side; j++) {
                if (z > nzones) break;
                aZone = zoneManager.getZone(z++);
                float x = (((float)side)/2.f - (float)j)  * radius*2;
                float y = (((float)side)/2.f - (float)i)  * radius*2;
                aZone.setShape(  new CircularZoneShape(x, y, radius));
            }
        }*/
        return id;
    }

    public static class FitOverviewListener implements ItemBoundsListener {
        private Rectangle2D m_bounds = new Rectangle2D.Double();
        private Rectangle2D m_temp = new Rectangle2D.Double();
        private double m_d = 15;
        public void itemBoundsChanged(Display d) {
            d.getItemBounds(m_temp);
            GraphicsLib.expand(m_temp, 25/d.getScale());
            
            double dd = m_d/d.getScale();
            double xd = Math.abs(m_temp.getMinX()-m_bounds.getMinX());
            double yd = Math.abs(m_temp.getMinY()-m_bounds.getMinY());
            double wd = Math.abs(m_temp.getWidth()-m_bounds.getWidth());
            double hd = Math.abs(m_temp.getHeight()-m_bounds.getHeight());
            if ( xd>dd || yd>dd || wd>dd || hd>dd ) {
                m_bounds.setFrame(m_temp);
                DisplayLib.fitViewToBounds(d, m_bounds, 0);
            }
        }
    }
  
    private ForceSimulatorRemovableForces getForceSimulator() { 
        float gravConstant = -0.6f; // -1.0f; 
        float minDistance = 100f; // -1.0f; 
        float theta = 0.1f; // 0.9f; 
        float drag = 0.01f; // 0.01f; 
        float springCoeff = 1E-9f; // 1E-4f; 
        float defaultLength = 150f; // 50; 
        
        //ForceSimulator fsim; 
        //fsim = new ForceSimulator(); 
        ForceSimulatorRemovableForces fsim = new ForceSimulatorRemovableForces();

        fsim.addForce(new NBodyForce(gravConstant, minDistance, theta)); 
        fsim.addForce(new DragForce(drag)); 
        fsim.addForce(new SpringForce(springCoeff, defaultLength)); 
        return fsim; 
    }  
} // end of class ForceDirectedView
