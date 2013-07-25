/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.iddetails.EntityDetailsPanel;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webadmin.preferences.PreferencesComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.bigtab.BigTabPanel;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialsPanel;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandlerRegistry;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Component with user's account management UI.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserAccountComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UserAccountComponent.class);
	private UnityMessageSource msg;
	
	@Autowired
	public UserAccountComponent(AuthenticationManagement authnMan, IdentitiesManagement idsMan,
			CredentialEditorRegistry credEditorReg,
			PreferencesHandlerRegistry registry, PreferencesManagement prefMan, 
			UnityMessageSource msg, EndpointManagement endpMan)
	{
		this.msg = msg;
		
		Label spacer = new Label();
		spacer.setHeight(20, Unit.PIXELS);
		addComponent(spacer);
		BigTabPanel tabPanel = new BigTabPanel(100, Unit.PIXELS, msg);
		tabPanel.setSizeFull();
		addComponent(tabPanel);
		setExpandRatio(tabPanel, 1.0f);

		AuthenticatedEntity theUser = InvocationContext.getCurrent().getAuthenticatedEntity();

		
		try
		{
			com.vaadin.ui.Component userInfo = getUserInfoComponent(theUser.getEntityId(), idsMan);
			tabPanel.addTab("UserHomeUI.accountInfoLabel", "UserHomeUI.accountInfoDesc", 
					Images.info64.getResource(), userInfo);
		} catch (AuthorizationException e)
		{
			//OK - rather shouldn't happen but the user is not authorized to even see the entity details.
		} catch (Exception e)
		{
			log.error("Error when creating user information view", e);
			ErrorPopup.showError(msg.getMessage("error"), e);
		}
		
		try
		{
			CredentialsPanel credentialsPanel = new CredentialsPanel(msg, theUser.getEntityId(), 
					authnMan, idsMan, credEditorReg);
			tabPanel.addTab("UserHomeUI.credentialsLabel", "UserHomeUI.credentialsDesc", 
					Images.key64.getResource(), credentialsPanel);
		} catch (AuthorizationException e)
		{
			//OK - rather shouldn't happen but the user is not authorized to even see the credentials.
		} catch (Exception e)
		{
			log.error("Error when creating credentials view", e);
			ErrorPopup.showError(msg.getMessage("error"), e);
		}

		PreferencesComponent preferencesComponent = new PreferencesComponent(msg, registry, prefMan, endpMan);
		tabPanel.addTab("UserHomeUI.preferencesLabel", "UserHomeUI.preferencesDesc", 
				Images.settings64.getResource(), preferencesComponent);
		
		tabPanel.select(0);
	}
	
	private com.vaadin.ui.Component getUserInfoComponent(long entityId, IdentitiesManagement idsMan) 
			throws EngineException
	{
		EntityDetailsPanel ret = new EntityDetailsPanel(msg);
		EntityParam param = new EntityParam(entityId);
		Collection<String> groups;
		try
		{
			groups = idsMan.getGroups(param);
		} catch (AuthorizationException e)
		{
			groups = new HashSet<String>();
			groups.add(msg.getMessage("UserHomeUI.unauthzGroups"));
		}
		Entity entity = idsMan.getEntity(param);
		ret.setInput(entity, groups);
		return ret;
	}
}
