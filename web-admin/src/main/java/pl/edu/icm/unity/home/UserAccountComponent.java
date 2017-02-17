/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.iddetails.EntityDetailsWithActions;
import pl.edu.icm.unity.home.iddetails.EntityRemovalButton;
import pl.edu.icm.unity.home.iddetails.UserAttributesPanel;
import pl.edu.icm.unity.home.iddetails.UserDetailsPanel;
import pl.edu.icm.unity.home.iddetails.UserIdentitiesPanel;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webadmin.preferences.PreferencesComponent;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider.WizardFinishedCallback;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.bigtab.BigTabPanel;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialsPanel;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandlerRegistry;

import com.vaadin.ui.Button;
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
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private CredentialEditorRegistry credEditorReg;
	private PreferencesHandlerRegistry registry;
	private PreferencesManagement prefMan;
	private EndpointManagement endpMan; 
	private AttributesInternalProcessing attrMan;
	private WebAuthenticationProcessor authnProcessor;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attributesMan;
	private IdentityEditorRegistry identityEditorRegistry;
	private SandboxAuthnNotifier sandboxNotifier;
	private String sandboxURL;
	private InputTranslationEngine inputTranslationEngine;
	private IdentitiesManagement insecureIdsMan;
	
	@Autowired
	public UserAccountComponent(UnityMessageSource msg, AuthenticationManagement authnMan,
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg,
			@Qualifier("insecure") IdentitiesManagement insecureIdsMan, 
			PreferencesHandlerRegistry registry, PreferencesManagement prefMan,
			EndpointManagement endpMan, AttributesInternalProcessing attrMan,
			WebAuthenticationProcessor authnProcessor,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attributesMan, IdentityEditorRegistry identityEditorRegistry,
			InputTranslationEngine inputTranslationEngine)
	{
		this.msg = msg;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.credEditorReg = credEditorReg;
		this.insecureIdsMan = insecureIdsMan;
		this.registry = registry;
		this.prefMan = prefMan;
		this.endpMan = endpMan;
		this.attrMan = attrMan;
		this.authnProcessor = authnProcessor;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attributesMan = attributesMan;
		this.identityEditorRegistry = identityEditorRegistry;
		this.inputTranslationEngine = inputTranslationEngine;
	}

	public void initUI(HomeEndpointProperties config, SandboxAuthnNotifier sandboxNotifier, String sandboxURL)
	{
		this.sandboxNotifier = sandboxNotifier;
		this.sandboxURL = sandboxURL;
		Label spacer = new Label();
		spacer.setHeight(20, Unit.PIXELS);
		addComponent(spacer);
		BigTabPanel tabPanel = new BigTabPanel(130, Unit.PIXELS, msg);
		tabPanel.setSizeFull();
		addComponent(tabPanel);
		setExpandRatio(tabPanel, 1.0f);

		Set<String> disabled = config.getDisabledComponents();
		
		LoginSession theUser = InvocationContext.getCurrent().getLoginSession();

		if (!disabled.contains(HomeEndpointProperties.Components.userDetailsTab.toString()))
			addUserInfo(tabPanel, theUser, config, disabled);
		
		if (!disabled.contains(HomeEndpointProperties.Components.credentialTab.toString()))
			addCredentials(tabPanel, theUser);

		if (!disabled.contains(HomeEndpointProperties.Components.preferencesTab.toString()))
			addPreferences(tabPanel);
		
		if (tabPanel.getTabsCount() > 0)
			tabPanel.select(0);
	}
	
	private void addUserInfo(BigTabPanel tabPanel, LoginSession theUser, HomeEndpointProperties config, 
			Set<String> disabled)
	{
		try
		{
			UserDetailsPanel userInfo = getUserInfoComponent(theUser.getEntityId(), idsMan, attrMan);
			Button removalButton = getRemovalButton(theUser, config);
			final UserIdentitiesPanel idsPanel = new UserIdentitiesPanel(msg, 
					identityEditorRegistry, idsMan, theUser.getEntityId());
			final UserAttributesPanel attrsPanel = new UserAttributesPanel(msg, attributeHandlerRegistry, 
					attributesMan, idsMan, config, theUser.getEntityId());
			ConnectIdWizardProvider connectIdProvider = new ConnectIdWizardProvider(msg, 
					sandboxURL, sandboxNotifier, inputTranslationEngine, new WizardFinishedCallback()
					{
						@Override
						public void onCancel()
						{
						}

						@Override
						public void onSuccess(MappingResult mergedIdentity)
						{
							try
							{
								idsPanel.refresh();
								attrsPanel.refresh();
							} catch (EngineException e)
							{
								NotificationPopup.showError(msg, msg.getMessage("error"), e);
							}							
						}

						@Override
						public void onError(Exception error)
						{
						}
					});
			EntityDetailsWithActions tabRoot = new EntityDetailsWithActions(disabled, 
					userInfo, idsPanel, attrsPanel, removalButton, msg, connectIdProvider);
			tabPanel.addTab("UserHomeUI.accountInfoLabel", "UserHomeUI.accountInfoDesc", 
					Images.info64.getResource(), tabRoot);
		} catch (AuthorizationException e)
		{
			//OK - rather shouldn't happen but the user is not authorized to even see the entity details.
		} catch (Exception e)
		{
			log.error("Error when creating user information view", e);
			ErrorComponent errorC = new ErrorComponent();
			errorC.setError(msg.getMessage("error") + ": " + NotificationPopup.getHumanMessage(e));
			tabPanel.addTab("UserHomeUI.accountInfoLabel", "UserHomeUI.accountInfoDesc", 
					Images.info64.getResource(), errorC);
		}
	}
	
	private void addCredentials(BigTabPanel tabPanel, LoginSession theUser)
	{
		try
		{
			CredentialsPanel credentialsPanel = new CredentialsPanel(msg, theUser.getEntityId(), 
					authnMan, idsMan, credEditorReg, true);
			if (!credentialsPanel.isCredentialRequirementEmpty())
				tabPanel.addTab("UserHomeUI.credentialsLabel", "UserHomeUI.credentialsDesc", 
					Images.key64.getResource(), credentialsPanel);
		} catch (Exception e)
		{
			if (!(e instanceof AuthorizationException || 
					(e.getCause() != null && e.getCause() instanceof AuthorizationException)))
			{
				log.error("Error when creating credentials view", e);
				ErrorComponent errorC = new ErrorComponent();
				errorC.setError(msg.getMessage("error") + ": " + NotificationPopup.getHumanMessage(e));
				tabPanel.addTab("UserHomeUI.credentialsLabel", "UserHomeUI.credentialsDesc", 
					Images.key64.getResource(), errorC);
			}
		}
	}
	
	private void addPreferences(BigTabPanel tabPanel)
	{
		PreferencesComponent preferencesComponent = new PreferencesComponent(msg, registry, prefMan, endpMan);
		tabPanel.addTab("UserHomeUI.preferencesLabel", "UserHomeUI.preferencesDesc", 
				Images.settings64.getResource(), preferencesComponent);
	}
	
	private UserDetailsPanel getUserInfoComponent(long entityId, IdentitiesManagement idsMan, 
			AttributesInternalProcessing attrMan) throws EngineException
	{
		UserDetailsPanel ret = new UserDetailsPanel(msg);
		EntityParam param = new EntityParam(entityId);
		Collection<Group> groups = new HashSet<Group>();
		try
		{
			groups = idsMan.getGroupsForPresentation(param);
		} catch (AuthorizationException e)
		{
			//OK, let's skip this.
		}
		Entity entity = idsMan.getEntity(param);
		String label = idsMan.getEntityLabel(param);
		EntityWithLabel entityWithLabel = new EntityWithLabel(entity, label);
		ret.setInput(entityWithLabel, groups);
		return ret;
	}
	
	private Button getRemovalButton(LoginSession theUser, HomeEndpointProperties config)
	{
		return new EntityRemovalButton(msg, theUser.getEntityId(), idsMan, 
				insecureIdsMan, authnProcessor, config);
	}
}
