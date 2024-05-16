/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.attributes;

import com.vaadin.flow.component.html.Div;
import io.imunity.console.attribute.AttributeChangedEvent;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

import java.util.*;


@Component
class AttributesController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributesController.class);

	private final AttributeClassManagement acMan;
	private final AttributeTypeManagement aTypeManagement;
	private final GroupsManagement groupsManagement;
	private final MessageSource msg;
	private final AttributesManagement attrMan;

	private final AttributeHandlerRegistry registry;
	private final AttributeTypeSupport atSupport;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	AttributesController(AttributesManagement attrMan, AttributeClassManagement acMan,
	        AttributeTypeManagement aTypeManagement, GroupsManagement groupsManagement,
			AttributeHandlerRegistry registry, AttributeTypeSupport atSupport, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.attrMan = attrMan;
		this.acMan = acMan;
		this.aTypeManagement = aTypeManagement;
		this.groupsManagement = groupsManagement;
		this.registry = registry;
		this.atSupport = atSupport;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	Collection<AttributeExt> getAttributes(EntityWithLabel owner, String groupPath)
	{
		try
		{
			EntityParam entParam = new EntityParam(owner.getEntity().getId());
			return attrMan.getAllAttributes(entParam, true, groupPath, null, true);
		} catch (AuthorizationException e)
		{
			notificationPresenter.showError(msg.getMessage("Attribute.noReadAuthz", groupPath, owner), e.getMessage());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Attribute.internalError", groupPath, owner), e.getMessage());
			log.fatal("Problem retrieving attributes in the group " + groupPath + " for " + owner, e);
		}
		return List.of();
	}

	AttributeClassHelper getAttributeClassHelper(EntityParam owner, String groupPath)
	{
		Group group;
		try
		{
			group = groupsManagement.getContents(groupPath, GroupContents.METADATA).getGroup();
			Collection<AttributesClass> acs = acMan.getEntityAttributeClasses(owner, groupPath);
			Map<String, AttributesClass> knownClasses = acMan.getAttributeClasses();
			Set<String> assignedClasses = new HashSet<String>(acs.size());
			for (AttributesClass ac : acs)
				assignedClasses.add(ac.getName());
			assignedClasses.addAll(group.getAttributesClasses());
			return new AttributeClassHelper(knownClasses, assignedClasses);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
		return new AttributeClassHelper();
	}

	void removeAttribute(EntityParam owner, Collection<AttributeExt> items, EventsBus bus)
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (AttributeExt toRemove : items)
			{
				attrMan.removeAttribute(owner, toRemove.getGroupPath(), toRemove.getName());
				bus.fireEvent(new AttributeChangedEvent(toRemove.getGroupPath(), toRemove.getName()));
				removed.add(toRemove.getName());
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				notificationPresenter.showError(msg.getMessage("Attribute.removeAttributeError"), e.getMessage());
			} else
			{
				notificationPresenter.showError(msg.getMessage("Attribute.removeAttributeError",
						msg.getMessage("AttributesController.partiallyRemoved", removed)), e.getMessage());
			}
		}
	}

	void updateAttribute(EntityParam owner, Attribute attribute, EventsBus bus)
	{
		try
		{
			attrMan.setAttributeSuppressingConfirmation(owner, attribute);
			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(), attribute.getName()));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Attribute.updateError", attribute.getName()), e.getMessage());
		}
	}

	void addAttribute(EntityParam owner, Attribute attribute, EventsBus bus)
	{
		try
		{
			attrMan.createAttributeSuppressingConfirmation(owner, attribute);

			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(), attribute.getName()));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Attribute.addError", attribute.getName()), e.getMessage());
		}
	}

	Map<String, AttributeType> getAttributeTypes()
	{
		try
		{
			return aTypeManagement.getAttributeTypesAsMap();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Attribute.getAttributeTypesError"), e.getMessage());
		}
		return Map.of();
	}

	public com.vaadin.flow.component.Component getDetailsComponent(AttributeExt a)
	{
		try
		{
			AttributeValueSyntax<?> syntax = atSupport.getSyntax(a);
			WebAttributeHandler handler = registry.getHandler(syntax);
			return new AttributeDetailsComponent(msg, handler, a);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Attribute.createDetailsComponentError"), e.getMessage());
		}
		return new Div();
	}

	public com.vaadin.flow.component.Component getShortAttrValuesRepresentation(AttributeExt a)
	{
		try
		{
			return registry.getSimplifiedShortValuesRepresentation(a);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Attribute.createDetailsComponentError"), e.getMessage());
		}
		return new Div();
	}

}
