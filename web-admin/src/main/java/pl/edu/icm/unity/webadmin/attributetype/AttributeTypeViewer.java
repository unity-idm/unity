/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.SafePanel;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Allows to inspect a single attribute type
 * @author K. Benedyczak
 */
public class AttributeTypeViewer extends FormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private DescriptionTextArea typeDescription;
	private Label cardinality;
	private Label uniqueVals;
	private Label selfModificable;
	private Label visibility;
	private Label flags;
	private Label syntax;
	private SafePanel syntaxPanel;
	private VerticalLayout metaPanel;
	
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
		
		typeDescription = new DescriptionTextArea(msg.getMessage("AttributeType.description"), true, "");
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
		
		syntaxPanel = new SafePanel();
		syntaxPanel.setStyleName(Reindeer.PANEL_LIGHT);
		addComponent(syntaxPanel);
		
		metaPanel = new VerticalLayout();
		metaPanel.setCaption(msg.getMessage("AttributeType.metadata"));
		metaPanel.setSpacing(true);
		addComponent(metaPanel);
		
		setContentsVisible(false);
	}
	
	private void setContentsVisible(boolean how)
	{
		name.setVisible(how);
		typeDescription.setVisible(how);
		cardinality.setVisible(how);
		uniqueVals.setVisible(how);
		selfModificable.setVisible(how);
		visibility.setVisible(how);
		flags.setVisible(how);
		syntax.setVisible(how);
		syntaxPanel.setVisible(how);
		metaPanel.setVisible(how);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setInput(AttributeType aType, WebAttributeHandler handler, 
			AttributeMetadataHandlerRegistry metaHandlersReg)
	{
		if (aType == null)
		{
			setContentsVisible(false);
			return;
		}
		
		setContentsVisible(true);
		name.setValue(aType.getName());
		typeDescription.setValue(aType.getDescription());
		cardinality.setValue(AttributeTypeUtils.getBoundsDesc(msg, aType.getMinElements(), aType.getMaxElements()));
		uniqueVals.setValue(AttributeTypeUtils.getBooleanDesc(msg, aType.isUniqueValues()));
		selfModificable.setValue(AttributeTypeUtils.getBooleanDesc(msg, aType.isSelfModificable()));
		visibility.setValue(AttributeTypeUtils.getVisibilityDesc(msg, aType));
		flags.setValue(AttributeTypeUtils.getFlagsDesc(msg, aType));
		String syntaxId = aType.getValueType().getValueSyntaxId();
		syntax.setValue(syntaxId);
		syntaxPanel.setContent(handler.getSyntaxViewer(aType.getValueType()));
		
		metaPanel.removeAllComponents();
		Map<String, String> meta = aType.getMetadata();
		for (Map.Entry<String, String> metaE: meta.entrySet())
		{
			WebAttributeMetadataHandler mHandler = metaHandlersReg.getHandler(metaE.getKey());
			Component metaPresentation = mHandler.getRepresentation(metaE.getValue());
			metaPanel.addComponent(metaPresentation);
		}
		metaPanel.setVisible(!meta.isEmpty());
	}
}
