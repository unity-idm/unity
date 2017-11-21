/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.Map;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Allows to inspect a single attribute type
 * @author K. Benedyczak
 */
public class AttributeTypeViewer extends CompactFormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private I18nLabel displayedName;
	private I18nLabel typeDescription;
	private Label cardinality;
	private Label uniqueVals;
	private Label selfModificable;
	private Label flags;
	private Label syntax;
	private SafePanel syntaxPanel;
	private VerticalLayout metaPanel;

	public AttributeTypeViewer(UnityMessageSource msg)
	{
		this.msg = msg;
		initUI();
	}
	
	private void initUI()
	{
		name = new Label();
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
		
		displayedName = new I18nLabel(msg, msg.getMessage("AttributeType.displayedName"));
		addComponent(displayedName);
		
		typeDescription = new I18nLabel(msg, msg.getMessage("AttributeType.description"));
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
		
		flags = new Label();
		flags.setCaption(msg.getMessage("AttributeType.flags"));
		addComponent(flags);
		
		syntax = new Label();
		syntax.setCaption(msg.getMessage("AttributeType.type"));
		addComponent(syntax);
		
		syntaxPanel = new SafePanel();
		syntaxPanel.setStyleName(Styles.vPanelLight.toString());
		addComponent(syntaxPanel);
		
		metaPanel = new VerticalLayout();
		metaPanel.setCaption(msg.getMessage("AttributeType.metadata"));
		metaPanel.setMargin(false);
		addComponent(metaPanel);
		
		setContentsVisible(false);
	}
	
	private void setContentsVisible(boolean how)
	{
		name.setVisible(how);
		displayedName.setVisible(how);
		typeDescription.setVisible(how);
		cardinality.setVisible(how);
		uniqueVals.setVisible(how);
		selfModificable.setVisible(how);
		flags.setVisible(how);
		syntax.setVisible(how);
		syntaxPanel.setVisible(how);
		metaPanel.setVisible(how);
	}
	
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
		displayedName.setValue(aType.getDisplayedName());
		typeDescription.setValue(aType.getDescription());
		cardinality.setValue(AttributeTypeUtils.getBoundsDesc(msg, aType.getMinElements(), aType.getMaxElements()));
		uniqueVals.setValue(AttributeTypeUtils.getBooleanDesc(msg, aType.isUniqueValues()));
		selfModificable.setValue(AttributeTypeUtils.getBooleanDesc(msg, aType.isSelfModificable()));
		flags.setValue(AttributeTypeUtils.getFlagsDesc(msg, aType));
		syntax.setValue(aType.getValueSyntax());
		syntaxPanel.setContent(handler.getSyntaxViewer());
		
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
