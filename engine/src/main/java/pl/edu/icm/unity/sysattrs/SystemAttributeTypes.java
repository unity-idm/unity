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

import pl.edu.icm.unity.db.generic.ac.AttributeClassUtil;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
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
	public static final String ATTRIBUTE_CLASSES = AttributeClassUtil.ATTRIBUTE_CLASSES_ATTRIBUTE; 
	public static final String CREDENTIAL_PREFIX = "sys:Credential:";
	public static final String AUTHORIZATION_ROLE = "sys:AuthorizationRole";
	public static final String PREFERENCES = "sys:Preferences";
	
	private AuthorizationManager authz;
	
	private List<AttributeType> systemAttributes = new ArrayList<AttributeType>();
	
	@Autowired
	public SystemAttributeTypes(AuthenticatorsRegistry authReg, AuthorizationManager authz)
	{
		this.authz = authz;
		systemAttributes.add(getCredentialRequirementsAT());
		systemAttributes.add(getAuthozationRoleAT());
		systemAttributes.add(getPreferenceAT());
		systemAttributes.add(getAttributeClassesAT());
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
	
	private AttributeType getPreferenceAT()
	{
		AttributeType preferenceAt = new AttributeType(PREFERENCES, new StringAttributeSyntax());
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setDescription("Preferences of the user.");
		preferenceAt.setMinElements(1);
		preferenceAt.setMaxElements(1);
		preferenceAt.setUniqueValues(false);
		preferenceAt.setVisibility(AttributeVisibility.local);
		return preferenceAt;
	}

	private AttributeType getAttributeClassesAT()
	{
		AttributeType preferenceAt = new AttributeType(ATTRIBUTE_CLASSES, new StringAttributeSyntax());
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setDescription("Attribute classes of the user.");
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(AttributeClassHelper.MAX_CLASSES_PER_ENTITY);
		preferenceAt.setUniqueValues(true);
		preferenceAt.setVisibility(AttributeVisibility.local);
		return preferenceAt;
	}
	
	public List<AttributeType> getSystemAttributes()
	{
		return systemAttributes;
	}
}
