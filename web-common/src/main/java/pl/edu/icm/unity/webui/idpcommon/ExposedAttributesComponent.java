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
import java.util.Optional;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewer;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Component showing all attributes that are going to be sent to the requesting
 * service. By default attributes are collapsed.
 * 
 * @author K. Benedyczak
 */
public class ExposedAttributesComponent extends CustomComponent
{
	private final MessageSource msg;
	private final IdentityPresentationUtil identityPresenter;
	
	protected Map<String, DynamicAttribute> attributes;
	protected ListOfSelectableElements attributesHiding;
	private AttributeHandlerRegistry handlersRegistry;
	private Optional<IdentityParam> selectedIdentity;

	public ExposedAttributesComponent(MessageSource msg,
			IdentityTypeSupport idTypeSupport,
			AttributeHandlerRegistry handlersRegistry,
			Collection<DynamicAttribute> attributesCol,
			Optional<IdentityParam> selectedIdentity)
	{
		this.msg = msg;
		this.identityPresenter = new IdentityPresentationUtil(msg, idTypeSupport);
		this.handlersRegistry = handlersRegistry;
		this.selectedIdentity = selectedIdentity;

		attributes = new HashMap<>();
		for (DynamicAttribute a : attributesCol)
			attributes.put(a.getAttribute().getName(), a);
		initUI();
	}

	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	public List<DynamicAttribute> getUserFilteredAttributes()
	{
		return new ArrayList<>(attributes.values());
	}

	private void initUI()
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);

		final VerticalLayout details = new VerticalLayout();
		details.setMargin(false);
		details.setSpacing(false);
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(
				msg.getMessage("ExposedAttributesComponent.attributes"),
				true, details);
		showDetails.setId("ExposedAttributes.showDetails");
		
		HtmlLabel credInfo = new HtmlLabel(msg);
		credInfo.setHtmlValue("ExposedAttributesComponent.credInfo");
		credInfo.addStyleName(Styles.vLabelSmall.toString());
		credInfo.setWidth(100, Unit.PERCENTAGE);

		contents.addComponent(showDetails);
		contents.addComponent(details);

		FormLayout attribtuesFL = new FormLayout();
		addIdentity(attribtuesFL);
		addAttributesList(attribtuesFL);
		details.addComponent(attribtuesFL);
		details.addComponent(credInfo);

		setCompositionRoot(contents);
	}

	private void addAttributesList(FormLayout attribtuesFL)
	{
		for (DynamicAttribute dat : attributes.values())
		{
			List<Component> components = getAttributeComponent(dat);
			components.stream().forEach(c -> attribtuesFL.addComponent(c));
		}
	}

	private void addIdentity(FormLayout attribtuesFL)
	{
		if (!selectedIdentity.isPresent())
			return;
		IdentityParam id = selectedIdentity.get();
		attribtuesFL.addComponent(getIdentityTF(id));
	}
	
	private Component getIdentityTF(IdentityParam identity)
	{
		TextField identityField = new TextField(msg.getMessage("IdentitySelectorComponent.identity"));
		identityField.setValue(identityPresenter.getIdentityVisualValue(identity));
		identityField.setReadOnly(true);
		if (!identityField.getValue().equals(identity.getValue()))
		{
			identityField.setDescription(msg.getMessage(
					"IdentitySelectorComponent.fullValue", identity.getValue()));
		}
		return identityField;
	}
	
	

	
	private List<Component> getAttributeComponent(DynamicAttribute dat)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		AttributeViewer attrViewer = new AttributeViewer(msg, handlersRegistry, 
				attributeType, at, false, AttributeViewerContext.builder().withShowConfirmation(false).build()); 
		return attrViewer.getAsComponents(dat.getDisplayedName(), dat.getDescription());
	}
}
