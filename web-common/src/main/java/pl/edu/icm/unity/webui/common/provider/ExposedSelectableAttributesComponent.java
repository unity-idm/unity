/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.SelectableAttributeWithValues;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Component showing all attributes that are going to be sent to the requesting service. User
 * can select attributes which should be hidden.
 * By default attributes are collapsed.
 * @author K. Benedyczak
 */
public class ExposedSelectableAttributesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	protected AttributeHandlerRegistry handlersRegistry;
	
	protected Map<String, Attribute<?>> attributes;
	protected Map<String, SelectableAttributeWithValues<?>> attributesHiding;
	private AttributesManagement attrMan;

	public ExposedSelectableAttributesComponent(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			AttributesManagement attrMan, Collection<Attribute<?>> attributesCol) throws EngineException
	{
		super();
		this.handlersRegistry = handlersRegistry;
		this.msg = msg;
		this.attrMan = attrMan;

		attributes = new HashMap<>();
		for (Attribute<?> a: attributesCol)
			attributes.put(a.getName(), a);
		initUI();
	}
	
	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	public Map<String, Attribute<?>> getUserFilteredAttributes()
	{
		Map<String, Attribute<?>> ret = new HashMap<>();
		for (Entry<String, SelectableAttributeWithValues<?>> entry : attributesHiding.entrySet())
			ret.put(entry.getKey(), entry.getValue().getAttributeWithoutHidden());
		return ret;
	}

	/**
	 * @return collection of attributes with values hidden by the user.
	 */
	public Map<String, Attribute<?>> getHiddenAttributes()
	{
		Map<String, Attribute<?>> ret = new HashMap<>();
		for (Entry<String, SelectableAttributeWithValues<?>> entry : attributesHiding.entrySet())
			ret.put(entry.getKey(), entry.getValue().getHiddenAttributeValues());
		return ret;
	}
	
	public void setInitialState(Map<String, Attribute<?>> savedState)
	{
		for (Entry<String, Attribute<?>> entry : savedState.entrySet())
		{
			SelectableAttributeWithValues<?> selectableAttributeWithValues = 
					attributesHiding.get(entry.getKey());
			if (selectableAttributeWithValues != null)
				selectableAttributeWithValues.setHiddenValues(entry.getValue());
		}
	}
	
	private void initUI() throws EngineException
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

		HtmlLabel attributesInfo = new HtmlLabel(msg, "ExposedAttributesComponent.attributesInfo");
		attributesInfo.addStyleName(Styles.vLabelSmall.toString());
		details.addComponent(attributesInfo);

		Label hideL = new Label(msg.getMessage("ExposedAttributesComponent.hide"));
		
		attributesHiding = new HashMap<>();
		Map<String, AttributeType> attributeTypes = attrMan.getAttributeTypesAsMap();
		boolean first = true;
		for (Attribute<?> at: attributes.values())
		{
			WebAttributeHandler<?> handler = handlersRegistry.getHandler(
					at.getAttributeSyntax().getValueSyntaxId());
			AttributeType attributeType = attributeTypes.get(at.getName());
			if (attributeType == null) //can happen for dynamic attributes from output translation profile
				attributeType = new AttributeType(at.getName(), new StringAttributeSyntax());
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			SelectableAttributeWithValues<?> attributeComponent = new SelectableAttributeWithValues(
					null, hideL, at, attributeType, handler, msg);
			
			if (first)
			{
				first = false;
				hideL = null;
			}
			
			attributesHiding.put(at.getName(), attributeComponent);
			details.addComponent(attributeComponent);
		}
		
		setCompositionRoot(contents);
	}

}
