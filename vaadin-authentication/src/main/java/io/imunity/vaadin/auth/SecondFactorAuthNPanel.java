/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;

/**
 * The login component used for the 2nd factor authentication
 */
public class SecondFactorAuthNPanel extends AuthNPanelBase implements AuthenticationUIController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SecondFactorAuthNPanel.class);
	private final MessageSource msg;
	private final EntityManagement idsMan;
	private final Runnable switchToFirstFactor;

	SecondFactorAuthNPanel(MessageSource msg,
	                       EntityManagement idsMan,
	                       VaadinAuthentication.VaadinAuthenticationUI secondaryUI, PartialAuthnState partialState,
	                       AuthenticationOptionKey optionId, Runnable switchToFirstFactor)
	{
		super(secondaryUI, optionId, new VerticalLayout());
		this.msg = msg;
		this.idsMan = idsMan;
		this.switchToFirstFactor = switchToFirstFactor;

		authenticatorContainer.setHeightFull();
		authenticatorContainer.setWidthFull();
		authenticatorContainer.setPadding(true);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addClassName("u-authn-component");
		add(authenticatorContainer);
		setAuthenticator(secondaryUI, partialState);
	}

	private void setAuthenticator(VaadinAuthentication.VaadinAuthenticationUI secondaryUI, PartialAuthnState partialState)
	{
		secondaryUI.clear();
		
		Component retrievalComponent = authnUI.getComponent();
		authenticatorContainer.add(retrievalComponent);
		
		try
		{
			secondaryUI.presetEntity(resolveEntity(partialState.getPrimaryResult()));
		} catch (EngineException e)
		{
			log.error("Can't resolve the first authenticated entity", e);
		}
		
		
		LinkButton resetMfaButton = new LinkButton(
				msg.getMessage("AuthenticationUI.resetMfaButton"),
				event -> switchToFirstFactor.run()
		);
		resetMfaButton.getElement().setProperty("title", msg.getMessage("AuthenticationUI.resetMfaButtonDesc"));
		resetMfaButton.addClassName("u-authn-resetMFAButton");
		authenticatorContainer.add(resetMfaButton);
		authenticatorContainer.setAlignItems(FlexComponent.Alignment.END);
	}

	private Entity resolveEntity(AuthenticationResult unresolved) throws EngineException
	{
		AuthenticatedEntity ae = unresolved.getSuccessResult().authenticatedEntity;
		return idsMan.getEntity(new EntityParam(ae.getEntityId()));
	}
}
