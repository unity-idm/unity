/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class AttributesManagementImpl implements AttributesManagement
{
	private AttributeClassUtil acUtil;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeDAO dbAttributes;
	private EntityResolver idResolver;
	private AuthorizationManager authz;
	private AttributesHelper attributesHelper;
	private EmailConfirmationManager confirmationManager;
	private TransactionalRunner txRunner;

	@Autowired
	public AttributesManagementImpl(AttributeClassUtil acUtil,
			AttributeTypeDAO attributeTypeDAO, AttributeDAO dbAttributes,
			EntityResolver idResolver, AuthorizationManager authz,
			AttributesHelper attributesHelper, EmailConfirmationManager confirmationManager,
			TransactionalRunner txRunner)
	{
		this.acUtil = acUtil;
		this.attributeTypeDAO = attributeTypeDAO;
		this.dbAttributes = dbAttributes;
		this.idResolver = idResolver;
		this.authz = authz;
		this.attributesHelper = attributesHelper;
		this.confirmationManager = confirmationManager;
		this.txRunner = txRunner;
	}

	@Override
	public void setAttribute(EntityParam entity, Attribute attribute, boolean update)
			throws EngineException
	{
		entity.validateInitialization(); 
		txRunner.runInTransactionThrowing(() -> {
			boolean fullAuthz;
			//Important - attributes can be also set as a result of addMember and addEntity.
			//  when changing this method, verify if those needs an update too.
			long entityId = idResolver.getEntityId(entity);
			AttributeType at = attributeTypeDAO.get(attribute.getName());
			fullAuthz = checkSetAttributeAuthz(entityId, at, attribute);
			checkIfAllowed(entityId, attribute.getGroupPath(), attribute.getName());

			attributesHelper.addAttribute(entityId, attribute, at, update, fullAuthz);
		});
		confirmationManager.sendVerificationQuietNoTx(entity, attribute, false);
	}
	
	private boolean checkSetAttributeAuthz(long entityId, AttributeType at, Attribute attribute) 
			throws AuthorizationException
	{
		Set<AuthzCapability> nonSelfCapabilities = authz.getCapabilities(false, 
				attribute.getGroupPath());
		boolean fullAuthz = nonSelfCapabilities.contains(AuthzCapability.attributeModify);

		//even if we have fullAuthz we need to check authZ (e.g. to get outdated credential error)
		authz.checkAuthorization(at.isSelfModificable() && authz.isSelf(entityId), 
				attribute.getGroupPath(), AuthzCapability.attributeModify);
		return fullAuthz;
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
		
		checkIfMandatory(entityId, groupPath, attributeTypeId);
		
		dbAttributes.deleteAttribute(attributeTypeId, entityId, groupPath);
	}

	@Override
	@Transactional
	public Collection<AttributeExt> getAttributes(EntityParam entity, String groupPath,
			String attributeTypeId) throws EngineException
	{
		Collection<AttributeExt> ret = getAllAttributesInternal(entity, true, groupPath, attributeTypeId, 
				new AuthzCapability[] {AuthzCapability.read}, false);
		return ret;
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
				Collection<AttributeExt> ret = getAllAttributesInternal(entity, effective, 
						groupPath, attributeTypeId, 
						new AuthzCapability[] {AuthzCapability.read}, false);
				return ret;
			} else
				throw e;
		}
	}

	private Collection<AttributeExt> getAllAttributesInternal(EntityParam entity, boolean effective, 
			String groupPath,
			String attributeTypeName, AuthzCapability[] requiredCapability, boolean allowDisabled) 
					throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), groupPath, requiredCapability);
		Collection<AttributeExt> ret = attributesHelper.getAllAttributesInternal(entityId, 
				effective, groupPath, attributeTypeName, allowDisabled);
		return ret;
	}
	
	/**
	 * Verifies if the attribute is allowed wrt attribute classes defined for the entity in the respective group.
	 * @param entityId
	 * @param attribute
	 * @throws EngineException
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
	 * @param entityId
	 * @param attribute
	 * @throws EngineException
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
