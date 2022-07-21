/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.home.externalApplication.TrustedApplicationTab;
import io.imunity.home.iddetails.EntityDetailsWithActions;
import io.imunity.home.iddetails.EntityRemovalButton;
import io.imunity.home.iddetails.UserAttributesPanel;
import io.imunity.home.iddetails.UserDetailsPanel;
import io.imunity.home.iddetails.UserIdentitiesPanel;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider.WizardFinishedCallback;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.bigtab.BigTabPanel;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialsPanel;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseEditorController;
import pl.edu.icm.unity.webui.forms.enquiry.StickyEnquiryUpdatableComponent;

/**
 * Component with user's account management UI.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserAccountComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UserAccountComponent.class);
	private MessageSource msg;
	private CredentialManagement credMan;
	private EntityManagement idsMan;
	private CredentialEditorRegistry credEditorReg;

	private AttributeSupport atSupport;
	private StandardWebLogoutHandler authnProcessor;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attributesMan;
	private IdentityEditorRegistry identityEditorRegistry;
	private SandboxAuthnNotifier sandboxNotifier;
	private String sandboxURL;
	private InputTranslationEngine inputTranslationEngine;
	private EntityCredentialManagement ecredMan;
	private CredentialRequirementManagement credReqMan;
	private IdentityTypeSupport idTypeSupport;
	private EntityManagement insecureIdsMan;
	private TokensManagement tokenMan;
	private AdditionalAuthnHandler additionalAuthnHandler;
	private EnquiryResponseEditorController enquiryResController;
	private final ObjectFactory<TrustedApplicationTab> externalAppFactory;

	@Autowired
	public UserAccountComponent(MessageSource msg, CredentialManagement credMan, EntityManagement idsMan,
			EntityCredentialManagement ecredMan, CredentialRequirementManagement credReqMan,
			CredentialEditorRegistry credEditorReg, @Qualifier("insecure") EntityManagement insecureIdsMan,
			AttributeSupport attrMan, StandardWebLogoutHandler authnProcessor,
			AttributeHandlerRegistry attributeHandlerRegistry, AttributesManagement attributesMan,
			IdentityEditorRegistry identityEditorRegistry, InputTranslationEngine inputTranslationEngine,
			IdentityTypeSupport idTypeSupport, TokensManagement tokenMan, AdditionalAuthnHandler additionalAuthnHandler,
			EnquiryResponseEditorController enquiryResController,
			ObjectFactory<TrustedApplicationTab> externalAppFactory)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.idsMan = idsMan;
		this.ecredMan = ecredMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		this.insecureIdsMan = insecureIdsMan;
		this.atSupport = attrMan;
		this.authnProcessor = authnProcessor;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attributesMan = attributesMan;
		this.identityEditorRegistry = identityEditorRegistry;
		this.inputTranslationEngine = inputTranslationEngine;
		this.idTypeSupport = idTypeSupport;
		this.tokenMan = tokenMan;
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.enquiryResController = enquiryResController;
		this.externalAppFactory = externalAppFactory;
	}

	public void initUI(HomeEndpointProperties config, SandboxAuthnNotifier sandboxNotifier, String sandboxURL)
	{
		this.sandboxNotifier = sandboxNotifier;
		this.sandboxURL = sandboxURL;
		setMargin(false);
		setSpacing(false);
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
			addCredentials(tabPanel, theUser, config.getBooleanValue(HomeEndpointProperties.DISABLE_2ND_FACTOR_OPT_IN));

		if (!disabled.contains(HomeEndpointProperties.Components.trustedApplicationTab.toString()))
			addTrustedApplicationATab(tabPanel);

		if (!disabled.contains(HomeEndpointProperties.Components.accountUpdateTab.toString()))
			addAccountUpdate(tabPanel, config.getEnabledEnquiries());

		if (tabPanel.getTabsCount() > 0)
			tabPanel.select(0);
	}

	private void addTrustedApplicationATab(BigTabPanel tabPanel)
	{
		TrustedApplicationTab externalApplicationAccessTab = externalAppFactory.getObject();
		tabPanel.addTab("UserHomeUI.trustedApplicationLabel", "UserHomeUI.trustedApplicationDesc", Images.globe,
				externalApplicationAccessTab, (src) -> externalApplicationAccessTab.refresh());

	}

	private void addAccountUpdate(BigTabPanel tabPanel, List<String> enquiries)
	{
		try
		{
			VerticalLayout main = new VerticalLayout();
			main.setSpacing(false);
			main.setMargin(false);
			StickyEnquiryUpdatableComponent updater = new StickyEnquiryUpdatableComponent(msg, enquiryResController,
					enquiries);
			if (updater.isFormsAreApplicable())
			{
				main.addComponent(updater);
				tabPanel.addTab("UserHomeUI.accountUpdateLabel", "UserHomeUI.accountUpdateDesc", Images.records, main,
						t -> updater.reload());
			}

		} catch (Exception e)
		{
			log.error("Error when creating enquiries view", e);
			ErrorComponent errorC = new ErrorComponent();
			errorC.setError(msg.getMessage("error") + ": " + NotificationPopup.getHumanMessage(e));
			tabPanel.addTab("UserHomeUI.enquiryLabel", "UserHomeUI.enquiryDesc", Images.records, errorC);
		}

	}

	private void addUserInfo(BigTabPanel tabPanel, LoginSession theUser, HomeEndpointProperties config,
			Set<String> disabled)
	{
		try
		{
			UserDetailsPanel userInfo = getUserInfoComponent(theUser.getEntityId(), idsMan, atSupport);
			Button removalButton = getRemovalButton(theUser, config);
			final UserIdentitiesPanel idsPanel = new UserIdentitiesPanel(msg, identityEditorRegistry, idsMan,
					theUser.getEntityId(), idTypeSupport);
			final UserAttributesPanel attrsPanel = new UserAttributesPanel(additionalAuthnHandler, msg,
					attributeHandlerRegistry, attributesMan, idsMan, atSupport, config, theUser.getEntityId());
			ConnectIdWizardProvider connectIdProvider = new ConnectIdWizardProvider(msg, sandboxURL, sandboxNotifier,
					inputTranslationEngine, new WizardFinishedCallback()
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
			EntityDetailsWithActions tabRoot = new EntityDetailsWithActions(disabled, userInfo, idsPanel, attrsPanel,
					removalButton, msg, connectIdProvider);
			tabPanel.addTab("UserHomeUI.accountInfoLabel", "UserHomeUI.accountInfoDesc", Images.info, tabRoot);
		} catch (AuthorizationException e)
		{
			// OK - rather shouldn't happen but the user is not authorized to even see the
			// entity details.
		} catch (Exception e)
		{
			log.error("Error when creating user information view", e);
			ErrorComponent errorC = new ErrorComponent();
			errorC.setError(msg.getMessage("error") + ": " + NotificationPopup.getHumanMessage(e));
			tabPanel.addTab("UserHomeUI.accountInfoLabel", "UserHomeUI.accountInfoDesc", Images.info, errorC);
		}
	}

	private void addCredentials(BigTabPanel tabPanel, LoginSession theUser, boolean disable2ndFactorOptIn)
	{
		try
		{
			CredentialsPanel credentialsPanel = new CredentialsPanel(additionalAuthnHandler, msg, theUser.getEntityId(),
					credMan, ecredMan, idsMan, credReqMan, credEditorReg, tokenMan, true, disable2ndFactorOptIn);
			if (!credentialsPanel.isCredentialRequirementEmpty())
				tabPanel.addTab("UserHomeUI.credentialsLabel", "UserHomeUI.credentialsDesc", Images.key_o,
						credentialsPanel);
		} catch (Exception e)
		{
			if (!(e instanceof AuthorizationException
					|| (e.getCause() != null && e.getCause() instanceof AuthorizationException)))
			{
				log.error("Error when creating credentials view", e);
				ErrorComponent errorC = new ErrorComponent();
				errorC.setError(msg.getMessage("error") + ": " + NotificationPopup.getHumanMessage(e));
				tabPanel.addTab("UserHomeUI.credentialsLabel", "UserHomeUI.credentialsDesc", Images.key_o, errorC);
			}
		}
	}

	private UserDetailsPanel getUserInfoComponent(long entityId, EntityManagement idsMan, AttributeSupport attrMan)
			throws EngineException
	{
		UserDetailsPanel ret = new UserDetailsPanel(msg);
		EntityParam param = new EntityParam(entityId);
		Collection<Group> groups = new HashSet<Group>();
		try
		{
			groups = idsMan.getGroupsForPresentation(param);
		} catch (AuthorizationException e)
		{
			// OK, let's skip this.
		}
		Entity entity = idsMan.getEntity(param);
		String label = idsMan.getEntityLabel(param);
		EntityWithLabel entityWithLabel = new EntityWithLabel(entity, label);
		ret.setInput(entityWithLabel, groups);
		return ret;
	}

	private Button getRemovalButton(LoginSession theUser, HomeEndpointProperties config)
	{
		return new EntityRemovalButton(msg, theUser.getEntityId(), idsMan, insecureIdsMan, authnProcessor, config);
	}
}
