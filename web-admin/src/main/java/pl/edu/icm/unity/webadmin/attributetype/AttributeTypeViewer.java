/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

/**
 * Allows to inspect a single attribute type
 * @author K. Benedyczak
 */
public class AttributeTypeViewer extends FormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private TextArea typeDescription;
	private Label cardinality;
	private Label uniqueVals;
	private Label selfModificable;
	private Label visibility;
	private Label flags;
	private Label syntax;
	private Panel syntaxPanel;
	
	public AttributeTypeViewer(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
		
		initUI();
	}
	
	private void initUI()
	{
		name = new Label();
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
		
		typeDescription = new TextArea(msg.getMessage("AttributeType.description"));
		typeDescription.setReadOnly(true);
		typeDescription.setSizeFull();
		typeDescription.setWordwrap(true);
		typeDescription.setHeight(3, Unit.EM);
		addComponent(typeDescription);
		
		cardinality = new Label();
		cardinality.setCaption(msg.getMessage("AttributeType.cardinality"));
		addComponent(cardinality);
		
		uniqueVals = new Label();
		uniqueVals.setCaption(msg.getMessage("AttributeType.uniqueValues"));
		addComponent(uniqueVals);
		
		selfModificable = new Label();
		selfModificable.setCaption(msg.getMessage("AttributeType.selfModificable"));
		addComponent(selfModificable);
		
		visibility = new Label();
		visibility.setCaption(msg.getMessage("AttributeType.visibility"));
		addComponent(visibility);
		
		flags = new Label();
		flags.setCaption(msg.getMessage("AttributeType.flags"));
		addComponent(flags);
		
		syntax = new Label();
		syntax.setCaption(msg.getMessage("AttributeType.type"));
		addComponent(syntax);
		
		syntaxPanel = new Panel();
		syntaxPanel.setStyleName(Reindeer.PANEL_LIGHT);
		addComponent(syntaxPanel);
		
		setEmpty();
	}
	
	private void setEmpty()
	{
		name.setValue(msg.getMessage("AttributeType.notSelected"));
		setDesc("");
		cardinality.setValue("");
		uniqueVals.setValue("");
		selfModificable.setValue("");
		visibility.setValue("");
		flags.setValue("");
		syntax.setValue("");
		syntaxPanel.setContent(new VerticalLayout());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setInput(AttributeType aType, WebAttributeHandler handler)
	{
		if (aType == null)
		{
			setEmpty();
			return;
		}
		
		name.setValue(aType.getName());
		setDesc(aType.getDescription());
		cardinality.setValue(AttributeTypeUtils.getBoundsDesc(msg, aType.getMinElements(), aType.getMaxElements()));
		uniqueVals.setValue(AttributeTypeUtils.getBooleanDesc(msg, aType.isUniqueValues()));
		selfModificable.setValue(AttributeTypeUtils.getBooleanDesc(msg, aType.isSelfModificable()));
		visibility.setValue(AttributeTypeUtils.getVisibilityDesc(msg, aType));
		flags.setValue(AttributeTypeUtils.getFlagsDesc(msg, aType));
		String syntaxId = aType.getValueType().getValueSyntaxId();
		syntax.setValue(syntaxId);
		syntaxPanel.setContent(handler.getSyntaxViewer(aType.getValueType()));
	}
	
	private void setDesc(String val)
	{
		typeDescription.setReadOnly(false);
		typeDescription.setValue(val);
		typeDescription.setReadOnly(true);
	}

}
