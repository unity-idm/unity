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

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Code to initialize popular objects. Useful for various initializers.
 * 
 * TODO Deprecated: should be replaced with the new style initializers. 
 * @author K. Benedyczak
 */
@Component
@Deprecated
public class InitializerCommon
{
	public static final String JPEG_ATTR = "jpegPhoto";
	public static final String CN_ATTR = "cn";
	public static final String ORG_ATTR = "o";
	public static final String EMAIL_ATTR = "email";
	
	public static final String MAIN_AC = "Common attributes";
	public static final String NAMING_AC = "Common identification attributes";
	
	@Autowired
	@Qualifier("insecure") 
	private AttributesManagement attrMan;
	@Autowired
	@Qualifier("insecure") 
	private AttributeTypeManagement aTypeMan;
	@Autowired
	@Qualifier("insecure") 
	private AttributeClassManagement acMan;
	@Autowired
	@Qualifier("insecure") 
	private GroupsManagement groupsMan;
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private UnityMessageSource msg;

	public void initializeMainAttributeClass() throws EngineException
	{
		AttributesClass mainAC = new AttributesClass(MAIN_AC, 
				"General purpose attributes, should be enabled for everybody", 
				new HashSet<>(Arrays.asList("sys:AuthorizationRole")), 
				new HashSet<String>(), false, 
				new HashSet<String>());
		Map<String, AttributesClass> allAcs = acMan.getAttributeClasses();
		if (!allAcs.containsKey(MAIN_AC))
			acMan.addAttributeClass(mainAC);

		AttributesClass namingAC = new AttributesClass(NAMING_AC, 
				"Identification attributes, should be set for everybody to enable common system features", 
				new HashSet<String>(Arrays.asList(ORG_ATTR, JPEG_ATTR)), 
				new HashSet<String>(Arrays.asList(CN_ATTR, EMAIL_ATTR)), false, 
				new HashSet<String>());
		if (!allAcs.containsKey(NAMING_AC))
			acMan.addAttributeClass(namingAC);
	}
	

	public void initializeCommonAttributeStatements() throws EngineException
	{
		AttributeStatement everybodyStmt = AttributeStatement.getFixedEverybodyStatement(
				EnumAttribute.of("sys:AuthorizationRole", 
				"/", 
				"Regular User")); 
		Group rootGroup = groupsMan.getContents("/", GroupContents.METADATA).getGroup();
		rootGroup.setAttributeStatements(new AttributeStatement[]{everybodyStmt});
		groupsMan.updateGroup("/", rootGroup);
	}
	
	public void initializeCommonAttributeTypes() throws EngineException
	{
		Set<AttributeType> existingATs = new HashSet<>(aTypeMan.getAttributeTypes());
		
		AttributeType userPicture = new AttributeType(JPEG_ATTR, JpegImageAttributeSyntax.ID, msg);
		JpegImageAttributeSyntax jpegSyntax = new JpegImageAttributeSyntax();
		jpegSyntax.setMaxSize(2000000);
		jpegSyntax.setMaxWidth(120);
		jpegSyntax.setMaxHeight(120);
		userPicture.setMinElements(1);
		userPicture.setValueSyntaxConfiguration(jpegSyntax.getSerializedConfiguration());
		if (!existingATs.contains(userPicture))
			aTypeMan.addAttributeType(userPicture);

		AttributeType cn = new AttributeType(CN_ATTR, StringAttributeSyntax.ID, msg);
		cn.setMinElements(1);
		StringAttributeSyntax cnSyntax = new StringAttributeSyntax();
		cnSyntax.setMaxLength(100);
		cnSyntax.setMinLength(2);
		cn.setValueSyntaxConfiguration(cnSyntax.getSerializedConfiguration());
		cn.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		if (!existingATs.contains(cn))
			aTypeMan.addAttributeType(cn);

		AttributeType org = new AttributeType(ORG_ATTR, StringAttributeSyntax.ID, msg);
		StringAttributeSyntax orgSyntax = new StringAttributeSyntax();
		org.setMinElements(1);
		org.setMaxElements(10);
		orgSyntax.setMaxLength(33);
		orgSyntax.setMinLength(2);
		org.setValueSyntaxConfiguration(orgSyntax.getSerializedConfiguration());
		if (!existingATs.contains(org))
			aTypeMan.addAttributeType(org);

		AttributeType verifiableEmail = new AttributeType(EMAIL_ATTR, 
				VerifiableEmailAttributeSyntax.ID, msg);
		verifiableEmail.setMinElements(1);
		verifiableEmail.setMaxElements(5);
		verifiableEmail.getMetadata().put(ContactEmailMetadataProvider.NAME, "");
		
		if (!existingATs.contains(verifiableEmail))
			aTypeMan.addAttributeType(verifiableEmail);
		
		
	}
	
	public void assignCnToAdmin() throws EngineException
	{
		String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER);
		Attribute cnA = StringAttribute.of(CN_ATTR, "/", "Default Administrator");
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
