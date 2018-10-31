/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.CompositeGroupContents;
import pl.edu.icm.unity.engine.attribute.AttributeStatementProcessor;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.credential.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;

@Component
class BulkQueryServiceImpl implements BulkGroupQueryService
{
	@Autowired
	private AttributeStatementProcessor statementsHelper;
	@Autowired
	private EntityCredentialsHelper credentialsHelper;
	@Autowired
	private LocalCredentialsRegistry localCredReg;
	@Autowired
	private CompositeEntitiesInfoProvider dataProvider;
	@Autowired
	private AuthorizationManager authz;
	
	@Override
	public CompositeGroupContents getBulkDataForGroup(String group) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getCompositeGroupContents(group);
	}
	
	/**
	 * @return all effective attributes of all entities in the group (including disabled ones)
	 */
	@Override
	public Map<Long, Map<String, AttributeExt>> getGroupUsersAttributes(String group, CompositeGroupContents dataO)
	{
		CompositeGroupContentsImpl data = (CompositeGroupContentsImpl) dataO;
		Map<Long, Map<String, AttributeExt>> ret = new HashMap<>();
		for (Long entityId: data.getEntityInfo().keySet())
			ret.put(entityId, getAllAttributesAsMap(entityId, group, data));
		return ret;
	}

	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(String group, CompositeGroupContents dataO)
	{
		return getGroupEntitiesNoContextWithTargeted(group, true, dataO);
	}

	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(String group, CompositeGroupContents dataO)
	{
		return getGroupEntitiesNoContextWithTargeted(group, false, dataO);
	}
	
	private Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(String group, boolean includeTargeted, 
			CompositeGroupContents dataO)
	{
		CompositeGroupContentsImpl data = (CompositeGroupContentsImpl) dataO;
		Map<Long, Entity> ret = new HashMap<>();
		for (Long entityId: data.getEntityInfo().keySet())
			ret.put(entityId, assembleEntity(entityId, includeTargeted, data));
		return ret;
	}
	
	private Entity assembleEntity(long entityId, boolean includeTargeted, CompositeGroupContentsImpl data)
	{
		CredentialInfo credInfo = getCredentialInfo(entityId, data);
		List<Identity> identitites = data.getIdentities().get(entityId);
		if (!includeTargeted)
			identitites = filterTargetedIdentitites(identitites);
		return new Entity(identitites, data.getEntityInfo().get(entityId), credInfo);
	}
	
	private List<Identity> filterTargetedIdentitites(List<Identity> all)
	{
		return all.stream().filter(id -> id.getTarget() != null).collect(Collectors.toList());
	}
	
	private Map<String, AttributeExt> getAllAttributesAsMap(long entityId, String groupPath, 
			CompositeGroupContentsImpl data) 
	{
		Map<String, Map<String, AttributeExt>> directAttributesByGroup = data.getDirectAttributes().get(entityId);
		Set<String> allGroups = data.getMemberships().get(entityId);
		List<Identity> identities = data.getIdentities().get(entityId);
		return statementsHelper.getEffectiveAttributes(identities, 
				groupPath, null, allGroups, directAttributesByGroup, 
				data.getAttributeClasses(),
				data.getGroups()::get, 
				data.getAttributeTypes()::get);
	}
	
	private CredentialInfo getCredentialInfo(long entityId, CompositeGroupContentsImpl data)
	{
		Map<String, AttributeExt> attributes = data.getDirectAttributes().get(entityId).get("/");
		String credentialRequirementId = credentialsHelper.getCredentialReqFromAttribute(attributes);
		
		CredentialRequirementsHolder credReq;
		try
		{
			credReq = new CredentialRequirementsHolder(localCredReg, 
					data.getCredentialRequirements().get(credentialRequirementId), 
					data.getCredentials());
		} catch (IllegalCredentialException e)
		{
			throw new InternalException("Unknown credential assigned to entity", e);
		}
		return credentialsHelper.getCredentialInfoNoQuery(entityId, attributes, credReq, credentialRequirementId);
	}
}
