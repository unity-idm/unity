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
import pl.edu.icm.unity.server.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Provides access to all attributes which are managed internally in the engine.
 * @author K. Benedyczak
 */
@Component
public class SystemAttributeTypes implements SystemAttributesProvider
{
	public static final String CREDENTIAL_REQUIREMENTS = "sys:CredentialRequirements"; 
	public static final String ATTRIBUTE_CLASSES = AttributeClassUtil.ATTRIBUTE_CLASSES_ATTRIBUTE; 
	public static final String CREDENTIAL_PREFIX = "sys:Credential:";
	public static final String AUTHORIZATION_ROLE = "sys:AuthorizationRole";
	public static final String PREFERENCES = "sys:Preferences";
	public static final String LAST_AUTHENTICATION = "sys:LastAuthentication";
	public static final String FILLED_ENQUIRES = "sys:FilledEnquires";
	public static final String IGNORED_ENQUIRES = "sys:IgnoredEnquires";
	
	private AuthorizationManager authz;
	private UnityMessageSource msg;
	
	private List<AttributeType> systemAttributes = new ArrayList<AttributeType>();
	
	@Autowired
	public SystemAttributeTypes(AuthorizationManager authz, UnityMessageSource msg)
	{
		this.authz = authz;
		this.msg = msg;
		systemAttributes.add(getCredentialRequirementsAT());
		systemAttributes.add(getAuthozationRoleAT());
		systemAttributes.add(getPreferenceAT());
		systemAttributes.add(getAttributeClassesAT());
		systemAttributes.add(getLastAuthenticationAT());
		systemAttributes.add(getFilledEnquiresAT());
		systemAttributes.add(getIgnoredEnquiresAT());
	}
	
	private AttributeType getCredentialRequirementsAT()
	{
		AttributeType credentialRequiremetsAt = new AttributeType(CREDENTIAL_REQUIREMENTS, 
				new StringAttributeSyntax(), msg);
		credentialRequiremetsAt.setMaxElements(1);
		credentialRequiremetsAt.setMinElements(1);
		credentialRequiremetsAt.setVisibility(AttributeVisibility.local);
		credentialRequiremetsAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		return credentialRequiremetsAt;
	}
	
	private AttributeType getAuthozationRoleAT()
	{
		Set<String> vals = authz.getRoleNames();
		AttributeType authorizationAt = new AttributeType(AUTHORIZATION_ROLE, new EnumAttributeSyntax(vals),
				msg, AUTHORIZATION_ROLE, new Object[] {authz.getRolesDescription()});
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(10);
		authorizationAt.setUniqueValues(true);
		authorizationAt.setVisibility(AttributeVisibility.local);
		return authorizationAt;
	}
	
	private AttributeType getPreferenceAT()
	{
		AttributeType preferenceAt = new AttributeType(PREFERENCES, new StringAttributeSyntax(), msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(1);
		preferenceAt.setMaxElements(1);
		preferenceAt.setUniqueValues(false);
		preferenceAt.setVisibility(AttributeVisibility.local);
		return preferenceAt;
	}

	private AttributeType getAttributeClassesAT()
	{
		AttributeType preferenceAt = new AttributeType(ATTRIBUTE_CLASSES, new StringAttributeSyntax(), msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(AttributeClassHelper.MAX_CLASSES_PER_ENTITY);
		preferenceAt.setUniqueValues(true);
		preferenceAt.setVisibility(AttributeVisibility.local);
		return preferenceAt;
	}
	
	private AttributeType getLastAuthenticationAT()
	{
		AttributeType preferenceAt = new AttributeType(LAST_AUTHENTICATION, new StringAttributeSyntax(), msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(1);
		preferenceAt.setVisibility(AttributeVisibility.local);
		return preferenceAt;
	}

	private AttributeType getFilledEnquiresAT()
	{
		AttributeType preferenceAt = new AttributeType(FILLED_ENQUIRES, new StringAttributeSyntax(), msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(Integer.MAX_VALUE);
		preferenceAt.setUniqueValues(true);
		return preferenceAt;
	}

	private AttributeType getIgnoredEnquiresAT()
	{
		AttributeType preferenceAt = new AttributeType(IGNORED_ENQUIRES, new StringAttributeSyntax(), msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(Integer.MAX_VALUE);
		preferenceAt.setUniqueValues(true);
		return preferenceAt;
	}
	
	public List<AttributeType> getSystemAttributes()
	{
		return systemAttributes;
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}
}
