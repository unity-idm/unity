/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.outdated;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.VerticalLayout;

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
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistryV8;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;


/**
 * Controls and creates UI to change outdated credential.
 *  
 * @author K. Benedyczak
 */
@PrototypeComponent
public class OutdatedCredentialController
{
	private CredentialManagement credMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistryV8 credEditorReg;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private ImageAccessService imageAccessService;
	
	private long entityId;
	private String credentialId;
	private CredentialChangePanel ui;
	private CredentialChangeConfiguration uiConfig;
	private MessageSource msg;
	private StandardWebLogoutHandler authnProcessor;
	private Runnable finishHandler;
	
	@Autowired
	public OutdatedCredentialController(AdditionalAuthnHandler additionalAuthnHandler,
	                                    MessageSource msg, CredentialManagement credMan,
	                                    EntityCredentialManagement ecredMan, EntityManagement entityMan,
	                                    CredentialRequirementManagement credReqMan,
	                                    CredentialEditorRegistryV8 credEditorReg, ImageAccessService imageAccessService)
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.imageAccessService = imageAccessService;
	}

	public void init(CredentialChangeConfiguration uiConfig, StandardWebLogoutHandler authnProcessor,
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
					() -> afterCredentialUpdate(false));
		master.addComponent(ui);
		master.setComponentAlignment(ui, Alignment.MIDDLE_CENTER);
		
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
			NotificationPopup.showWarningAutoClosing(msg.getMessage("OutdatedCredentialDialog.finalOK"), 
					msg.getMessage("OutdatedCredentialDialog.finalInfo"), this::cleanup);
		} else
		{
			NotificationPopup.showWarningAutoClosing(msg.getMessage("OutdatedCredentialDialog.finalError"), 
					msg.getMessage("OutdatedCredentialDialog.finalInfoNotChanged"), this::cleanup);
		}
	}
	
	private void cleanup()
	{
		finishHandler.run();
		authnProcessor.logout(true);
	}
}
