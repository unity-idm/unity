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
import pl.edu.icm.unity.webadmin.credentials.CredentialDefinitionsComponent;
import pl.edu.icm.unity.webadmin.credreq.CredentialRequirementsComponent;

import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

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
	private MainTabPanel tabs;

	@Autowired
	public SchemaManagementTab(UnityMessageSource msg, AttributeTypesComponent attributeTypes,
			CredentialDefinitionsComponent cdComponent, CredentialRequirementsComponent crComponent)
	{
		super();
		this.msg = msg;
		this.tabs = new MainTabPanel(attributeTypes, cdComponent, crComponent);
		this.tabs.setStyleName(Reindeer.TABSHEET_MINIMAL);
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("SchemaManagementTab.caption"));
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		wrapper.addComponent(tabs);
		wrapper.setSizeFull();
		addComponent(wrapper);
		setSizeFull();
	}

}
