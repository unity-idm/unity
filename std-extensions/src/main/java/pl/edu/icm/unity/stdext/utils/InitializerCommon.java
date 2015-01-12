/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;

/**
 * Code to initialize popular objects. Useful for various initializers. 
 * @author K. Benedyczak
 */
@Component
public class InitializerCommon
{
	public static final String JPEG_ATTR = "jpegPhoto";
	public static final String CN_ATTR = "cn";
	public static final String ORG_ATTR = "o";
	public static final String EMAIL_ATTR = "email";
	
	public static final String MAIN_AC = "Common attributes";
	public static final String NAMING_AC = "Common identification attributes";
	
	private AttributesManagement attrMan;
	private GroupsManagement groupsMan;
	private UnityServerConfiguration config;

	@Autowired
	public InitializerCommon(@Qualifier("insecure") AttributesManagement attrMan, 
			@Qualifier("insecure") GroupsManagement groupsMan, UnityServerConfiguration config)
	{
		this.attrMan = attrMan;
		this.groupsMan = groupsMan;
		this.config = config;
	}

	public void initializeMainAttributeClass() throws EngineException
	{
		AttributesClass mainAC = new AttributesClass(MAIN_AC, 
				"General purpose attributes, should be enabled for everybody", 
				new HashSet<>(Arrays.asList("sys:AuthorizationRole")), 
				new HashSet<String>(), false, 
				new HashSet<String>());
		Map<String, AttributesClass> allAcs = attrMan.getAttributeClasses();
		if (!allAcs.containsKey(MAIN_AC))
			attrMan.addAttributeClass(mainAC);

		AttributesClass namingAC = new AttributesClass(NAMING_AC, 
				"Identification attributes, should be set for everybody to enable common system features", 
				new HashSet<String>(), 
				new HashSet<String>(Arrays.asList(CN_ATTR, EMAIL_ATTR)), false, 
				new HashSet<String>());
		if (!allAcs.containsKey(NAMING_AC))
			attrMan.addAttributeClass(namingAC);
	}
	

	public void initializeCommonAttributeStatements() throws EngineException
	{
		EverybodyStatement everybodyStmt = new EverybodyStatement(
				new EnumAttribute("sys:AuthorizationRole", 
						"/", AttributeVisibility.local,
						"Regular User"), 
				ConflictResolution.skip);
		Group rootGroup = groupsMan.getContents("/", GroupContents.METADATA).getGroup();
		rootGroup.setAttributeStatements(new AttributeStatement[]{everybodyStmt});
		groupsMan.updateGroup("/", rootGroup);
	}
	
	public void initializeCommonAttributeTypes() throws EngineException
	{
		Set<AttributeType> existingATs = new HashSet<>(attrMan.getAttributeTypes());
		
		AttributeType userPicture = new AttributeType(JPEG_ATTR, new JpegImageAttributeSyntax());
		((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxSize(2000000);
		((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxWidth(120);
		((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxHeight(120);
		userPicture.setMinElements(1);
		userPicture.setDescription("Small JPEG photo for user's profile");
		if (!existingATs.contains(userPicture))
			attrMan.addAttributeType(userPicture);

		AttributeType cn = new AttributeType(CN_ATTR, new StringAttributeSyntax());
		cn.setMinElements(1);
		cn.setDescription("Common name");
		((StringAttributeSyntax)cn.getValueType()).setMaxLength(100);
		((StringAttributeSyntax)cn.getValueType()).setMinLength(2);
		cn.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		if (!existingATs.contains(cn))
			attrMan.addAttributeType(cn);

		AttributeType org = new AttributeType(ORG_ATTR, new StringAttributeSyntax());
		org.setMinElements(1);
		org.setMaxElements(10);
		org.setDescription("Organization");
		((StringAttributeSyntax)org.getValueType()).setMaxLength(33);
		((StringAttributeSyntax)org.getValueType()).setMinLength(2);
		if (!existingATs.contains(org))
			attrMan.addAttributeType(org);

		AttributeType email = new AttributeType(EMAIL_ATTR, new StringAttributeSyntax());
		email.setMinElements(1);
		email.setDescription("E-mail");
		((StringAttributeSyntax)email.getValueType()).setMaxLength(33);
		((StringAttributeSyntax)email.getValueType()).setMinLength(5);
		((StringAttributeSyntax)email.getValueType()).setRegexp("[^@]+@.+\\..+");
		email.getMetadata().put(ContactEmailMetadataProvider.NAME, "");
		if (!existingATs.contains(email))
			attrMan.addAttributeType(email);
	}
	
	public void assignCnToAdmin() throws EngineException
	{
		String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER);
		StringAttribute cnA = new StringAttribute(CN_ATTR, "/", AttributeVisibility.full, 
				"Default Administrator");
		EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, adminU));
		try
		{
			if (attrMan.getAttributes(entity, "/", CN_ATTR).isEmpty())
				attrMan.setAttribute(entity, cnA, false);
		} catch (IllegalIdentityValueException e)
		{
			//ok - no default admin, no default CN.
		}
	}
}
