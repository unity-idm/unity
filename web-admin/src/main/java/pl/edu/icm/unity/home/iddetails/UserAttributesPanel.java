/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

/**
 * Shows (optionally in edit mode) all configured attributes.
 * 
 * @author K. Benedyczak
 */
public class UserAttributesPanel extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UserAttributesPanel.class);
	private UnityMessageSource msg;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attributesMan;
	private HomeEndpointProperties config;
	private long entityId;
	
	private List<FixedAttributeEditor> attributeEditors;
	
	public UserAttributesPanel(UnityMessageSource msg,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attributesMan, HomeEndpointProperties config,
			long entityId) throws EngineException
	{
		this.msg = msg;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attributesMan = attributesMan;
		this.config = config;
		this.entityId = entityId;
		
		initUI();
	}

	private void initUI() throws EngineException
	{
		attributeEditors = new ArrayList<FixedAttributeEditor>();
		Set<String> keys = config.getStructuredListKeys(HomeEndpointProperties.ATTRIBUTES);
		Map<String, AttributeType> atTypes = attributesMan.getAttributeTypesAsMap();

		VerticalLayout root = new VerticalLayout();
		root.setSpacing(true);
		
		FormLayout attributeFL = new FormLayout();
		for (String aKey: keys)
		{
			addAttribute(attributeFL, atTypes, aKey);
		}
		
		root.addComponent(attributeFL);
		
		if (attributeEditors.size() > 0)
		{
			Button save = new Button(msg.getMessage("save"));
			save.setIcon(Images.save.getResource());
			save.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					saveChanges();
				}
			});
			root.addComponent(save);
		}
		setCompositionRoot(root);
	}
	
	private void addAttribute(AbstractOrderedLayout layout, Map<String, AttributeType> atTypes, String key)
	{		
		String group = config.getValue(key+HomeEndpointProperties.GWA_GROUP);
		String attributeName = config.getValue(key+HomeEndpointProperties.GWA_ATTRIBUTE);
		boolean showGroup = config.getBooleanValue(key+HomeEndpointProperties.GWA_SHOW_GROUP);
		boolean editable = config.getBooleanValue(key+HomeEndpointProperties.GWA_EDITABLE);
		
		AttributeType at = atTypes.get(attributeName);
		AttributeExt<?> attribute = getAttribute(attributeName, group);

		
		if (editable && at.isSelfModificable())
		{
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
				at, showGroup, group, AttributeVisibility.full, 
				null, null, false, layout);
			if (attribute != null)
				editor.setAttributeValues(attribute.getValues());
			attributeEditors.add(editor);
		} else
		{
			if (attribute == null)
				return;
			String aString = attributeHandlerRegistry.getSimplifiedAttributeRepresentation(attribute, 120);
			layout.addComponent(new Label(aString));
		}
	}
	
	private AttributeExt<?> getAttribute(String attributeName, String group)
	{
		Collection<AttributeExt<?>> attributes;
		try
		{
			attributes = attributesMan.getAttributes(
					new EntityParam(entityId), group, attributeName);
		} catch (EngineException e)
		{
			log.debug("Can not resolve attribute " + attributeName + " for entity", e);
			return null;
		}
		if (attributes.isEmpty())
			return null;
		return attributes.iterator().next();
	}
	
	private void saveChanges()
	{
		for (FixedAttributeEditor ae: attributeEditors)
		{
			try
			{
				Attribute<?> a = ae.getAttribute();
				if (a != null)
					updateAttribute(a);
				else
					removeAttribute(ae);
			} catch (FormValidationException e)
			{
				continue;
			}
		}
	}
	
	private void updateAttribute(Attribute<?> a)
	{
		try
		{
			attributesMan.setAttribute(new EntityParam(entityId), a, true);
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("UserAttributesPanel.errorSaving",
					a.getName()), e);
		}
		
	}
	
	private void removeAttribute(FixedAttributeEditor ae)
	{
		try
		{
			attributesMan.removeAttribute(new EntityParam(entityId), 
					ae.getGroup(), ae.getAttributeType().getName());
		} catch (IllegalAttributeValueException e)
		{
			//OK - attribute already doesn't exist
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("UserAttributesPanel.errorSaving",
					ae.getAttributeType().getName()), e);
		}
	}
}
