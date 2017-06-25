/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Defines string attribute type used to store user's credential requirements and the prefix for attributes
 * storing credentials.
 * 
 * @author K. Benedyczak
 */
@Component
public class CredentialAttributeTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String CREDENTIAL_REQUIREMENTS = "sys:CredentialRequirements"; 
	public static final String CREDENTIAL_PREFIX = "sys:Credential:";
	
	@Autowired
	public CredentialAttributeTypeProvider(UnityMessageSource msg)
	{
		super(msg);
	}
	
	@Override
	protected AttributeType getAttributeType()
	{
		AttributeType credentialRequiremetsAt = new AttributeType(CREDENTIAL_REQUIREMENTS, 
				StringAttributeSyntax.ID, msg);
		credentialRequiremetsAt.setMaxElements(1);
		credentialRequiremetsAt.setMinElements(1);
		credentialRequiremetsAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | 
				AttributeType.INSTANCES_IMMUTABLE_FLAG);
		return credentialRequiremetsAt;
	}
}
