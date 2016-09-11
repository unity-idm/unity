/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.ServerInitializer;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Populates DB with UNICORE related contents.
 * @author K. Benedyczak
 */
@Component
public class UnicoreContentInitializer implements ServerInitializer
{
	private static Logger log = Log.getLogger(Log.U_SERVER, UnicoreContentInitializer.class);
	public static final String NAME = "unicoreInitializer";
	private InitializerCommon commonInitializer;
	private AttributeTypeManagement attrMan;
	private GroupsManagement groupsMan;
	private UnityMessageSource msg;
	private AttributeClassManagement acMan;
	
	@Autowired
	public UnicoreContentInitializer(InitializerCommon commonInitializer,
			@Qualifier("insecure") AttributeTypeManagement attrMan, 
			@Qualifier("insecure") GroupsManagement groupsMan,
			@Qualifier("insecure") AttributeClassManagement acMan,
			UnityMessageSource msg)
	{
		this.commonInitializer = commonInitializer;
		this.attrMan = attrMan;
		this.groupsMan = groupsMan;
		this.acMan = acMan;
		this.msg = msg;
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
			if (acMan.getAttributeClasses().containsKey(unicoreAC.getName()))
			{
				log.info("Seems that UNICORE contents is installed, skipping.");
				return;
			}
			acMan.addAttributeClass(unicoreAC);
			
			Set<AttributeType> existingATs = new HashSet<>(attrMan.getAttributeTypes());
			
			Set<String> allowedRoles = new HashSet<>();
			allowedRoles.add("user");
			allowedRoles.add("admin");
			allowedRoles.add("server");
			allowedRoles.add("banned");
			AttributeType roleAT = new AttributeType("urn:unicore:attrType:role", 
					EnumAttributeSyntax.ID, msg);
			EnumAttributeSyntax roleSyntax = new EnumAttributeSyntax(allowedRoles);
			roleAT.setMinElements(1);
			roleAT.setValueSyntaxConfiguration(roleSyntax.getSerializedConfiguration());
			if (!existingATs.contains(roleAT))
				attrMan.addAttributeType(roleAT);

			AttributeType xloginAT = new AttributeType("urn:unicore:attrType:xlogin", 
					StringAttributeSyntax.ID, msg);
			xloginAT.setMinElements(1);
			xloginAT.setMaxElements(16);
			StringAttributeSyntax xloginSyntax = new StringAttributeSyntax();
			xloginSyntax.setMaxLength(100);
			xloginSyntax.setMinLength(1);
			xloginAT.setValueSyntaxConfiguration(xloginSyntax.getSerializedConfiguration());
			if (!existingATs.contains(xloginAT))
				attrMan.addAttributeType(xloginAT);
			
			//TODO add the rest of UNICORE attribute types.
			
			Group portal = new Group("/portal");
			portal.setAttributesClasses(Collections.singleton(unicoreAC.getName()));
			groupsMan.addGroup(portal);			
			
		} catch (Exception e)
		{
			log.warn("Error loading default UNICORE contents. This is not critical and usaully " +
					"means that your existing data is in conflict with the loaded contents.", e);
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
