/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_classes;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
class AttributeClassController
{
	private final MessageSource msg;
	private final AttributeClassManagement attrClassMan;
	private final AttributeTypeManagement attrTypeMan;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	AttributeClassController(MessageSource msg, AttributeClassManagement attrClassMan, AttributeTypeManagement attrTypeMan, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.attrClassMan = attrClassMan;
		this.attrTypeMan = attrTypeMan;
		this.notificationPresenter = notificationPresenter;
	}

	Collection<AttributesClass> getAttributeClasses()
	{
		try
		{
			return attrClassMan.getAttributeClasses().values();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeClassController.getAllError"), e.getMessage());
		}
		return List.of();
	}

	void removeAttributeClass(AttributesClass attrClass)
	{
		try
		{
			attrClassMan.removeAttributeClass(attrClass.getName());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeClassController.removeError", attrClass.getName()), e.getMessage());
		}

	}

	void addAttributeClass(AttributesClass attrClass)
	{
		try
		{
			attrClassMan.addAttributeClass(attrClass);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeClassController.addError", attrClass.getName()), e.getMessage());
		}

	}
	
	void updateAttributeClass(AttributesClass attrClass)
	{
		try
		{
			attrClassMan.updateAttributeClass(attrClass);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeClassController.updateError", attrClass.getName()), e.getMessage());
		}

	}

	AttributesClass getAttributeClass(String attributeClassName)
	{
		try
		{
			return attrClassMan.getAttributeClass(attributeClassName);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeClassController.getError", attributeClassName),
					e.getMessage());
		}
		return new AttributesClass();
	}

	List<String> getAllAttributeTypes()
	{
		try
		{
			return attrTypeMan.getAttributeTypes().stream()
					.map(AttributeType::getName)
					.sorted()
					.toList();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeClassController.createEditorError"), e.getMessage());
		}
		return List.of();
	}

	Map<String, AttributesClass> getAllAttributeClasses()
	{
		try
		{
			return attrClassMan.getAttributeClasses();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeClassController.createEditorError"), e.getMessage());
		}
		return Map.of();
	}
}
