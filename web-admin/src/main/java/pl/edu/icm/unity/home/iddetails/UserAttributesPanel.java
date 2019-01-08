/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler.AuthnResult;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewer;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext.ConfirmationMode;
import pl.edu.icm.unity.webui.common.attributes.edit.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter.ComposableComponents;
import pl.edu.icm.unity.webui.common.composite.GroupOfGroups;

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

	private List<AttributeViewer> viewers;
	private EntityManagement idsMan;
	private AttributeSupport atMan;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private GroupOfGroups componentsGroup;
	
	public UserAttributesPanel(
			AdditionalAuthnHandler additionalAuthnHandler,
			UnityMessageSource msg,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attributesMan, EntityManagement idsMan,
			AttributeSupport atMan,
			HomeEndpointProperties config,
			long entityId) throws EngineException
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attributesMan = attributesMan;
		this.idsMan = idsMan;
		this.atMan = atMan;
		this.config = config;
		this.entityId = entityId;
	}

	public ComposableComponents getContents() throws EngineException
	{
		componentsGroup = new GroupOfGroups();
		initUI();
		return componentsGroup;
	}
	
	private void initUI() throws EngineException
	{
		attributeEditors = new ArrayList<>();
		viewers = new ArrayList<>();
		Set<String> keys = config.getStructuredListKeys(HomeEndpointProperties.ATTRIBUTES);
		
		Map<String, AttributeType> atTypes = atMan.getAttributeTypesAsMap();
		Set<String> groups = idsMan.getGroupsForPresentation(new EntityParam(entityId)).
				stream().map(g -> g.toString()).collect(Collectors.toSet());
		for (String aKey: keys)
			addAttribute(atTypes, aKey, groups, componentsGroup);
	}
	
	private void addAttribute(Map<String, AttributeType> atTypes, String key, Set<String> groups, 
			GroupOfGroups componentsGroup)
	{		
		String group = config.getValue(key+HomeEndpointProperties.GWA_GROUP);
		String attributeName = config.getValue(key+HomeEndpointProperties.GWA_ATTRIBUTE);
		boolean showGroup = config.getBooleanValue(key+HomeEndpointProperties.GWA_SHOW_GROUP);
		boolean editable = config.getBooleanValue(key+HomeEndpointProperties.GWA_EDITABLE);
		AttributeType at = atTypes.get(attributeName);
		if (at == null)
		{
			log.warn("No attribute type " + attributeName + " defined in the system.");
			return;
		}
		AttributeExt attribute = getAttribute(attributeName, group);

		if (!groups.contains(group))
			return;
		if (editable && at.isSelfModificable())
		{
			
			AttributeEditContext editContext = AttributeEditContext.builder()
					.withConfirmationMode(ConfirmationMode.USER)
					.withAttributeType(at)
					.withAttributeGroup(group)
					.withAttributeOwner(new EntityParam(entityId)).build();
			
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
				editContext, showGroup, null, null);
			if (attribute != null)
				editor.setAttributeValues(attribute.getValues());
			attributeEditors.add(editor);
			componentsGroup.addComposableComponents(editor.getComponentsGroup());
		} else
		{
			if (attribute == null)
				return;
			
			AttributeViewer viewer = new AttributeViewer(msg, attributeHandlerRegistry, at, 
					attribute, showGroup, AttributeViewerContext.EMPTY);
			viewers.add(viewer);
			componentsGroup.addComposableComponents(viewer.getComponentsGroup());
		}
	}
	
	private void clear()
	{
		for (AttributeViewer viewer: viewers)
			viewer.clear();
		for (FixedAttributeEditor editor: attributeEditors)
			editor.clear();
	}
	
	public void refresh() throws EngineException
	{
		clear();
		initUI();
	}
	
	private AttributeExt getAttribute(String attributeName, String group)
	{
		Collection<AttributeExt> attributes;
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
	
	public boolean saveChanges() throws Exception
	{
		boolean changed = false;
		for (FixedAttributeEditor ae: attributeEditors)
		{
			try
			{
				if (!ae.isChanged())
					continue;
				Optional<Attribute> a = ae.getAttribute();
				if (a.isPresent())
					updateAttribute(a.get());
				else
					removeAttribute(ae);
				changed = true;
			} catch (FormValidationException e)
			{
				continue;
			} catch (AdditionalAuthenticationRequiredException additionalAuthn)
			{
				additionalAuthnHandler.handleAdditionalAuthenticationException(additionalAuthn, 
						msg.getMessage("UserAttributesPanel.additionalAuthnRequired"), 
						msg.getMessage("UserAttributesPanel.additionalAuthnRequiredInfo"),
						this::onAdditionalAuthnForAttributesSave);
				return false;
			} catch (AdditionalAuthenticationMisconfiguredException misconfigured)
			{
				NotificationPopup.showError(msg.getMessage("UserAttributesPanel.attributeUpdateError"), 
						msg.getMessage("AdditionalAuthenticationMisconfiguredError"));
				return changed;
			}
		}
		return changed;
	}
	
	private void onAdditionalAuthnForAttributesSave(AuthnResult result)
	{
		try
		{
			if (result == AuthnResult.SUCCESS)
			{
				saveChanges();
				refresh();
			} else if (result == AuthnResult.ERROR)
			{
				NotificationPopup.showError(msg.getMessage("UserAttributesPanel.attributeUpdateError"), 
						msg.getMessage("UserAttributesPanel.additionalAuthnFailed"));
				refresh();
			}
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("UserAttributesPanel.attributeUpdateError"), e);
		}
	}
	
	private void updateAttribute(Attribute a) throws EngineException
	{
		attributesMan.setAttribute(new EntityParam(entityId), a);
	}
	
	private void removeAttribute(FixedAttributeEditor ae) throws EngineException
	{
		try
		{
			attributesMan.removeAttribute(new EntityParam(entityId), 
					ae.getGroup(), ae.getAttributeType().getName());
		} catch (IllegalArgumentException e)
		{
			//OK - attribute already doesn't exist
		}
	}

	public boolean hasEditable()
	{
		return attributeEditors.size() > 0;
	}
}
