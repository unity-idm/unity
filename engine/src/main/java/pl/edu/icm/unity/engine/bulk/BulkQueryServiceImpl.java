/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipInfo;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.attribute.AttributeStatementProcessor;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.credential.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.forms.enquiry.EnquiryTargetCondEvaluator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.registration.EnquiryForm;

@Component
@Primary
class BulkQueryServiceImpl implements BulkGroupQueryService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, BulkQueryServiceImpl.class);
	
	private AttributeStatementProcessor statementsHelper;
	private EntityCredentialsHelper credentialsHelper;
	private LocalCredentialsRegistry localCredReg;
	private CompositeEntitiesInfoProvider dataProvider;
	private AuthorizationManager authz;
	
	@Autowired
	public BulkQueryServiceImpl(AttributeStatementProcessor statementsHelper,
			EntityCredentialsHelper credentialsHelper,
			LocalCredentialsRegistry localCredReg,
			CompositeEntitiesInfoProvider dataProvider,AuthorizationManager authz)
	{
		this.statementsHelper = statementsHelper;
		this.credentialsHelper = credentialsHelper;
		this.localCredReg = localCredReg;
		this.dataProvider = dataProvider;
		this.authz = authz;
	}


	@Transactional
	@Override
	public GroupMembershipData getBulkMembershipData(String group, Set<Long> filter) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getCompositeGroupContents(group, Optional.ofNullable(filter));
	}
	
	@Transactional
	@Override
	public GroupMembershipData getBulkMembershipData(String group) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getCompositeGroupContents(group, Optional.empty());
	}
	
	@Transactional
	@Override
	public GroupStructuralData getBulkStructuralData(String group) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getGroupStructuralContents(group);
	}
	
	/**
	 * @return all effective attributes of all entities in the group (including disabled ones)
	 */
	@Override
	public Map<Long, Map<String, AttributeExt>> getGroupUsersAttributes(String group, GroupMembershipData dataO)
	{
		Stopwatch watch = Stopwatch.createStarted();
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		Map<Long, Map<String, AttributeExt>> ret = new HashMap<>();
		for (Long entityId: data.getEntityInfo().keySet())
			ret.put(entityId, getAllAttributesAsMap(entityId, group, data));
		log.debug("Bulk attributes assembly: {}", watch.toString());
		return ret;
	}
	
	@Override
	public Map<Long, GroupMembershipInfo> getMembershipInfo(GroupMembershipData dataO)
	{
		Stopwatch watch = Stopwatch.createStarted();
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		Map<Long, Set<String>> memberships = data.getMemberships();
		Map<Long, List<Identity>> identities = data.getIdentities();
		Map<Long, Map<String, Map<String, AttributeExt>>> directAttributes = data.getDirectAttributes();
		Map<Long, GroupMembershipInfo> ret = new HashMap<>();

		for (Long e : memberships.keySet())
		{
			CredentialInfo credentialInfo = getCredentialInfo(e, data);
			ret.put(e, new GroupMembershipInfo(data.getEntityInfo().get(e), identities.get(e),
					memberships.get(e), directAttributes.get(e),
					getEnquiryForms(e, data, credentialInfo), getCredentialInfo(e, data)));
		}

		log.debug("Bulk members with groups: {}", watch.toString());
		return ret;
	}

	private Set<String> getEnquiryForms(Long e, GroupMembershipDataImpl data, CredentialInfo credentialInfo)
	{
		Set<String> forms = new HashSet<>();

		for (EnquiryForm enqForm : data.getEnquiryForms().values())
		{
			if (EnquiryTargetCondEvaluator.evaluateTargetCondition(enqForm,
							data.getIdentities().get(e),
							data.getEntityInfo().get(e).getEntityState().toString(),
							credentialInfo.getCredentialRequirementId(),
							data.getMemberships().get(e),
							data.getDirectAttributes().get(e).get("/").values()))
				forms.add(enqForm.getName());
		}
		return forms;
	}
	
	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(GroupMembershipData dataO)
	{
		return getGroupEntitiesNoContext(true, dataO);
	}

	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(GroupMembershipData dataO)
	{
		return getGroupEntitiesNoContext(false, dataO);
	}
	

	@Override
	public Map<String, GroupContents> getGroupAndSubgroups(GroupStructuralData dataO)
	{
		Stopwatch watch = Stopwatch.createStarted();
		Map<String, GroupContents> ret = new HashMap<>();
		GroupStructuralDataImpl data = (GroupStructuralDataImpl) dataO;
		Set<String> allGroups = data.getGroups().keySet();
		for (Group group: data.getGroups().values())
		{
			if (!Group.isChildOrSame(group.toString(), data.getGroup()))
				continue;
			GroupContents entry = new GroupContents();
			entry.setGroup(group);
			entry.setSubGroups(getDirectSubGroups(group.toString(), allGroups));
			ret.put(group.toString(), entry);
		}
		log.debug("Bulk group and subgroups resolve: {}", watch.toString());
		return ret;
	}
	
	private List<String> getDirectSubGroups(String root, Set<String> allGroups)
	{
		int prefix = root.length() + 1;
		return allGroups.stream().
				filter(g -> Group.isChild(g, root)).
				filter(g -> !g.substring(prefix).contains("/")).
				collect(Collectors.toList());
	}
	
	private Map<Long, Entity> getGroupEntitiesNoContext(boolean includeTargeted, 
			GroupMembershipData dataO)
	{
		Stopwatch watch = Stopwatch.createStarted();
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		Map<Long, Entity> ret = new HashMap<>();
		for (Long entityId: data.getEntityInfo().keySet())
			ret.put(entityId, assembleEntity(entityId, includeTargeted, data));
		log.debug("Bulk entities assembly: {}", watch.toString());
		return ret;
	}
	
	private Entity assembleEntity(long entityId, boolean includeTargeted, GroupMembershipDataImpl data)
	{
		CredentialInfo credInfo = getCredentialInfo(entityId, data);
		List<Identity> identitites = data.getIdentities().get(entityId);
		if (!includeTargeted)
			identitites = filterTargetedIdentitites(identitites);
		return new Entity(identitites, data.getEntityInfo().get(entityId), credInfo);
	}
	
	private List<Identity> filterTargetedIdentitites(List<Identity> all)
	{
		return all.stream().filter(id -> id.getTarget() == null).collect(Collectors.toList());
	}
	
	private Map<String, AttributeExt> getAllAttributesAsMap(long entityId, String group, GroupMembershipDataImpl data) 
	{
		Map<String, Map<String, AttributeExt>> directAttributesByGroup = data.getDirectAttributes().get(entityId);
		Set<String> allGroups = data.getMemberships().get(entityId);
		List<Identity> identities = data.getIdentities().get(entityId);
		return statementsHelper.getEffectiveAttributes(identities, 
				group, null, allGroups, directAttributesByGroup, 
				data.getAttributeClasses(),
				data.getGroups()::get, 
				data.getAttributeTypes()::get);
	}
	
	private CredentialInfo getCredentialInfo(long entityId, GroupMembershipDataImpl data)
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
