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
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
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
	private AttributeTypeSupport atSupport;
	
	protected Map<String, DynamicAttribute> attributes;
	protected ListOfSelectableElements attributesHiding;
	private AttributeHandlerRegistry handlersRegistry;

	public ExposedAttributesComponent(UnityMessageSource msg, AttributeTypeSupport atSupport,
			AttributeHandlerRegistry handlersRegistry,
			Collection<DynamicAttribute> attributesCol)
	{
		this.atSupport = atSupport;
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
		contents.setSpacing(true);

		final VerticalLayout details = new VerticalLayout();
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(true, details);

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
		ListOfElements<Label> attributesList = new ListOfElements<>(msg, label -> label);

		for (DynamicAttribute dat : attributes.values())
			attributesList.addEntry(getAttributeComponent(dat));
		
		return attributesList;
	}

	private Label getAttributeComponent(DynamicAttribute dat)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType;
		try
		{
			attributeType = atSupport.getType(at);
		} catch (IllegalArgumentException e)
		{
			// can happen for dynamic attributes from output translation profile
			attributeType = new AttributeType(at.getName(),	StringAttributeSyntax.ID);
		}
		String representation = handlersRegistry.getSimplifiedAttributeRepresentation(at,
				80, getAttributeDisplayedName(dat, attributeType));
		Label labelRep = new Label(representation);
		labelRep.setDescription(getAttributeDescription(dat, attributeType));
		
		return labelRep;
	}

	private String getAttributeDescription(DynamicAttribute dat, AttributeType attributeType)
	{
		String attrDescription = dat.getDescription();
		if (attrDescription == null || attrDescription.isEmpty())
		{
			attrDescription = attributeType.getDescription() != null
					? attributeType.getDescription().getValue(msg)
					: dat.getAttribute().getName();
		}
		
		return attrDescription;
	}

	private String getAttributeDisplayedName(DynamicAttribute dat, AttributeType attributeType)
	{
		String attrDisplayedName = dat.getDisplayedName();
		if (attrDisplayedName == null || attrDisplayedName.isEmpty())
		{
			attrDisplayedName = attributeType.getDisplayedName() != null
					? attributeType.getDisplayedName().getValue(msg)
					: dat.getAttribute().getName();
		}
		
		return attrDisplayedName;
	}
}
