/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

/**
 * Code to initialize popular objects used in many tests
 * 
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
	
	@Autowired
	@Qualifier("insecure") 
	private AttributeTypeManagement aTypeMan;
	@Autowired
	@Qualifier("insecure") 
	private AttributeClassManagement acMan;
	@Autowired
	private MessageSource msg;

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
	

	public void initializeCommonAttributeTypes() throws EngineException
	{
		Map<String, AttributeType> existingATs = aTypeMan.getAttributeTypesAsMap();
		
		AttributeType userPicture = new AttributeType(JPEG_ATTR, ImageAttributeSyntax.ID, msg);
		ImageAttributeSyntax jpegSyntax = new ImageAttributeSyntax();
		jpegSyntax.getConfig().setMaxSize(2000000);
		jpegSyntax.getConfig().setMaxWidth(120);
		jpegSyntax.getConfig().setMaxHeight(120);
		userPicture.setMinElements(1);
		userPicture.setValueSyntaxConfiguration(jpegSyntax.getSerializedConfiguration());
		
		if (!existingATs.containsKey(JPEG_ATTR))
			aTypeMan.addAttributeType(userPicture);

		AttributeType cn = new AttributeType(CN_ATTR, StringAttributeSyntax.ID, msg);
		cn.setMinElements(1);
		StringAttributeSyntax cnSyntax = new StringAttributeSyntax();
		cnSyntax.setMaxLength(100);
		cnSyntax.setMinLength(2);
		cn.setValueSyntaxConfiguration(cnSyntax.getSerializedConfiguration());
		cn.getMetadata().put(EntityNameMetadataProvider.NAME, "");
		if (!existingATs.containsKey(CN_ATTR))
			aTypeMan.addAttributeType(cn);

		AttributeType org = new AttributeType(ORG_ATTR, StringAttributeSyntax.ID, msg);
		StringAttributeSyntax orgSyntax = new StringAttributeSyntax();
		org.setMinElements(1);
		org.setMaxElements(10);
		orgSyntax.setMaxLength(33);
		orgSyntax.setMinLength(2);
		org.setValueSyntaxConfiguration(orgSyntax.getSerializedConfiguration());
		if (!existingATs.containsKey(ORG_ATTR))
			aTypeMan.addAttributeType(org);

		AttributeType verifiableEmail = new AttributeType(EMAIL_ATTR, 
				VerifiableEmailAttributeSyntax.ID, msg);
		verifiableEmail.setMinElements(1);
		verifiableEmail.setMaxElements(5);
		verifiableEmail.getMetadata().put(ContactEmailMetadataProvider.NAME, "");
		
		if (!existingATs.containsKey(EMAIL_ATTR))
			aTypeMan.addAttributeType(verifiableEmail);
	}
}
