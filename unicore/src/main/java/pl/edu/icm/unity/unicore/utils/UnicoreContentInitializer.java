/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.utils.DemoContentInitializer;

/**
 * Populates DB with UNICORE related contents.
 * @author K. Benedyczak
 */
@Component
public class UnicoreContentInitializer implements ServerInitializer
{
	private static Logger log = Log.getLogger(Log.U_SERVER, DemoContentInitializer.class);
	public static final String NAME = "unicoreInitializer";
	private InitializerCommon commonInitializer;
	private AttributesManagement attrMan;
	private GroupsManagement groupsMan;
	
	@Autowired
	public UnicoreContentInitializer(InitializerCommon commonInitializer,
			@Qualifier("insecure") AttributesManagement attrMan, 
			@Qualifier("insecure") GroupsManagement groupsMan)
	{
		this.commonInitializer = commonInitializer;
		this.attrMan = attrMan;
		this.groupsMan = groupsMan;
	}

	@Override
	public void run()
	{
		try
		{
			commonInitializer.initializeCommonAttributeTypes();
			commonInitializer.assignCnToAdmin();
			commonInitializer.initializeCommonAttributeStatements();
			commonInitializer.initializeMainAttributeClass();
			AttributesClass unicoreAC = new AttributesClass("UNICORE portal attributes", 
					"Attributes useful for the UNICORE portal", 
					new HashSet<>(Arrays.asList(InitializerCommon.JPEG_ATTR,
							InitializerCommon.ORG_ATTR)), 
					new HashSet<>(Arrays.asList(InitializerCommon.CN_ATTR,
							InitializerCommon.EMAIL_ATTR)), false, 
					new HashSet<>(Arrays.asList(InitializerCommon.MAIN_AC)));
			attrMan.addAttributeClass(unicoreAC);
			
			Group portal = new Group("/portal");
			portal.setAttributesClasses(Collections.singleton(unicoreAC.getName()));
			groupsMan.addGroup(portal);			
			
		} catch (Exception e)
		{
			log.error("Error loading UNICORE contents", e);
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
