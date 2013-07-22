/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.preferences.PreferencesComponent;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.TopHeader;
import pl.edu.icm.unity.webui.common.bigtab.BigTabPanel;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialsPanel;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandlerRegistry;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("UserHomeUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class UserHomeUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UserHomeUI.class);

	private EndpointDescription endpointDescription;

	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private CredentialEditorRegistry credEditorReg;
	private PreferencesHandlerRegistry registry;
	private PreferencesManagement prefMan;

	@Autowired
	public UserHomeUI(UnityMessageSource msg, AuthenticationManagement authnMan,
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg,
			PreferencesHandlerRegistry registry, PreferencesManagement prefMan)
	{
		super(msg);
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.credEditorReg = credEditorReg;
		this.registry = registry;
		this.prefMan = prefMan;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		this.endpointDescription = description;
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();
		TopHeader header = new TopHeader(endpointDescription.getId(), msg);
		contents.addComponent(header);

		Label spacer = new Label();
		spacer.setHeight(20, Unit.PIXELS);
		contents.addComponent(spacer);
		BigTabPanel tabPanel = new BigTabPanel(100, Unit.PIXELS, msg);
		contents.addComponent(tabPanel);
		
		Label tmp1 = new Label("INFO panel");
		tabPanel.addTab("UserHomeUI.accountInfoLabel", "UserHomeUI.accountInfoDesc", 
				Images.hInfo.getResource(), tmp1);
		
		AuthenticatedEntity theUser = InvocationContext.getCurrent().getAuthenticatedEntity();
		try
		{
			CredentialsPanel credentialsPanel = new CredentialsPanel(msg, theUser.getEntityId(), 
					authnMan, idsMan, credEditorReg);
			tabPanel.addTab("UserHomeUI.credentialsLabel", "UserHomeUI.credentialsDesc", 
					Images.hKey.getResource(), credentialsPanel);
		} catch (AuthorizationException e)
		{
			//OK - rather shouldn't happen but the user is not authorized to even see the credentials.
		} catch (Exception e)
		{
			log.error("Error when creating credentials view", e);
			ErrorPopup.showError(msg.getMessage("error"), e);
		}

		PreferencesComponent preferencesComponent = new PreferencesComponent(msg, registry, prefMan);
		tabPanel.addTab("UserHomeUI.preferencesLabel", "UserHomeUI.preferencesDesc", 
				Images.hSettings.getResource(), preferencesComponent);
		
		
		tabPanel.setWidth(80, Unit.PERCENTAGE);
		tabPanel.select(0);
		contents.setComponentAlignment(tabPanel, Alignment.TOP_CENTER);
		contents.setExpandRatio(tabPanel, 1.0f);
		
		contents.setSizeFull();
		setContent(contents);
	}
}


