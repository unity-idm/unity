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

import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
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
	public static final String AUTHORIZATION_ROLE = "sys:AuthorizationRole";
	
	private AuthorizationManager authz;
	
	private List<AttributeType> systemAttributes = new ArrayList<AttributeType>();
	
	@Autowired
	public SystemAttributeTypes(AuthenticatorsRegistry authReg, AuthorizationManager authz)
	{
		this.authz = authz;
		systemAttributes.add(getCredentialRequirementsAT());
		systemAttributes.add(getCredentialsStateAT());
		systemAttributes.add(getAuthozationRoleAT());
	}
	
	private AttributeType getCredentialRequirementsAT()
	{
		AttributeType credentialRequiremetsAt = new AttributeType(CREDENTIAL_REQUIREMENTS, 
				new StringAttributeSyntax());
		credentialRequiremetsAt.setMaxElements(1);
		credentialRequiremetsAt.setMinElements(1);
		credentialRequiremetsAt.setVisibility(AttributeVisibility.local);
		credentialRequiremetsAt.setDescription("Defines which credential requirements are set for the owner");
		credentialRequiremetsAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		return credentialRequiremetsAt;
	}
	
	private AttributeType getCredentialsStateAT()
	{
		String[] vals = getEnumAsStrings(LocalAuthenticationState.values());
		AttributeType credentialStateAt = new AttributeType(CREDENTIALS_STATE, 
				new EnumAttributeSyntax(vals));
		credentialStateAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		credentialStateAt.setDescription("Holds information about the actual state of the owner's credential");
		credentialStateAt.setMinElements(1);
		credentialStateAt.setMaxElements(1);
		credentialStateAt.setVisibility(AttributeVisibility.local);
		return credentialStateAt;
	}

	private AttributeType getAuthozationRoleAT()
	{
		Set<String> vals = authz.getRoleNames();
		AttributeType authorizationAt = new AttributeType(AUTHORIZATION_ROLE, new EnumAttributeSyntax(vals));
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setDescription("Defines what operations are allowed for the bearer.");
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(10);
		authorizationAt.setUniqueValues(true);
		authorizationAt.setVisibility(AttributeVisibility.local);
		return authorizationAt;
	}
	
	private static String[] getEnumAsStrings(Enum<?>[] en)
	{
		String[] vals = new String[en.length];
		for (int i=0; i<en.length; i++)
			vals[i] = en[i].toString();
		return vals;
	}
	
	public List<AttributeType> getSystemAttributes()
	{
		return systemAttributes;
	}
}
