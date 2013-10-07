/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.generic.ac.AttributeClassDB;
import pl.edu.icm.unity.db.generic.ac.AttributeClassUtil;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementDB;
import pl.edu.icm.unity.engine.authn.CredentialHolder;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
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
	private AttributeClassDB acDB;
	private DBGroups dbGroups;
	private AuthenticatorsRegistry authReg;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialRequirementDB;

	
	@Autowired
	public EngineHelper(DBAttributes dbAttributes, AttributeClassDB acDB,
			DBGroups dbGroups, AuthenticatorsRegistry authReg,
			CredentialDB credentialDB, CredentialRequirementDB credentialRequirementDB)
	{
		this.dbAttributes = dbAttributes;
		this.acDB = acDB;
		this.dbGroups = dbGroups;
		this.authReg = authReg;
		this.credentialDB = credentialDB;
		this.credentialRequirementDB = credentialRequirementDB;
	}

	public void setEntityCredentialRequirements(long entityId, String credReqId, SqlSession sqlMap) 
			throws EngineException
	{
		if (!credentialRequirementDB.exists(credReqId, sqlMap))
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

	public CredentialRequirementsHolder getCredentialRequirements(String requirementName, SqlSession sqlMap) 
			throws EngineException
	{
		CredentialRequirements requirements = credentialRequirementDB.get(requirementName, sqlMap);
		List<CredentialDefinition> credDefs = credentialDB.getAll(sqlMap);
		return new CredentialRequirementsHolder(authReg, requirements, credDefs);
	}
	

	public Set<Long> getEntitiesByAttribute(String attribute, Set<String> values, SqlSession sql) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		return dbAttributes.getEntitiesBySimpleAttribute("/", attribute, values, sql);
	}
	
	/**
	 * @param desiredCredState If value is 'correct', then method checks if there is an existing credential and 
	 * if it is correct with the given CredentialHolder. If it is set and incorrect, an exception is thrown. 
	 * If the value is 'outdated' then nothing is done.
	 * If the value is 'notSet' then the credential is removed if the entity has it set. 
	 * @param entityId
	 * @param credentialChanged
	 * @param sql
	 * @throws EngineException 
	 */
	public void checkEntityCredentialState(long entityId, LocalCredentialState desiredCredState,
			CredentialHolder credentialChanged, SqlSession sql) 
			throws EngineException
	{
		if (desiredCredState == LocalCredentialState.outdated)
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
		CredentialPublicInformation currentState = 
				credentialChanged.getHandler().checkCredentialState(credential);
		if (currentState.getState() != LocalCredentialState.correct && 
				desiredCredState == LocalCredentialState.correct)
			throw new IllegalCredentialException("The new credential is not compatible with the previous definition and can not keep the credential state as correct");
	}
	
	/**
	 * Checks if the given set of attributes fulfills rules of ACs of a specified group 
	 * @throws EngineException 
	 */
	public void checkGroupAttributeClassesConsistency(List<Attribute<?>> attributes, String path, SqlSession sql) 
			throws EngineException
	{
		AttributeClassHelper helper = AttributeClassUtil.getACHelper(path, 
				new ArrayList<String>(0), acDB, dbGroups, sql);
		Set<String> attributeNames = new HashSet<>(attributes.size());
		for (Attribute<?> a: attributes)
			attributeNames.add(a.getName());
		helper.checkAttribtues(attributeNames, null);
	}
	
	public void addAttributesList(List<Attribute<?>> attributes, long entityId, SqlSession sqlMap) 
			throws EngineException
	{
		for (Attribute<?> a: attributes)
		{
			AttributeType at = dbAttributes.getAttributeType(a.getName(), sqlMap);
			if (at.isInstanceImmutable())
				throw new IllegalAttributeTypeException("The attribute with name " + at.getName() + 
						" can not be manually added");
			dbAttributes.addAttribute(entityId, a, false, sqlMap);
		}
	}
	
	/**
	 * Sets entity's credential. This is internal method which doesn't perform any authorization nor
	 * argument initialization checking.
	 * @param entityId
	 * @param credentialId
	 * @param rawCredential
	 * @param sqlMap
	 * @throws EngineException
	 */
	public void setEntityCredentialInternal(long entityId, String credentialId, String rawCredential,
			SqlSession sqlMap) throws EngineException
	{
		Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(
				entityId, "/", null, sqlMap);
		
		Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
		String credentialRequirements = (String)credReqA.getValues().get(0);
		CredentialRequirementsHolder credReqs = getCredentialRequirements(credentialRequirements, sqlMap);
		LocalCredentialVerificator handler = credReqs.getCredentialHandler(credentialId);
		if (handler == null)
			throw new IllegalCredentialException("The credential id is not among the entity's credential requirements: " + credentialId);

		String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialId;
		Attribute<?> currentCredentialA = attributes.get(credentialAttributeName);
		String currentCredential = currentCredentialA != null ? 
				(String)currentCredentialA.getValues().get(0) : null;
				
		//set credential value		
		String newCred = handler.prepareCredential(rawCredential, currentCredential);
		StringAttribute newCredentialA = new StringAttribute(credentialAttributeName, 
				"/", AttributeVisibility.local, Collections.singletonList(newCred));
		dbAttributes.addAttribute(entityId, newCredentialA, true, sqlMap);
	}
}
