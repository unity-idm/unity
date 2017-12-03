/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Shows a single {@link SPSettings}.
 * 
 * @author K. Benedyczak
 */
public class SamlSPSettingsViewer extends FormLayout
{
	protected UnityMessageSource msg;
	protected Label autoConfirm;
	protected ListSelect<String> hiddenAttributes;
	protected Label defaultIdentity;
	protected AttributeHandlerRegistry attrHandlerRegistry;
	
	public SamlSPSettingsViewer(UnityMessageSource msg, AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerRegistry;
		
		setSpacing(true);
		setMargin(true);
		autoConfirm = new Label();
		autoConfirm.setCaption(msg.getMessage("SAMLPreferences.autoConfirm"));
		defaultIdentity = new Label();
		defaultIdentity.setCaption(msg.getMessage("SAMLPreferences.defaultIdentity"));
		hiddenAttributes = new ListSelect<>(msg.getMessage("SAMLPreferences.hiddenAttributes"));
		hiddenAttributes.setWidth(90, Unit.PERCENTAGE);
		hiddenAttributes.setRows(6);
		
		addComponents(autoConfirm, defaultIdentity, hiddenAttributes);
	}
	
	public void setInput(SPSettings spSettings)
	{
		if (spSettings == null)
		{
			setVisibleRec(false);
			return;
		}
		setVisibleRec(true);
		
		if (spSettings.isDoNotAsk())
		{
			if (spSettings.isDefaultAccept())
				autoConfirm.setValue(msg.getMessage("SAMLPreferences.accept"));
			else
				autoConfirm.setValue(msg.getMessage("SAMLPreferences.decline"));
		} else
			autoConfirm.setValue(msg.getMessage("no"));
		
		hiddenAttributes.setReadOnly(false);
		Map<String, Attribute> attributes = spSettings.getHiddenAttribtues();
		hiddenAttributes.setVisible(!attributes.isEmpty());
		
		List<String> hiddenValues  = new ArrayList<>(); 
		for (Entry<String, Attribute> entry : attributes.entrySet())
		{
			if (entry.getValue() == null)
				hiddenValues.add(entry.getKey());
			else
			{
				String simplifiedAttributeRepresentation = attrHandlerRegistry.
						getSimplifiedAttributeRepresentation(entry.getValue());
				hiddenValues.add(simplifiedAttributeRepresentation);
			}
		}
		hiddenAttributes.setItems(hiddenValues);
		
		hiddenAttributes.setReadOnly(true);
		
		String selIdentity = spSettings.getSelectedIdentity();
		if (selIdentity != null)
		{
			defaultIdentity.setValue(selIdentity);
			defaultIdentity.setVisible(true);
		} else
			defaultIdentity.setVisible(false);
	}
	
	private void setVisibleRec(boolean how)
	{
		Iterator<Component> children = iterator();
		while (children.hasNext())
		{
			Component c = children.next();
			c.setVisible(how);
		}
	}
}
