/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sysattrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Provides access to all attributes which are managed internally in the engine.
 * @author K. Benedyczak
 */
@Component
public class SystemAttributeTypes
{
	public static final String CREDENTIAL_REQUIREMENTS = "sys:CredentialRequirements"; 
	//public static final String ATTRIBUTE_CLASSES = "sys:AttributeClasses"; 
	public static final String CREDENTIALS_STATE = "sys:CredentialsState";
	public static final String CREDENTIAL_PREFIX = "sys:Credential:";
	
	private List<AttributeType> systemAttributes = new ArrayList<AttributeType>();
	
	@Autowired
	public SystemAttributeTypes(AuthenticatorsRegistry authReg)
	{
		AttributeType credentialRequiremetsAt = new AttributeType(CREDENTIAL_REQUIREMENTS, 
				new StringAttributeSyntax());
		credentialRequiremetsAt.setMaxElements(1);
		credentialRequiremetsAt.setMinElements(1);
		credentialRequiremetsAt.setVisibility(AttributeVisibility.local);
		credentialRequiremetsAt.setDescription("Defines which credential requirements are set for the owner");
		credentialRequiremetsAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		systemAttributes.add(credentialRequiremetsAt);
		
		String[] vals = new String[LocalAuthenticationState.values().length];
		for (int i=0; i<LocalAuthenticationState.values().length; i++)
			vals[i] = LocalAuthenticationState.values()[i].toString();
		AttributeType credentialStateAt = new AttributeType(CREDENTIALS_STATE, 
				new EnumAttributeSyntax(vals));
		credentialStateAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		credentialStateAt.setDescription("Holds information about the actual state of the owner's credential");
		credentialStateAt.setMinElements(1);
		credentialStateAt.setMaxElements(1);
		credentialStateAt.setVisibility(AttributeVisibility.local);
		systemAttributes.add(credentialStateAt);
		
		Set<CredentialType> localCreds = authReg.getLocalCredentialTypes();
		for (CredentialType cred: localCreds)
		{
			AttributeType credentialAt = new AttributeType(CREDENTIAL_PREFIX+cred.getName(), 
					new StringAttributeSyntax());
			credentialAt.setMaxElements(1);
			credentialAt.setMinElements(1);
			credentialAt.setVisibility(AttributeVisibility.local);
			credentialAt.setDescription("Credential of " + cred.getName());
			credentialAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
			systemAttributes.add(credentialAt);
		}
			
	}
	
	public List<AttributeType> getSystemAttributes()
	{
		return systemAttributes;
	}
}
