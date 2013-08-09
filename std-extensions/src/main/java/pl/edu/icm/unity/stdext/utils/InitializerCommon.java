/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Code to initialize popular objects. Useful for various initializers. 
 * @author K. Benedyczak
 */
@Component
public class InitializerCommon
{
	public static final String JPEG_ATTR = "jpegImage";
	public static final String CN_ATTR = "cn";
	public static final String ORG_ATTR = "o";
	public static final String EMAIL_ATTR = "email";
	private AttributesManagement attrMan;

	@Autowired
	public InitializerCommon(@Qualifier("insecure") AttributesManagement attrMan)
	{
		this.attrMan = attrMan;
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
		if (!existingATs.contains(cn))
			attrMan.addAttributeType(cn);

		AttributeType org = new AttributeType(ORG_ATTR, new StringAttributeSyntax());
		org.setMinElements(1);
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
		if (!existingATs.contains(email))
			attrMan.addAttributeType(email);
	}
}
