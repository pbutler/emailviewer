package edu.vt.Miner;

import edu.vt.Miner.FlexibleZone;
import profusians.zonemanager.ZoneManager;
import profusians.zonemanager.zone.DefaultZoneFactory;
import profusians.zonemanager.zone.Zone;
import profusians.zonemanager.zone.attributes.ZoneAttributes;
import profusians.zonemanager.zone.colors.ZoneColors;
import profusians.zonemanager.zone.shape.ZoneShape;
import profusians.zonemanager.zone.shape.CircularZoneShape;

public class FlexibleZoneFactory extends DefaultZoneFactory {

    ZoneManager m_zManager;

    public FlexibleZoneFactory(ZoneManager zManager) {
	m_zManager = zManager;
    }

    public Zone getZone(ZoneShape zShape, ZoneColors zColors,
	    ZoneAttributes zAttributes) {

	if ( zShape instanceof CircularZoneShape) {
		System.out.println("boink");
	    return new FlexibleZone(m_zManager, m_vis, m_fsim, zShape, zColors, zAttributes);
	} else {
	    return super.getZone(zShape, zColors, zAttributes);
	}

    }

}
