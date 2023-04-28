/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.outdated;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;

import java.util.Collection;


/**
 * Controls and creates UI to change outdated credential.
 */
@PrototypeComponent
public class OutdatedCredentialController
{
	private CredentialManagement credMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private VaadinLogoImageLoader imageAccessService;
	
	private long entityId;
	private String credentialId;
	private CredentialChangePanel ui;
	private CredentialChangeConfiguration uiConfig;
	private MessageSource msg;
	private VaddinWebLogoutHandler authnProcessor;
	private Runnable finishHandler;
	private NotificationPresenter notificationPresenter;

	@Autowired
	public OutdatedCredentialController(AdditionalAuthnHandler additionalAuthnHandler,
	                                    MessageSource msg, CredentialManagement credMan,
	                                    EntityCredentialManagement ecredMan, EntityManagement entityMan,
	                                    CredentialRequirementManagement credReqMan,
	                                    CredentialEditorRegistry credEditorReg, VaadinLogoImageLoader imageAccessService,
	                                    NotificationPresenter notificationPresenter)
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
	}

	public void init(CredentialChangeConfiguration uiConfig, VaddinWebLogoutHandler authnProcessor,
	                 Runnable finishHandler)
	{
		this.authnProcessor = authnProcessor;
		this.finishHandler = finishHandler;
		WrappedSession vss = VaadinSession.getCurrent().getSession();
		LoginSession ls = (LoginSession) vss.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		this.entityId = ls.getEntityId();
		this.credentialId = ls.getOutdatedCredentialId();
		this.uiConfig = uiConfig;
	}

	public Component getComponent()
	{
		CredentialDefinition credDef = getCredentialDefinition();
		VerticalLayout master = new VerticalLayout();
		master.setMargin(false);
		master.setSizeFull();
		ui = new CredentialChangePanel(msg, entityId, imageAccessService, ecredMan, entityMan,
					credEditorReg, credDef, additionalAuthnHandler, uiConfig,
					() -> afterCredentialUpdate(true),
					() -> afterCredentialUpdate(false), notificationPresenter);
		master.add(ui);
		master.setAlignItems(FlexComponent.Alignment.CENTER);
		
		Focusable toFocus = ui.getFocussedComponent();
		if (toFocus != null)
			toFocus.focus();
		return master;
	}
	
	private CredentialDefinition getCredentialDefinition()
	{
		try
		{
			Collection<CredentialDefinition> allCreds = credMan
					.getCredentialDefinitions();
			CredentialDefinition credDef = null;
			for (CredentialDefinition cd : allCreds)
			{
				if (cd.getName().equals(credentialId))
					credDef = cd;
			}
			if (credDef == null)
				throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredDefs")
						+ credentialId);
			return credDef;
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not load credentials", e);
		}
	}

	private void afterCredentialUpdate(boolean changed)
	{
		ui.setEnabled(false);
		if (changed)
		{
			notificationPresenter.showWarning(msg.getMessage("OutdatedCredentialDialog.finalOK"),
					msg.getMessage("OutdatedCredentialDialog.finalInfo"));
		} else
		{
			notificationPresenter.showWarning(msg.getMessage("OutdatedCredentialDialog.finalError"),
					msg.getMessage("OutdatedCredentialDialog.finalInfoNotChanged"));
		}
	}
	
	private void cleanup()
	{
		finishHandler.run();
		authnProcessor.logout(true);
	}
}
