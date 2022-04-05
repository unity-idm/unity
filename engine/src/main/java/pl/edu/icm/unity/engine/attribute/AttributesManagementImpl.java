/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.audit.AuditEventTrigger;
import pl.edu.icm.unity.engine.audit.AuditPublisher;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.session.AdditionalAuthenticationService;
import pl.edu.icm.unity.exceptions.*;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.*;
import pl.edu.icm.unity.types.basic.audit.AuditEventAction;
import pl.edu.icm.unity.types.basic.audit.AuditEventTag;
import pl.edu.icm.unity.types.basic.audit.AuditEventType;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class AttributesManagementImpl implements AttributesManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributesManagementImpl.class);
	private AttributeClassUtil acUtil;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeDAO dbAttributes;
	private EntityResolver idResolver;
	private InternalAuthorizationManager authz;
	private AttributesHelper attributesHelper;
	private EmailConfirmationManager confirmationManager;
	private TransactionalRunner txRunner;
	private AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private AdditionalAuthenticationService additionalAuthnService;
	private final AuditPublisher audit;
	private final MembershipDAO membershipDAO;

	@Autowired
	public AttributesManagementImpl(AttributeClassUtil acUtil,
			AttributeTypeDAO attributeTypeDAO, AttributeDAO dbAttributes,
			EntityResolver idResolver, InternalAuthorizationManager authz,
			AttributesHelper attributesHelper, EmailConfirmationManager confirmationManager,
			TransactionalRunner txRunner,
			AttributeMetadataProvidersRegistry atMetaProvidersRegistry,
			AdditionalAuthenticationService repeatedAuthnService,
			AuditPublisher audit,
			MembershipDAO membershipDAO)
	{
		this.acUtil = acUtil;
		this.attributeTypeDAO = attributeTypeDAO;
		this.dbAttributes = dbAttributes;
		this.idResolver = idResolver;
		this.authz = authz;
		this.attributesHelper = attributesHelper;
		this.confirmationManager = confirmationManager;
		this.txRunner = txRunner;
		this.atMetaProvidersRegistry = atMetaProvidersRegistry;
		this.additionalAuthnService = repeatedAuthnService;
		this.audit = audit;	
		this.membershipDAO = membershipDAO;
	}

	

	@Override
	public void createAttribute(EntityParam entity, Attribute attribute) throws EngineException
	{
		setAttribute(entity, attribute, false, true);
	}

	@Override
	public void setAttribute(EntityParam entity, Attribute attribute) throws EngineException
	{
		setAttribute(entity, attribute, true, true);
	}

	@Override
	public void createAttributeSuppressingConfirmation(EntityParam entity, Attribute attribute)
			throws EngineException
	{
		setAttribute(entity, attribute, false, false);
	}

	@Override
	public void setAttributeSuppressingConfirmation(EntityParam entity, Attribute attribute)
			throws EngineException
	{
		setAttribute(entity, attribute, true, false);
	}
	
	@Override
	public void setAttribute(EntityParam entity, Attribute attribute, boolean allowUpdate)
			throws EngineException
	{
		setAttribute(entity, attribute, allowUpdate, true);
	}
	
	private void setAttribute(EntityParam entity, Attribute attribute, boolean allowUpdate, 
			boolean sendConfirmations) throws EngineException
	{
		entity.validateInitialization(); 
		txRunner.runInTransactionThrowing(() -> {
			//Important - attributes can be also set as a result of addMember and addEntity.
			//  when changing this method, verify if those needs an update too.
			
			long entityId = idResolver.getEntityId(entity);
			AttributeType at = attributeTypeDAO.get(attribute.getName());
			boolean fullAuthz = checkSetAttributeAuthz(entityId, at, attribute);
			if (!fullAuthz)
				checkAdditionalAuthn(at);
			checkIfAllowed(entityId, attribute.getGroupPath(), attribute.getName());
			attributesHelper.addAttribute(entityId, attribute, at, allowUpdate, fullAuthz);
		});

		//this is merely to propagate the change to authz layer more quickly in typical situations. It does 
		// not guarantee that authz cache is cleared after all possible situations when roles are be altered. 
		if (RoleAttributeTypeProvider.AUTHORIZATION_ROLE.equals(attribute.getName()))
			authz.clearCache();
		if (sendConfirmations)
			confirmationManager.sendVerificationQuietNoTx(entity, attribute, false);
	}

	private void checkAdditionalAuthn(AttributeType at)
	{
		if (isSensitiveAttributeChange(at))
		{
			log.info("Additional authentication triggered for sensitive >{}< attribute change", at.getName());
			additionalAuthnService.checkAdditionalAuthenticationRequirements();
		}
	}
	
	private boolean isSensitiveAttributeChange(AttributeType at)
	{
		Set<String> metadataSet = at.getMetadata().keySet();
		for (String meta: metadataSet)
		{
			AttributeMetadataProvider metaInfo = atMetaProvidersRegistry.getByName(meta);
			if (metaInfo.isSecuritySensitive())
				return true;
		}
		return false;
	}
	
	private boolean checkSetAttributeAuthz(long entityId, AttributeType at, Attribute attribute) 
			throws AuthorizationException
	{
		if (RoleAttributeTypeProvider.AUTHORIZATION_ROLE.equals(attribute.getName()))
		{
			authz.checkAuthZAttributeChangeAuthorization(authz.isSelf(entityId), attribute);
			return true;
		}
		
		boolean fullAuthz = hasFullAuthzToChangeAttr(attribute.getGroupPath());

		//even if we have fullAuthz we need to check authZ (e.g. to get outdated credential error)
		authz.checkAuthorization(at.isSelfModificable() && authz.isSelf(entityId), 
				attribute.getGroupPath(), AuthzCapability.attributeModify);
		return fullAuthz;
	}
	
	private boolean hasFullAuthzToChangeAttr(String groupPath) throws AuthorizationException
	{
		Set<AuthzCapability> nonSelfCapabilities = authz.getCapabilities(false, groupPath);
		return nonSelfCapabilities.contains(AuthzCapability.attributeModify);
	}
	
	@Override
	@Transactional
	public void removeAttribute(EntityParam entity, String groupPath, String attributeTypeId)
			throws EngineException
	{
		if (groupPath == null)
			throw new IllegalGroupValueException("Group must not be null");
		if (attributeTypeId == null)
			throw new IllegalAttributeValueException("Attribute name must not be null");
		entity.validateInitialization();

		long entityId = idResolver.getEntityId(entity);
		AttributeType at = attributeTypeDAO.get(attributeTypeId);
		if (at.isInstanceImmutable())
			throw new SchemaConsistencyException("The attribute with name " + at.getName() + 
					" can not be manually modified");
		authz.checkAuthorization(at.isSelfModificable() && authz.isSelf(entityId),
				groupPath, AuthzCapability.attributeModify);
		boolean fullAuthz = hasFullAuthzToChangeAttr(groupPath);
		if (!fullAuthz)
			checkAdditionalAuthn(at);
		
		checkIfMandatory(entityId, groupPath, attributeTypeId);
		
		dbAttributes.deleteAttribute(attributeTypeId, entityId, groupPath);
		
		audit.log(AuditEventTrigger.builder()
				.type(AuditEventType.ATTRIBUTE)
				.action(AuditEventAction.REMOVE)
				.name(attributeTypeId)
				.subject(entityId)
				.details(ImmutableMap.of("group", groupPath))
				.tags(AuditEventTag.USERS));
	}

	@Override
	@Transactional
	public Collection<AttributeExt> getAttributes(EntityParam entity, String groupPath,
			String attributeTypeId) throws EngineException
	{
		Collection<AttributeExt> ret = getAllAttributesInternal(entity, true, groupPath,
				attributeTypeId, new AuthzCapability[] {AuthzCapability.read}, false
		);
		return filterSecuritySensitive(ret);
	}

	private Collection<AttributeExt> filterSecuritySensitive(Collection<AttributeExt> ret)
	{
		return ret.stream()
				.filter(SensitiveAttributeMatcher::isNotSensitive)
				.collect(toList());
	}


	@Override
	@Transactional
	public Collection<AttributeExt> getAllAttributes(EntityParam entity, boolean effective, String groupPath,
			String attributeTypeId, boolean allowDegrade) throws EngineException
	{
		try
		{
			return getAllAttributesInternal(entity, effective, groupPath, attributeTypeId,
				new AuthzCapability[] {AuthzCapability.readHidden, AuthzCapability.read}, true);
		} catch (AuthorizationException e)
		{
			if (allowDegrade)
			{
				return getAllAttributesInternal(entity, effective,
					groupPath, attributeTypeId,
					new AuthzCapability[] {AuthzCapability.read}, false);
			} else
				throw e;
		}
	}

	@Override
	@Transactional
	public Collection<AttributeExt> getAllAttributes(EntityParam entity, boolean effective,
			List<GroupPattern> groupPathPatterns, String attributeTypeId, boolean allowDegrade) throws EngineException
	{
		try
		{
			return getAllAttributesInternal(entity, effective, groupPathPatterns, attributeTypeId,
				new AuthzCapability[] {AuthzCapability.readHidden, AuthzCapability.read}, true);
		} catch (AuthorizationException e)
		{
			if (allowDegrade)
			{
				return getAllAttributesInternal(entity, effective,
					groupPathPatterns, attributeTypeId,
					new AuthzCapability[] {AuthzCapability.read}, false);
			} else
				throw e;
		}
	}

	@Override
	@Transactional
	public Collection<AttributeExt> getAllDirectAttributes(EntityParam entity)
	{
		authz.checkAuthorizationRT(AuthzCapability.readHidden, AuthzCapability.read);		try
		{
			long entityId = idResolver.getEntityId(entity);
			return attributesHelper.getAllEntityAttributesMap(entityId)
					.values().stream()
					.map(Map::values)
					.flatMap(Collection::stream)
					.collect(toList());
		} catch (EngineException e)
		{
			throw new RuntimeEngineException(e);
		}
	}

	private Collection<AttributeExt> getAllAttributesInternal(EntityParam entity, boolean effective,
			List<GroupPattern> groupPathPatterns, String attributeTypeName, AuthzCapability[] requiredCapability,
			boolean allowDisabled) throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);

		List<String> groupsPaths = getGroupsPaths(groupPathPatterns, entityId);
		if(groupsPaths.isEmpty())
			return emptyList();

		for (String group : groupsPaths)
		{
			authz.checkAuthorization(authz.isSelf(entityId), group, requiredCapability);
		}
		return attributesHelper.getAttributesInternal(entityId,
				effective, groupsPaths, attributeTypeName, allowDisabled);
	}

	private Collection<AttributeExt> getAllAttributesInternal(EntityParam entity, boolean effective,
			String groupPath, String attributeTypeName, AuthzCapability[] requiredCapability, boolean allowDisabled)
			throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), groupPath, requiredCapability);
		return attributesHelper.getAttributesInternal(entityId,
			effective, groupPath, attributeTypeName, allowDisabled);
	}

	private List<String> getGroupsPaths(List<GroupPattern> groupPathPatterns, long entityId)
	{
		List<Group> entityGroups = membershipDAO.getEntityMembershipGroups(entityId);
		List<String> groupsPaths = new ArrayList<>();
		for(GroupPattern groupPathPattern : groupPathPatterns)
		{
			groupsPaths.addAll(getMatchingGroups(entityGroups, groupPathPattern.pattern));
		}
		return groupsPaths;
	}

	private List<String> getMatchingGroups(List<Group> entityGroups, String groupPathPattern)
	{
		return GroupPatternMatcher.filterMatching(entityGroups, groupPathPattern).stream()
			.map(Group::getName)
			.collect(toList());
	}

	/**
	 * Verifies if the attribute is allowed wrt attribute classes defined for the entity in the respective group.
	 */
	private void checkIfAllowed(long entityId, String groupPath, String attributeTypeId) 
			throws EngineException
	{
		AttributeClassHelper acHelper = acUtil.getACHelper(entityId, groupPath);
		if (!acHelper.isAllowed(attributeTypeId))
			throw new SchemaConsistencyException("The attribute with name " + attributeTypeId + 
					" is not allowed by the entity's attribute classes in the group " + groupPath);
	}

	/**
	 * Verifies if the attribute is allowed wrt attribute classes defined for the entity in the respective group.
	 */
	private void checkIfMandatory(long entityId, String groupPath, String attributeTypeId) 
			throws EngineException
	{
		AttributeClassHelper acHelper = acUtil.getACHelper(entityId, groupPath);
		if (acHelper.isMandatory(attributeTypeId))
			throw new SchemaConsistencyException("The attribute with name " + attributeTypeId + 
					" is required by the entity's attribute classes in the group " + groupPath);
	}
}
