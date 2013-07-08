/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import pl.edu.icm.unity.engine.authn.CredentialHolder;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.AttributeExt;
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
	
	
	public void setEntityCredentialRequirements(long entityId, String credReqId, SqlSession sqlMap) 
			throws IllegalAttributeValueException, IllegalTypeException, IllegalAttributeTypeException, IllegalGroupValueException
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(credReqId, 
				AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sqlMap);
		if (raw == null)
			throw new IllegalArgumentException("There is no required credential set with id " + credReqId);
		setEntityCredentialRequirementsNoCheck(entityId, credReqId, sqlMap);
	}

	public void setEntityCredentialRequirementsNoCheck(long entityId, String credReqId, SqlSession sqlMap) 
			throws IllegalAttributeValueException, IllegalTypeException, IllegalAttributeTypeException, IllegalGroupValueException
	{
		StringAttribute credReq = new StringAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
				"/", AttributeVisibility.local, credReqId);
		dbAttributes.addAttribute(entityId, credReq, true, sqlMap);
	}

	public Set<Long> getEntitiesByAttribute(String attribute, Set<String> values, SqlSession sql) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		return dbAttributes.getEntitiesBySimpleAttribute("/", attribute, values, sql);
	}

	public CredentialRequirementsHolder getCredentialRequirements(String requirementName, SqlSession sqlMap) 
			throws IllegalCredentialException
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(requirementName, 
				AuthenticationManagementImpl.CREDENTIAL_REQ_OBJECT_TYPE, sqlMap);
		if (raw == null)
			throw new IllegalCredentialException("The credential requirement is unknown: " 
					+ requirementName);
		List<CredentialDefinition> credDefs = getCredentialDefinitions(sqlMap);
		return new CredentialRequirementsHolder(authReg, raw.getContents(), credDefs);
	}
	
	public CredentialHolder resolveCredentialBean(GenericObjectBean raw, SqlSession sqlMap)
	{
		CredentialHolder helper = new CredentialHolder(authReg);
		String contents = new String(raw.getContents(), Constants.UTF);
		helper.setSerializedConfiguration(contents);
		return helper;
	}

	/**
	 * @param desiredCredState If value is 'correct', then method checks if there is an existing credential and 
	 * if it is correct with the given CredentialHolder. If it is set and incorrect, an exception is thrown. 
	 * If the value is 'outdated' then nothing is done.
	 * If the value is 'notSet' then the credential is removed if the entity has it set. 
	 * @param entityId
	 * @param credentialChanged
	 * @param sql
	 * @throws IllegalCredentialException
	 * @throws IllegalTypeException
	 * @throws IllegalGroupValueException
	 * @throws IllegalAttributeValueException
	 * @throws IllegalAttributeTypeException
	 */
	public void checkEntityCredentialState(long entityId, LocalCredentialState desiredCredState,
			CredentialHolder credentialChanged, SqlSession sql) 
			throws IllegalCredentialException, IllegalTypeException, IllegalGroupValueException,
			IllegalAttributeValueException, IllegalAttributeTypeException
	{
		if (desiredCredState == LocalCredentialState.notSet)
			return;
		String credAttribute = SystemAttributeTypes.CREDENTIAL_PREFIX+
				credentialChanged.getCredentialDefinition().getName(); 
		Collection<AttributeExt<?>> attributes = dbAttributes.getAllAttributes(entityId, "/", false,
				credAttribute, sql);
		if (attributes.isEmpty())
			return;
		if (desiredCredState == LocalCredentialState.notSet)
		{
			dbAttributes.removeAttribute(entityId, "/", credAttribute, sql);
			return;
		}
		String credential = (String)attributes.iterator().next().getValues().get(0);
		LocalCredentialState currentState = credentialChanged.getHandler().checkCredentialState(credential);
		if (currentState != LocalCredentialState.correct && desiredCredState == LocalCredentialState.correct)
			throw new IllegalCredentialException("The new credential is not compatible with the previous definition and can not keep the credential state as correct");
	}
	
	public AuthenticatorImpl getAuthenticator(String id, SqlSession sql) 
			throws WrongArgumentException
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(id, 
				AuthenticationManagementImpl.AUTHENTICATOR_OBJECT_TYPE, sql);
		if (raw == null)
			throw new WrongArgumentException("The authenticator " + id + " is not known");
		AuthenticatorImpl ret = getAuthenticatorNoCheck(raw, sql);
		return ret;
	}

	public AuthenticatorImpl getAuthenticatorNoCheck(GenericObjectBean raw, SqlSession sql) 
			throws WrongArgumentException
	{
		AuthenticatorImpl authenticator = new AuthenticatorImpl(identityResolver, authReg, raw.getName());
		String contents = new String(raw.getContents(), Constants.UTF);
		authenticator.setSerializedConfiguration(contents);
		String localCredential = authenticator.getAuthenticatorInstance().getLocalCredentialName(); 
		if (localCredential != null)
		{
			GenericObjectBean rawC = dbGeneric.getObjectByNameType(localCredential, 
					AuthenticationManagementImpl.CREDENTIAL_OBJECT_TYPE, sql);
			if (rawC == null)
				throw new WrongArgumentException("The authenticator's " + 
						authenticator.getAuthenticatorInstance().getId() + 
						" credential is not known: " + localCredential);
			CredentialHolder credential = resolveCredentialBean(rawC, sql);
			authenticator.setVerificatorConfiguration(credential.getCredentialDefinition().
					getJsonConfiguration());
		}
		return authenticator;
	}
	
	public List<CredentialDefinition> getCredentialDefinitions(SqlSession sql)
	{
		List<CredentialDefinition> ret = new ArrayList<CredentialDefinition>();
		List<GenericObjectBean> raw = dbGeneric.getObjectsOfType(
				AuthenticationManagementImpl.CREDENTIAL_OBJECT_TYPE, sql);
		for (GenericObjectBean rawA: raw)
		{
			CredentialHolder helper = resolveCredentialBean(rawA, sql);
			ret.add(helper.getCredentialDefinition());
		}
		return ret;
	}
}
