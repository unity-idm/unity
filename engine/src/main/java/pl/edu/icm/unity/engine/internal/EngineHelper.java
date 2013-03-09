/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.AuthenticationManagementImpl;
import pl.edu.icm.unity.engine.authn.AuthenticatorImpl;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Misc operations on entities, attributes and generic objects useful for multiple
 * *ManagementImpl classes.
 * @author K. Benedyczak
 */
@Component
public class EngineHelper
{
	private DBAttributes dbAttributes;
	private DBGeneric dbGeneric;
	private AuthenticatorsRegistry authReg;
	private IdentityResolver identityResolver;
	
	@Autowired
	public EngineHelper(DBAttributes dbAttributes, DBGeneric dbGeneric,
			AuthenticatorsRegistry authReg, IdentityResolver identityResolver)
	{
		super();
		this.dbAttributes = dbAttributes;
		this.dbGeneric = dbGeneric;
		this.authReg = authReg;
		this.identityResolver = identityResolver;
	}
	
	
	public void setEntityAuthenticationState(long entityId, LocalAuthenticationState authnState, SqlSession sqlMap)
	{
		EnumAttribute authnStateA = new EnumAttribute(SystemAttributeTypes.CREDENTIALS_STATE,
				"/", AttributeVisibility.local, authnState.toString());
		dbAttributes.addAttribute(entityId, authnStateA, true, sqlMap);
	}



	public void setEntityCredentialRequirements(long entityId, String credReqId, SqlSession sqlMap)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(credReqId, 
				AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sqlMap);
		if (raw == null)
			throw new IllegalArgumentException("There is no required credential set with id " + credReqId);
		setEntityCredentialRequirementsNoCheck(entityId, credReqId, sqlMap);
	}

	public void setEntityCredentialRequirementsNoCheck(long entityId, String credReqId, SqlSession sqlMap)
	{
		StringAttribute credReq = new StringAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
				"/", AttributeVisibility.local, credReqId);
		dbAttributes.addAttribute(entityId, credReq, true, sqlMap);
	}

	public Set<Long> getEntitiesByAttribute(String attribute, String value, SqlSession sql)
	{
		return dbAttributes.getEntitiesBySimpleAttribute("/", attribute, value, sql);
	}

	public CredentialRequirementsHolder getCredentialRequirements(String requirementName, SqlSession sqlMap)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(requirementName, 
				AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sqlMap);
		if (raw == null)
			throw new RuntimeEngineException("The credential requirement is unknown: " 
					+ requirementName);
		return resolveCredentialRequirementsBean(raw, sqlMap);
	}
	
	public CredentialRequirementsHolder resolveCredentialRequirementsBean(GenericObjectBean raw, SqlSession sqlMap)
	{
		CredentialRequirementsHolder helper = new CredentialRequirementsHolder(authReg);
		String contents = new String(raw.getContents(), Constants.UTF);
		helper.setSerializedConfiguration(contents);
		return helper;
	}
	
	
	/**
	 * Credential state of the entity is updated to desired state. disabled is simply set.
	 * valid is set only if all credentials are matching the new definition, otherwise exception is thrown.
	 * outdated is set if some of the credentials are invalid, otherwise valid is set.
	 * @param entityId
	 * @param desiredAuthnState
	 * @param newCredReqs
	 * @param sql
	 */
	public void updateEntityCredentialState(long entityId, LocalAuthenticationState desiredAuthnState, 
			CredentialRequirementsHolder newCredReqs, SqlSession sql)
	{
		LocalAuthenticationState toSet;
		if (desiredAuthnState.equals(LocalAuthenticationState.disabled))
		{
			toSet = LocalAuthenticationState.disabled;
		} else
		{
			Map<String, Attribute<?>> attributes = dbAttributes.getAllAttributesAsMap(entityId, "/", null, sql);
			boolean allValid = newCredReqs.areAllCredentialsValid(attributes);
			if (desiredAuthnState.equals(LocalAuthenticationState.valid) && !allValid)
				throw new IllegalCredentialException("The new credential requirements are not compatible with the previous definition and can not keep the authentication state as valid");
			toSet = allValid ? LocalAuthenticationState.valid : LocalAuthenticationState.outdated;
		}
		setEntityAuthenticationState(entityId, toSet, sql);
	}
	
	public AuthenticatorImpl getAuthenticator(String id, SqlSession sql)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(id, 
				AuthenticationManagementImpl.AUTHENTICATOR_OBJECT_TYPE, sql);
		if (raw == null)
			throw new pl.edu.icm.unity.exceptions.IllegalArgumentException("The authenticator " + id + " is not known");
		return getAuthenticatorNoCheck(raw, sql);
	}

	public AuthenticatorImpl getAuthenticatorNoCheck(GenericObjectBean raw, SqlSession sql)
	{
		AuthenticatorImpl authenticator = new AuthenticatorImpl(identityResolver, authReg);
		String contents = new String(raw.getContents(), Constants.UTF);
		authenticator.setSerializedConfiguration(contents);
		return authenticator;
	}
}
