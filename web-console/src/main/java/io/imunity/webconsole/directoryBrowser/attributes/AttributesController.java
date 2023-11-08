/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser.attributes;

import io.imunity.webconsole.attribute.AttributeChangedEvent;
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
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.*;

/**
 * 
 * @author P.Piernik
 *
 */
@Component("AttributesControllerV8")
class AttributesController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributesController.class);

	private AttributeClassManagement acMan;
	private AttributeTypeManagement aTypeManagement;
	private GroupsManagement groupsManagement;
	private MessageSource msg;
	private AttributesManagement attrMan;

	private AttributeHandlerRegistryV8 registry;
	private AttributeTypeSupport atSupport;

	@Autowired
	AttributesController(AttributesManagement attrMan, AttributeClassManagement acMan,
	                     AttributeTypeManagement aTypeManagement, GroupsManagement groupsManagement,
	                     AttributeHandlerRegistryV8 registry, AttributeTypeSupport atSupport, MessageSource msg)
	{
		this.attrMan = attrMan;
		this.acMan = acMan;
		this.aTypeManagement = aTypeManagement;
		this.groupsManagement = groupsManagement;
		this.registry = registry;
		this.atSupport = atSupport;

		this.msg = msg;
	}

	Collection<AttributeExt> getAttributes(EntityWithLabel owner, String groupPath) throws ControllerException
	{
		try
		{
			EntityParam entParam = new EntityParam(owner.getEntity().getId());
			return attrMan.getAllAttributes(entParam, true, groupPath, null, true);
		} catch (AuthorizationException e)
		{
			throw new ControllerException(msg.getMessage("Attribute.noReadAuthz", groupPath, owner), e);
		} catch (Exception e)
		{
			log.fatal("Problem retrieving attributes in the group " + groupPath + " for " + owner, e);
			throw new ControllerException(msg.getMessage("Attribute.internalError", groupPath), e);
		}
	}

	AttributeClassHelper getAttributeClassHelper(EntityParam owner, String groupPath) throws ControllerException
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
			throw new ControllerException("", e);
		}

	}

	void removeAttribute(EntityParam owner, Collection<AttributeExt> items, EventsBus bus)
			throws ControllerException
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (AttributeExt toRemove : items)
			{
				attrMan.removeAttribute(owner, toRemove.getGroupPath(), toRemove.getName());
				bus.fireEvent(new AttributeChangedEvent(toRemove.getGroupPath(), toRemove.getName()));
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				throw new ControllerException(msg.getMessage("AttributesController.removeError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("AttributesController.removeError"),
						msg.getMessage("AttributesController.partiallyRemoved", removed), e);
			}
		}
	}

	void updateAttribute(EntityParam owner, Attribute attribute, EventsBus bus) throws ControllerException
	{
		try
		{
			attrMan.setAttributeSuppressingConfirmation(owner, attribute);
			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(), attribute.getName()));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributesController.updateError", attribute.getName()), e);
		}
	}

	void addAttribute(EntityParam owner, Attribute attribute, EventsBus bus) throws ControllerException
	{
		try
		{
			attrMan.createAttributeSuppressingConfirmation(owner, attribute);

			bus.fireEvent(new AttributeChangedEvent(attribute.getGroupPath(), attribute.getName()));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributesController.addError", attribute.getName()), e);
		}
	}

	Map<String, AttributeType> getAttributeTypes() throws ControllerException
	{
		try
		{
			return aTypeManagement.getAttributeTypesAsMap();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributesController.getAttributeTypesError"), e);
		}
	}

	public com.vaadin.ui.Component getDetailsComponent(AttributeExt a) throws ControllerException

	{
		try
		{
			AttributeValueSyntax<?> syntax = atSupport.getSyntax(a);
			WebAttributeHandler handler = registry.getHandler(syntax);
			return new AttributeDetailsComponent(msg, syntax, handler, a);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributesController.createDetailsComponentError"), e);
		}
	}

	public com.vaadin.ui.Component getShortAttrValuesRepresentation(AttributeExt a) throws ControllerException
	{
		try
		{
			return registry.getSimplifiedShortValuesRepresentation(a);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributesController.createDetailsComponentError"), e);
		}
	}

}
