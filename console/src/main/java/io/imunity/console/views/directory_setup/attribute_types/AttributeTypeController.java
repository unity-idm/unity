/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_types;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.AttributeMetadataHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for all attribute type views
 * 
 * @author P.Piernik
 *
 */
@Component
class AttributeTypeController
{
	private final MessageSource msg;
	private final AttributeTypeManagement attrTypeMan;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final AttributeMetadataHandlerRegistry attrMetaHandlerRegistry;
	private final AttributeTypeSupport atSupport;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	AttributeTypeController(MessageSource msg, AttributeTypeManagement attrTypeMan,
			AttributeHandlerRegistry attrHandlerRegistry, AttributeMetadataHandlerRegistry attrMetaHandlerRegistry,
			AttributeTypeSupport atSupport, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.attrTypeMan = attrTypeMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.attrMetaHandlerRegistry = attrMetaHandlerRegistry;
		this.atSupport = atSupport;
		this.notificationPresenter = notificationPresenter;
	}

	Collection<AttributeTypeEntry> getAttributeTypes() throws ControllerException
	{
		try
		{
			return attrTypeMan.getAttributeTypes()
					.stream()
					.map(at -> new AttributeTypeEntry(msg, at))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.getAllError"), e);
		}
	}

	void addAttributeType(AttributeType at) throws ControllerException
	{
		try
		{
			attrTypeMan.addAttributeType(at);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.addError", at.getName()), e);
		}

	}

	void updateAttributeType(AttributeType at) throws ControllerException
	{
		try
		{
			attrTypeMan.updateAttributeType(at);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.updateError", at.getName()), e);
		}

	}

	AttributeType getAttributeType(String attributeTypeName) throws ControllerException
	{
		try
		{
			return attrTypeMan.getAttributeType(attributeTypeName);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.getError", attributeTypeName), e);
		}
	}

	void removeAttributeTypes(Set<AttributeType> items, boolean deleteInstances) throws ControllerException
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (AttributeType toRemove : items)
			{
				attrTypeMan.removeAttributeType(toRemove.getName(), deleteInstances);
				removed.add(toRemove.getName());
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				throw new ControllerException(msg.getMessage("AttributeTypeController.removeError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("AttributeTypeController.removeError"),
						msg.getMessage("AttributeTypeController.partiallyRemoved", removed), e);
			}
		}

	}

	void mergeAttributeTypes(Set<AttributeType> toMerge, boolean overwrite) throws ControllerException
	{
		try
		{
			Set<String> existing = attrTypeMan.getAttributeTypesAsMap()
					.keySet();
			for (AttributeType at : toMerge)
			{

				if (!existing.contains(at.getName()))
				{
					attrTypeMan.addAttributeType(at);
				} else if (overwrite)
				{
					attrTypeMan.updateAttributeType(at);
				}
			}
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.mergeAttributeTypesError"), e);
		}
	}

	ImportAttributeTypeEditor getImportEditor() throws ControllerException
	{
		try
		{
			return new ImportAttributeTypeEditor(msg, getAttributeTypes().stream()
					.map(a -> a.attributeType)
					.collect(Collectors.toSet()), atSupport, notificationPresenter);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.getImportEditorError"), e);
		}
	}

	AttributeTypeEditor getEditor(AttributeType attributeType)
	{

		return attributeType == null
				? new RegularAttributeTypeEditor(msg, attrHandlerRegistry, attributeType, attrMetaHandlerRegistry,
						atSupport)
				: attributeType.isTypeImmutable() ? new ImmutableAttributeTypeEditor(msg, attributeType)
						: new RegularAttributeTypeEditor(msg, attrHandlerRegistry, attributeType,
								attrMetaHandlerRegistry, atSupport);
	}

	RegularAttributeTypeEditor getRegularAttributeTypeEditor(AttributeType attributeType)
	{

		return new RegularAttributeTypeEditor(msg, attrHandlerRegistry, attributeType, attrMetaHandlerRegistry,
				atSupport);

	}
}
