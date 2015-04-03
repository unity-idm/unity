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
import pl.edu.icm.unity.webadmin.attributeclass.AttributesClassesComponent;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesComponent;
import pl.edu.icm.unity.webadmin.credentials.CredentialDefinitionsComponent;
import pl.edu.icm.unity.webadmin.credreq.CredentialRequirementsComponent;

import com.vaadin.ui.VerticalLayout;
import pl.edu.icm.unity.webui.common.Styles;

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
			CredentialDefinitionsComponent cdComponent, CredentialRequirementsComponent crComponent,
			AttributesClassesComponent acComponent)
	{
		super();
		this.msg = msg;
		this.tabs = new MainTabPanel(attributeTypes, acComponent, cdComponent, crComponent);
		this.tabs.addStyleName(Styles.vTabsheetMinimal.toString());
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
