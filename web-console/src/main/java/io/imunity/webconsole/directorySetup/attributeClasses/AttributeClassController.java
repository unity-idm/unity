/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeClasses;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all attribute class views
 * 
 * @author P.Piernik
 *
 */
@Component
class AttributeClassController
{
	private MessageSource msg;
	private AttributeClassManagement attrClassMan;
	private AttributeTypeManagement attrTypeMan;

	@Autowired
	AttributeClassController(MessageSource msg, AttributeClassManagement attrClassMan,
			AttributeTypeManagement attrTypeMan)
	{
		this.msg = msg;
		this.attrClassMan = attrClassMan;
		this.attrTypeMan = attrTypeMan;
	}

	Collection<AttributesClass> getAttributeClasses() throws ControllerException
	{
		try
		{
			return attrClassMan.getAttributeClasses().values();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeClassController.getAllError"), e);
		}
	}

	void removeAttributeClass(AttributesClass attrClass) throws ControllerException
	{
		try
		{
			attrClassMan.removeAttributeClass(attrClass.getName());
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeClassController.removeError", attrClass.getName()), e);
		}

	}

	void addAttributeClass(AttributesClass attrClass) throws ControllerException
	{
		try
		{
			attrClassMan.addAttributeClass(attrClass);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeClassController.addError", attrClass.getName()), e);
		}

	}
	
	void updateAttributeClass(AttributesClass attrClass) throws ControllerException
	{
		try
		{
			attrClassMan.updateAttributeClass(attrClass);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeClassController.updateError", attrClass.getName()), e);
		}

	}

	AttributesClass getAttributeClass(String attributeClassName) throws ControllerException
	{
		try
		{
			return attrClassMan.getAttributeClass(attributeClassName);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeClassController.getError", attributeClassName), e);
		}
	}

	AttributesClassEditor getEditor(AttributesClass toEdit) throws ControllerException
	{
		try
		{
			AttributesClassEditor editor = new AttributesClassEditor(msg,
					attrClassMan.getAttributeClasses(), attrTypeMan.getAttributeTypes());
			if (toEdit != null)
			{
				editor.setEditedClass(toEdit);
			}
			return editor;

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeClassController.createEditorError"), e);
		}
	}

}
