/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Component showing all attributes that are going to be sent to the requesting service. User
 * can select attributes which should be hidden.
 * By default attributes are collapsed.
 * @author K. Benedyczak
 */
public class ExposedSelectableAttributesComponent extends CustomComponent implements SelectableAttributesComponent
{
	private MessageSource msg;
	private AttributeHandlerRegistryV8 handlersRegistry;
	
	private Map<String, DynamicAttribute> attributes;
	private Map<String, SelectableAttributeWithValues> attributesHiding;
	private Map<String, AttributeType> attributeTypes;
	private AttributeTypeSupport aTypeSupport;
	private Optional<IdentityParam> selectedIdentity;
	private IdentityPresentationUtil identityPresenter;
	

	public ExposedSelectableAttributesComponent(MessageSource msg,
			IdentityTypeSupport idTypeSupport, AttributeHandlerRegistryV8 handlersRegistry,
			Map<String, AttributeType> attributeTypes, AttributeTypeSupport aTypeSupport,
			Collection<DynamicAttribute> attributesCol,
			Optional<IdentityParam> selectedIdentity)
	{
		this.identityPresenter = new IdentityPresentationUtil(msg, idTypeSupport);
		this.handlersRegistry = handlersRegistry;
		this.msg = msg;
		this.attributeTypes = attributeTypes;
		this.aTypeSupport = aTypeSupport;
		this.selectedIdentity = selectedIdentity;

		attributes = new HashMap<>();
		for (DynamicAttribute a: attributesCol)
			attributes.put(a.getAttribute().getName(), a);
		initUI();
	}
	
	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	@Override
	public Collection<Attribute> getUserFilteredAttributes()
	{
		List<Attribute> ret = new ArrayList<>();
		for (Entry<String, SelectableAttributeWithValues> entry : attributesHiding.entrySet())
			if (!entry.getValue().isHidden())
				ret.add(entry.getValue().getWithoutHiddenValues());
		return ret;
	}

	/**
	 * @return collection of attributes with values hidden by the user.
	 */
	@Override
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
	
	@Override
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
		details.setMargin(false);
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(
				msg.getMessage("ExposedAttributesComponent.attributes"),
				true, details);
		showDetails.setId("ExposedSelectableAttributes.showDetails");
				
		contents.addComponent(showDetails);
		contents.addComponent(details);

		addIdentity(details);
		
		HtmlLabel attributesInfo = new HtmlLabel(msg,
					"ExposedAttributesComponent.attributesInfo");
		attributesInfo.addStyleName(Styles.vLabelSmall.toString());
		attributesInfo.setWidth(100, Unit.PERCENTAGE);
		details.addComponent(attributesInfo);
		
		details.addComponent(getAttributesListComponent());
		
		HtmlLabel credInfo = new HtmlLabel(msg);
		credInfo.setHtmlValue("ExposedAttributesComponent.credInfo");
		credInfo.addStyleName(Styles.vLabelSmall.toString());
		credInfo.setWidth(100, Unit.PERCENTAGE);
		details.addComponent(credInfo);

		setCompositionRoot(contents);
	}
	
	private void addIdentity(VerticalLayout container)
	{
		if (!selectedIdentity.isPresent())
			return;
		IdentityParam id = selectedIdentity.get();
		container.addComponent(getIdentityTF(id));
	}
	
	private Component getIdentityTF(IdentityParam identity)
	{
		Label identityField = new Label100(identityPresenter.getIdentityVisualValue(identity));
		identityField.setCaption(msg.getMessage("IdentitySelectorComponent.identity"));
		if (!identityField.getValue().equals(identity.getValue()))
		{
			identityField.setDescription(msg.getMessage(
					"IdentitySelectorComponent.fullValue", identity.getValue()));
		}
		return identityField;
	}

	
	private Component getAttributesListComponent()
	{
		VerticalLayout attributesList = new VerticalLayout();
		attributesList.setMargin(false);
		
		attributesHiding = new HashMap<>();
		for (DynamicAttribute dat: attributes.values())
		{
			SelectableAttributeWithValues attributeComponent = getAttributeComponent(dat, attributeTypes);
			attributesHiding.put(dat.getAttribute().getName(), attributeComponent);
			attributesList.addComponent(attributeComponent);
		}
		
		return attributesList;	
		
	}
	
	private SelectableAttributeWithValues getAttributeComponent(DynamicAttribute dat, 
			Map<String, AttributeType> attributeTypes)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		
		WebAttributeHandler handler;
		handler = handlersRegistry.getHandlerWithStringFallback(attributeType);
		
		SelectableAttributeWithValues attributeComponent = new SelectableAttributeWithValues(
				at, dat.getDisplayedName(),
				dat.getDescription(), !dat.isMandatory(),
				attributeType, handler, msg, aTypeSupport);
		attributeComponent.setWidth(100, Unit.PERCENTAGE);
		
		return attributeComponent;
	
	}
}
