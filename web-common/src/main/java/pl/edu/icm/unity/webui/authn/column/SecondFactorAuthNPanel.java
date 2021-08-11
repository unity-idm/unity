/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * The login component used for the 2nd factor authentication 
 * 
 * @author K. Benedyczak
 */
public class SecondFactorAuthNPanel extends AuthNPanelBase implements AuthenticationUIController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SecondFactorAuthNPanel.class);
	private final MessageSource msg;
	private final EntityManagement idsMan;
	private final Runnable switchToFirstFactor;

	SecondFactorAuthNPanel(MessageSource msg,
			EntityManagement idsMan,
			VaadinAuthenticationUI secondaryUI, PartialAuthnState partialState,
			AuthenticationOptionKey optionId, Runnable switchToFirstFactor)
	{
		super(secondaryUI, optionId, new VerticalLayout());
		this.msg = msg;
		this.idsMan = idsMan;
		this.switchToFirstFactor = switchToFirstFactor;

		authenticatorContainer.setHeight(100, Unit.PERCENTAGE);
		authenticatorContainer.setWidth(100, Unit.PERCENTAGE);
		authenticatorContainer.setSpacing(true);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addStyleName("u-authn-component");
		setCompositionRoot(authenticatorContainer);
		setAuthenticator(secondaryUI, partialState);
	}

	private void setAuthenticator(VaadinAuthenticationUI secondaryUI, PartialAuthnState partialState)
	{
		secondaryUI.clear();
		
		Component retrievalComponent = authnUI.getComponent();
		authenticatorContainer.addComponent(retrievalComponent);
		
		try
		{
			secondaryUI.presetEntity(resolveEntity(partialState.getPrimaryResult()));
		} catch (EngineException e)
		{
			log.error("Can't resolve the first authenticated entity", e);
		}
		
		
		Button resetMfaButton = new Button(msg.getMessage("AuthenticationUI.resetMfaButton"));
		resetMfaButton.setDescription(msg.getMessage("AuthenticationUI.resetMfaButtonDesc"));
		resetMfaButton.addStyleName(Styles.vButtonLink.toString());
		resetMfaButton.addStyleName("u-authn-resetMFAButton");
		resetMfaButton.addClickListener(event -> switchToFirstFactor.run());
		authenticatorContainer.addComponent(resetMfaButton);
		authenticatorContainer.setComponentAlignment(resetMfaButton, Alignment.TOP_RIGHT);
	}

	private Entity resolveEntity(AuthenticationResult unresolved) throws EngineException
	{
		AuthenticatedEntity ae = unresolved.getSuccessResult().authenticatedEntity;
		return idsMan.getEntity(new EntityParam(ae.getEntityId()));
	}
}
