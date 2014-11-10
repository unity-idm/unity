/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements.DisableMode;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component showing all attributes that are going to be sent to the requesting service. 
 * By default attributes are hidden.
 * @author K. Benedyczak
 */
public class ExposedAttributesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	protected AttributeHandlerRegistry handlersRegistry;
	
	protected Map<String, Attribute<?>> attributes;
	protected ListOfSelectableElements attributesHiding;
	protected boolean allowHiding;

	public ExposedAttributesComponent(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			Collection<Attribute<?>> attributesCol, boolean allowHiding)
	{
		super();
		this.handlersRegistry = handlersRegistry;
		this.msg = msg;
		this.allowHiding = allowHiding;

		attributes = new HashMap<String, Attribute<?>>();
		for (Attribute<?> a: attributesCol)
			attributes.put(a.getName(), a);
		initUI();
	}
	
	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	public Collection<Attribute<?>> getUserFilteredAttributes()
	{
		if (!allowHiding)
			return new ArrayList<Attribute<?>>(attributes.values());
		
		Set<String> hidden = new HashSet<String>();
		for (CheckBox cb: attributesHiding.getSelection())
			if (cb.getValue())
				hidden.add((String) cb.getData());
		
		List<Attribute<?>> ret = new ArrayList<Attribute<?>>(attributes.size());
		for (Attribute<?> a: attributes.values())
			if (!hidden.contains(a.getName()))
				ret.add(a);
		return ret;
	}
	
	public void setHidden(Set<String> hidden)
	{
		if (!allowHiding)
			return;
		for (CheckBox cb: attributesHiding.getSelection())
		{
			String a = (String) cb.getData();
			if (hidden.contains(a))
				cb.setValue(true);
		}
	}
	
	public Set<String> getHidden()
	{
		Set<String> hidden = new HashSet<String>();
		if (!allowHiding)
			return hidden;
		for (CheckBox h: attributesHiding.getSelection())
		{
			if (!h.getValue())
				continue;
			String a = (String) h.getData();
			hidden.add(a);
		}
		return hidden;
	}
	
	private void initUI()
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setSpacing(true);

		final VerticalLayout details = new VerticalLayout();
		final ExpandCollapseButton showDetails = new ExpandCollapseButton(true, details);

		Label attributesL = new Label(msg.getMessage("ExposedAttributesComponent.attributes"));
		attributesL.setStyleName(Styles.bold.toString());
		
		
		Label credInfo = new Label(msg.getMessage("ExposedAttributesComponent.credInfo"));
		credInfo.setStyleName(Reindeer.LABEL_SMALL);
		credInfo.setContentMode(ContentMode.HTML);
		
		contents.addComponent(attributesL);
		contents.addComponent(showDetails);
		contents.addComponent(details);
		
		details.addComponent(credInfo);
		if (allowHiding)
		{
			Label attributesInfo = new Label(msg.getMessage("ExposedAttributesComponent.attributesInfo"));
			attributesInfo.setStyleName(Reindeer.LABEL_SMALL);
			attributesInfo.setContentMode(ContentMode.HTML);
			details.addComponent(attributesInfo);
			
			Label hideL = new Label(msg.getMessage("ExposedAttributesComponent.hide"));
			attributesHiding = new ListOfSelectableElements(null, hideL, DisableMode.WHEN_SELECTED);
			for (Attribute<?> at: attributes.values())
			{
				Label attrInfo = new Label();
				String representation = handlersRegistry.getSimplifiedAttributeRepresentation(at, 80);
				attrInfo.setValue(representation);
				attributesHiding.addEntry(attrInfo, false, at.getName());
			}
			details.addComponent(attributesHiding);
		} else
		{
			Label spacer = new Label("<br>");
			spacer.setStyleName(Reindeer.LABEL_SMALL);
			spacer.setContentMode(ContentMode.HTML);
			details.addComponent(spacer);
			
			ListOfElements<String> attributesList = new ListOfElements<String>(msg);
			for (Attribute<?> at: attributes.values())
			{
				String representation = handlersRegistry.getSimplifiedAttributeRepresentation(at, 80);
				attributesList.addEntry(representation);
			}
			details.addComponent(attributesList);
		}
		
		setCompositionRoot(contents);
	}

}
