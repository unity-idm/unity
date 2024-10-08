/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views.sign_in;

import static io.imunity.vaadin.elements.CSSVars.BIG_MARGIN;
import static io.imunity.vaadin.elements.CSSVars.MEDIUM_MARGIN;
import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Route;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.views.HomeUiMenu;
import io.imunity.home.views.HomeViewComponent;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;

@PermitAll
@Breadcrumb(key = "UserHomeUI.signIn")
@Route(value = "/sign-in", layout = HomeUiMenu.class)
public class SignInView extends HomeViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SignInView.class);
	private final CredentialManagement credMan;
	private final CredentialRequirementManagement credReqMan;
	private final EntityCredentialManagement ecredMan;
	private final EntityManagement entityMan;
	private final CredentialEditorRegistry credEditorReg;
	private final MessageSource msg;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private final NotificationPresenter notificationPresenter;

	SignInView(AdditionalAuthnHandler additionalAuthnHandler, MessageSource msg, CredentialManagement credMan,
					  EntityCredentialManagement ecredMan, EntityManagement entityMan,
					  CredentialRequirementManagement credReqMan,
					  CredentialEditorRegistry credEditorReg, NotificationPresenter notificationPresenter)
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityMan = entityMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		this.notificationPresenter = notificationPresenter;
	}
	
	private void init()
	{
		getContent().removeAll();
		LoginSession theUser = InvocationContext.getCurrent().getLoginSession();
		Boolean disable2ndFactorOptIn = ComponentUtil.getData(UI.getCurrent(), HomeEndpointProperties.class)
				.getBooleanValue(HomeEndpointProperties.DISABLE_2ND_FACTOR_OPT_IN);
		Entity entity = loadEntity(theUser.getEntityId());
		Map<String, CredentialDefinition> credentials = loadCredentials(entity);
		if (credentials.size() == 0)
		{
			getContent().add(new Span(msg.getMessage("CredentialChangeDialog.noCredentials")));
			return;
		}

		Map<String, CredentialPublicInformation> credentialsState = entity.getCredentialInfo()
				.getCredentialsState();

		List<SingleCredentialPanel> outdatedCredentialDefinition = 
				filterCredentials(credentials.values(), credentialsState, LocalCredentialState.outdated)
				.map((CredentialDefinition credDef) -> createPanel(credDef, theUser.getEntityId())).toList();
		List<SingleCredentialPanel> correctCredentialDefinition = 
				filterCredentials(credentials.values(), credentialsState, LocalCredentialState.correct)
				.map((CredentialDefinition credDef) -> createPanel(credDef, theUser.getEntityId())).toList();
		List<SingleCredentialPanel> notSetCredentialDefinition = 
				filterCredentials(credentials.values(), credentialsState, LocalCredentialState.notSet)
				.map((CredentialDefinition credDef) -> createPanel(credDef, theUser.getEntityId())).toList();

		VerticalLayout layout = new VerticalLayout();
		if (!disable2ndFactorOptIn)
			layout.add(create2ndFactorOptInComponent(theUser.getEntityId()));

		if(!outdatedCredentialDefinition.isEmpty())
			layout.add(getDetailsPanel(new H2(msg.getMessage("UserHomeUI.credentialRequiringUpdate")), 
					createPanelLayout(outdatedCredentialDefinition), false));

	
		layout.add(getDetailsPanel(new H2(msg.getMessage("UserHomeUI.signInCredentials")), 
				createPanelLayout(correctCredentialDefinition), true));

		if (!notSetCredentialDefinition.isEmpty())
		{
			layout.add(new HorizontalLayout());
			layout.add(new HorizontalLayout());
			layout.add(new HorizontalLayout());
			layout.add(getDetailsPanel(getH2(msg.getMessage("UserHomeUI.addAnotherSignInCredentials")),
					createPanelLayout(notSetCredentialDefinition), false));
		}
		layout.setSizeUndefined();
		layout.setId("credential-layout");
		getContent().add(layout);
	}

	private Details getDetailsPanel(Component summary, Component content, boolean opened)
	{
		Details details = new Details(summary, content);
		details.getStyle().set("margin-top", SMALL_MARGIN.value());
		details.setWidthFull();
		details.setOpened(opened);
		return details;
	}
	
	private H2 getH2(String title)
	{
		H2 summary = new H2(title);
		summary.getStyle().set("margin", "0");
		return summary;
	}

	private VerticalLayout createPanelLayout(Collection<SingleCredentialPanel> panels)
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.getStyle().set("margin-left", BIG_MARGIN.value());
		layout.getStyle().set("margin-top", MEDIUM_MARGIN.value());

		int last = panels.size();
		int credSize = panels.size();
		for (SingleCredentialPanel panel: panels)
		{
			if (last > 0 && last < credSize)
				layout.add(new Hr());
			layout.add(panel);
			last--;
		}
		return layout;
	}

	private SingleCredentialPanel createPanel(CredentialDefinition credDef, long entityId)
	{
		return new SingleCredentialPanel(additionalAuthnHandler, msg, entityId,
				ecredMan, credMan, entityMan, credEditorReg, credDef, false, notificationPresenter, this::init);
	}

	private Stream<CredentialDefinition> filterCredentials(Collection<CredentialDefinition> credentials,
			Map<String, CredentialPublicInformation> credentialsState,
			LocalCredentialState state)
	{
		return credentials.stream()
				.filter(credential -> credentialsState.get(credential.getName()).getState().equals(state));
	}

	private Checkbox create2ndFactorOptInComponent(long entityId)
	{
		Checkbox userOptInCheckBox = new Checkbox(msg.getMessage("CredentialChangeDialog.userMFAOptin"));
		userOptInCheckBox.setTooltipText(msg.getMessage("CredentialChangeDialog.userMFAOptinDesc"));
		getContent().add(userOptInCheckBox);
		userOptInCheckBox.addValueChangeListener(e -> setUserMFAOptin(e.getValue(), entityId));
		userOptInCheckBox.setValue(getUserOptInAttribute(entityId));
		userOptInCheckBox.getStyle().set("padding-top", "var(--unity-base-padding)");
		return userOptInCheckBox;
	}

	private void setUserMFAOptin(Boolean value, long entityId)
	{
		try
		{
			ecredMan.setUserMFAOptIn(new EntityParam(entityId), value);
		} catch (EngineException e)
		{
			log.warn("Can not set user MFA optin attribute", e);
			throw new InternalException(msg.getMessage(
					"CredentialChangeDialog.cantSetUserMFAOptin"), e);
		}
	}

	private boolean getUserOptInAttribute(long entityId)
	{
		try
		{
			return ecredMan.getUserMFAOptIn(new EntityParam(entityId));
		} catch (EngineException e)
		{
			log.warn("Can not get user MFA optin attribute", e);
			throw new InternalException(msg.getMessage(
					"CredentialChangeDialog.cantGetUserMFAOptin"), e);
		}
	}

	private Map<String, CredentialDefinition> loadCredentials(Entity entity)
	{
		CredentialInfo ci = entity.getCredentialInfo();
		String credReqId = ci.getCredentialRequirementId();
		CredentialRequirements credReq = null;
		Collection<CredentialDefinition> allCreds;
		try
		{
			Collection<CredentialRequirements> allReqs = credReqMan.getCredentialRequirements();
			for (CredentialRequirements cr: allReqs)
				if (credReqId.equals(cr.getName()))
					credReq = cr;

		} catch (Exception e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredReqs"), e);
		}

		if (credReq == null)
		{
			log.fatal("Can not find credential requirement information, for the one set for the entity: "
					+ credReqId);
			throw new InternalException(msg.getMessage("CredentialChangeDialog.noCredReqDef"));
		}

		try
		{
			allCreds = credMan.getCredentialDefinitions();
		} catch (EngineException e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredDefs"), e);
		}
		Set<String> required = credReq.getRequiredCredentials();

		return allCreds.stream()
				.filter(credential -> required.contains(credential.getName()))
				.collect(Collectors.toMap(CredentialDefinition::getName, Function.identity()));
	}

	private Entity loadEntity(long entityId)
	{
		try
		{
			return entityMan.getEntity(new EntityParam(entityId));
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("CredentialChangeDialog.entityRefreshError"),
					e.getMessage());
			throw new InternalException(msg.getMessage("CredentialChangeDialog.getEntityError"), e);
		}
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event)
	{
		init();
	}
}
