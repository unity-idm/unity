/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webadmin.bulk.BulkProcessingComponent;
import pl.edu.icm.unity.webadmin.msgtemplate.MessageTemplatesComponent;
import pl.edu.icm.unity.webadmin.serverman.AuthenticatorsComponent;
import pl.edu.icm.unity.webadmin.serverman.EndpointsComponent;
import pl.edu.icm.unity.webadmin.serverman.ImportExportComponent;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfilesComponent;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.providers.AdminUITabProvider;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;

/**
 * Tab containing server management views.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerManagementTab  extends VerticalLayout
{
	private UnityMessageSource msg;
	private MainTabPanel tabs;
	private TranslationProfilesComponent tComponent;

	@Autowired
	public ServerManagementTab(UnityMessageSource msg, ImportExportComponent ieComponent,
			EndpointsComponent eComponent, AuthenticatorsComponent aComponent,
			TranslationProfilesComponent tComponent, MessageTemplatesComponent msgComponent, 
			BulkProcessingComponent bulkProcessingComponent, AdminUITabProvider provider)
	{
		super();
		this.msg = msg;
		this.tComponent = tComponent;
		this.tabs = new MainTabPanel(eComponent, aComponent, tComponent, 
				msgComponent, ieComponent, bulkProcessingComponent, provider.getUI());
		this.tabs.addStyleName(Styles.vTabsheetMinimal.toString());
		initUI();
	}

	private void initUI()
	{
		setMargin(false);
		setSpacing(false);
		setCaption(msg.getMessage("ServerManagementTab.caption"));
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.addComponent(tabs);
		wrapper.setSizeFull();
		addComponent(wrapper);
		setSizeFull();
	}

	public void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier) 
	{
		tComponent.setSandboxNotifier(sandboxNotifier);
	}

}
