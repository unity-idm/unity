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

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewer;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Component showing all attributes that are going to be sent to the requesting
 * service. By default attributes are collapsed.
 * 
 * @author K. Benedyczak
 */
public class ExposedAttributesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	
	protected Map<String, DynamicAttribute> attributes;
	protected ListOfSelectableElements attributesHiding;
	private AttributeHandlerRegistry handlersRegistry;

	public ExposedAttributesComponent(UnityMessageSource msg,
			AttributeHandlerRegistry handlersRegistry,
			Collection<DynamicAttribute> attributesCol)
	{
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;

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
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(true, details);
		showDetails.setId("ExposedAttributes.showDetails");
		
		Label attributesL = new Label(
				msg.getMessage("ExposedAttributesComponent.attributes"));
		attributesL.addStyleName(Styles.bold.toString());

		HtmlLabel credInfo = new HtmlLabel(msg);
		credInfo.setHtmlValue("ExposedAttributesComponent.credInfo");
		credInfo.addStyleName(Styles.vLabelSmall.toString());

		contents.addComponent(attributesL);
		contents.addComponent(showDetails);
		contents.addComponent(details);

		details.addComponent(credInfo);
		Label spacer = HtmlTag.br();
		spacer.setStyleName(Styles.vLabelSmall.toString());
		details.addComponent(spacer);
		details.addComponent(getAttributesListComponent());

		setCompositionRoot(contents);
	}

	private Component getAttributesListComponent()
	{
		FormLayout attributesList = new FormLayout();
		for (DynamicAttribute dat : attributes.values())
		{
			List<Component> components = getAttributeComponent(dat);
			components.stream().forEach(c -> attributesList.addComponent(c));
		}
		
		return attributesList;
	}

	private List<Component> getAttributeComponent(DynamicAttribute dat)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		AttributeViewer attrViewer = new AttributeViewer(msg, handlersRegistry, 
				attributeType, at, false, AttributeViewerContext.EMPTY); 
		return attrViewer.getAsComponents(dat.getDisplayedName(), dat.getDescription());
	}
}
