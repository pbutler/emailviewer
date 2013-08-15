package edu.vt.Miner;

import java.util.Iterator;

import prefuse.data.tuple.TupleSet;
import prefuse.data.Tuple;

import prefuse.Visualization;
import prefuse.util.force.ForceSimulator;
import profusians.zonemanager.ZoneManager;
import profusians.zonemanager.zone.CircularZone;
import profusians.zonemanager.zone.RectangularZone;
import profusians.zonemanager.zone.attributes.ZoneAttributes;
import profusians.zonemanager.zone.colors.ZoneColors;
import profusians.zonemanager.zone.shape.ZoneShape;

public class FlexibleZone extends CircularZone {

    ZoneManager m_zManager;

    public FlexibleZone(ZoneManager zm,
	    Visualization vis, ForceSimulator fsim, ZoneShape zShape,
	    ZoneColors zColors, ZoneAttributes zAttributes) {

	super(vis, fsim, zShape, zColors, zAttributes);
	m_zManager = zm;
    }

    public float getUpdateCenterY() {
	System.out.println( "oink");
	TupleSet tset = getAllItems();
	for (Iterator<Tuple> it = tset.tuples(); it.hasNext(); ) {
		Tuple tuple = it.next();
		System.out.print( tuple.getString("_x")  );
		System.out.print( ", ");
		System.out.println( tuple.getString("_y") );
	}
	return 0.0f;
    }
    public float getUpdateCenterX() {
	    return 0.0f;
    }

    public int getUpdateRadius() {
	    return 100;
    }
}
