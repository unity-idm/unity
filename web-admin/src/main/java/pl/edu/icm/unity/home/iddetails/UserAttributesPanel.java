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
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewer;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;

import com.vaadin.ui.AbstractOrderedLayout;

/**
 * Shows (optionally in edit mode) all configured attributes.
 * 
 * @author K. Benedyczak
 */
public class UserAttributesPanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UserAttributesPanel.class);
	private UnityMessageSource msg;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attributesMan;
	private HomeEndpointProperties config;
	private long entityId;
	
	private List<FixedAttributeEditor> attributeEditors;

	private AbstractOrderedLayout parent;
	private List<AttributeViewer> viewers;
	
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
	}

	public void addIntoLayout(AbstractOrderedLayout layout) throws EngineException
	{
		this.parent = layout;
		initUI();
	}
	
	private void initUI() throws EngineException
	{
		attributeEditors = new ArrayList<FixedAttributeEditor>();
		viewers = new ArrayList<>();
		Set<String> keys = config.getStructuredListKeys(HomeEndpointProperties.ATTRIBUTES);
		Map<String, AttributeType> atTypes = attributesMan.getAttributeTypesAsMap();

		for (String aKey: keys)
		{
			addAttribute(atTypes, aKey);
		}
	}
	
	private void addAttribute(Map<String, AttributeType> atTypes, String key)
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
				null, null, false, false, parent);
			if (attribute != null)
				editor.setAttributeValues(attribute.getValues());
			attributeEditors.add(editor);
		} else
		{
			if (attribute == null)
				return;
			
			AttributeViewer viewer = new AttributeViewer(msg, attributeHandlerRegistry, at, 
					attribute, showGroup);
			viewers.add(viewer);
			viewer.addToLayout(parent);
		}
	}
	
	public void clear()
	{
		for (AttributeViewer viewer: viewers)
			viewer.removeFromLayout(parent);
		for (FixedAttributeEditor editor: attributeEditors)
			editor.clear();
	}
	
	public void refreshEditable() throws EngineException
	{
		initUI();
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
	
	public void validate() throws FormValidationException
	{
		for (FixedAttributeEditor ae: attributeEditors)
			ae.getAttribute();
	}
	
	public void saveChanges() throws EngineException
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
	
	private void updateAttribute(Attribute<?> a) throws EngineException
	{
		attributesMan.setAttribute(new EntityParam(entityId), a, true);
	}
	
	private void removeAttribute(FixedAttributeEditor ae) throws EngineException
	{
		try
		{
			attributesMan.removeAttribute(new EntityParam(entityId), 
					ae.getGroup(), ae.getAttributeType().getName());
		} catch (IllegalAttributeValueException e)
		{
			//OK - attribute already doesn't exist
		}
	}

	public boolean hasEditable()
	{
		return attributeEditors.size() > 0;
	}
}
