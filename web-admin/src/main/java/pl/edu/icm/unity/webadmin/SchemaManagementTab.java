/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesComponent;

import com.vaadin.ui.VerticalLayout;

/**
 * Tab containing management views for the data schema definition as attribute types, attribute classes
 * or identity types.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SchemaManagementTab  extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributeTypesComponent attributeTypes;

	@Autowired
	public SchemaManagementTab(UnityMessageSource msg, AttributeTypesComponent attributeTypes)
	{
		super();
		this.msg = msg;
		this.attributeTypes = attributeTypes;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("SchemaManagementTab.caption"));
		addComponent(attributeTypes);
		setSizeFull();
	}

}
