/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Component showing all attributes that are going to be sent to the requesting service. 
 * By default attributes are collapsed.
 * @author K. Benedyczak
 */
public class ExposedAttributesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	protected AttributeHandlerRegistry handlersRegistry;
	
	protected Map<String, Attribute> attributes;
	protected ListOfSelectableElements attributesHiding;

	public ExposedAttributesComponent(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			Collection<Attribute> attributesCol)
	{
		super();
		this.handlersRegistry = handlersRegistry;
		this.msg = msg;

		attributes = new HashMap<String, Attribute>();
		for (Attribute a: attributesCol)
			attributes.put(a.getName(), a);
		initUI();
	}
	
	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	public Collection<Attribute> getUserFilteredAttributes()
	{
		return new ArrayList<Attribute>(attributes.values());
	}
	
	private void initUI()
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setSpacing(true);

		final VerticalLayout details = new VerticalLayout();
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(true, details);

		Label attributesL = new Label(msg.getMessage("ExposedAttributesComponent.attributes"));
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
		
		ListOfElements<String> attributesList = new ListOfElements<String>(msg);
		for (Attribute at: attributes.values())
		{
			String representation = handlersRegistry.getSimplifiedAttributeRepresentation(at, 80);
			attributesList.addEntry(representation);
		}
		details.addComponent(attributesList);
		
		setCompositionRoot(contents);
	}

}
