/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.SelectableAttributeWithValues;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Component showing all attributes that are going to be sent to the requesting service. User
 * can select attributes which should be hidden.
 * By default attributes are collapsed.
 * @author K. Benedyczak
 */
public class ExposedSelectableAttributesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersRegistry;
	
	private Map<String, DynamicAttribute> attributes;
	private Map<String, SelectableAttributeWithValues> attributesHiding;
	private Map<String, AttributeType> attributeTypes;
	private AttributeTypeSupport aTypeSupport;
	private boolean enableEdit;
	

	public ExposedSelectableAttributesComponent(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			Map<String, AttributeType> attributeTypes, AttributeTypeSupport aTypeSupport,
			Collection<DynamicAttribute> attributesCol, boolean enableEdit)
	{
		super();
		this.handlersRegistry = handlersRegistry;
		this.msg = msg;
		this.attributeTypes = attributeTypes;
		this.aTypeSupport = aTypeSupport;

		attributes = new HashMap<>();
		for (DynamicAttribute a: attributesCol)
			attributes.put(a.getAttribute().getName(), a);
		this.enableEdit = enableEdit;
		initUI();
	}
	
	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	public Map<String, Attribute> getUserFilteredAttributes()
	{
		Map<String, Attribute> ret = new HashMap<>();
		for (Entry<String, SelectableAttributeWithValues> entry : attributesHiding.entrySet())
			if (!entry.getValue().isHidden())
				ret.put(entry.getKey(), entry.getValue().getWithoutHiddenValues());
		return ret;
	}

	/**
	 * @return collection of attributes with values hidden by the user.
	 */
	public Map<String, Attribute> getHiddenAttributes()
	{
		Map<String, Attribute> ret = new HashMap<>();
		for (Entry<String, SelectableAttributeWithValues> entry : attributesHiding.entrySet())
		{
			Attribute hiddenValues = entry.getValue().getHiddenValues();
			if (hiddenValues != null)
				ret.put(entry.getKey(), hiddenValues);
		}
		return ret;
	}
	
	public void setInitialState(Map<String, Attribute> savedState)
	{
		for (Entry<String, Attribute> entry : savedState.entrySet())
		{
			SelectableAttributeWithValues selectableAttributeWithValues = 
					attributesHiding.get(entry.getKey());
			if (selectableAttributeWithValues != null)
				selectableAttributeWithValues.setHiddenValues(entry.getValue());
		}
	}
	
	private void initUI()
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);

		final VerticalLayout details = new VerticalLayout();
		details.setSpacing(false);
		details.setMargin(false);
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(true, details);
		showDetails.setId("ExposedSelectableAttributes.showDetails");
		
		Label attributesL = new Label100(msg.getMessage("ExposedAttributesComponent.attributes"));
		attributesL.addStyleName(Styles.bold.toString());
		
		HtmlLabel credInfo = new HtmlLabel(msg);
		credInfo.setHtmlValue("ExposedAttributesComponent.credInfo");
		credInfo.addStyleName(Styles.vLabelSmall.toString());
		credInfo.setWidth(100, Unit.PERCENTAGE);
		
		contents.addComponent(attributesL);
		contents.addComponent(showDetails);
		contents.addComponent(details);
		
		details.addComponent(credInfo);
		if (enableEdit)
		{
			HtmlLabel attributesInfo = new HtmlLabel(msg,
					"ExposedAttributesComponent.attributesInfo");
			attributesInfo.addStyleName(Styles.vLabelSmall.toString());
			attributesInfo.setWidth(100, Unit.PERCENTAGE);
			details.addComponent(attributesInfo);
		}
		details.addComponent(getAttributesListComponent());
		setCompositionRoot(contents);
	}
	
	public Component getAttributesListComponent()
	{
		VerticalLayout attributesList = new VerticalLayout();
		attributesList.setSpacing(false);
		attributesList.setMargin(false);
		Label hideL = new Label(msg.getMessage("ExposedAttributesComponent.hide"));
		
		attributesHiding = new HashMap<>();
		boolean first = true;
		for (DynamicAttribute dat: attributes.values())
		{
			SelectableAttributeWithValues attributeComponent = 
					getAttributeComponent(dat, attributeTypes, hideL);
			if (first)
			{
				first = false;
				hideL = null;
			}
			
			attributesHiding.put(dat.getAttribute().getName(), attributeComponent);
			attributesList.addComponent(attributeComponent);
		}
		
		return attributesList;	
		
	}
	
	public SelectableAttributeWithValues getAttributeComponent(DynamicAttribute dat, 
			Map<String, AttributeType> attributeTypes, Label hideL)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		
		WebAttributeHandler handler;
		handler = handlersRegistry.getHandlerWithStringFallback(attributeType);
		
		SelectableAttributeWithValues attributeComponent = new SelectableAttributeWithValues(
				null, enableEdit ? hideL : null, at, dat.getDisplayedName(),
				dat.getDescription(), !dat.isMandatory() && enableEdit,
				attributeType, handler, msg, aTypeSupport);
		attributeComponent.setWidth(100, Unit.PERCENTAGE);
		
		return attributeComponent;
	
	}
}
