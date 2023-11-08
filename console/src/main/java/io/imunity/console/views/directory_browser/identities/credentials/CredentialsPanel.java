/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities.credentials;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
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
import pl.edu.icm.unity.engine.api.token.TokensManagement;

import java.util.*;


public class CredentialsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialsPanel.class);

	private final CredentialManagement credMan;
	private final CredentialRequirementManagement credReqMan;
	private final EntityCredentialManagement ecredMan;
	private final EntityManagement entityMan;
	private final CredentialEditorRegistry credEditorReg;
	private final TokensManagement tokenMan;
	private final MessageSource msg;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private final NotificationPresenter notificationPresenter;
	private final long entityId;
	private final boolean enableAdminActions;
	private final boolean disable2ndFactorOptIn;

	private Map<String, CredentialDefinition> credentials;
	private List<SingleCredentialPanel> panels;


	CredentialsPanel(AdditionalAuthnHandler additionalAuthnHandler, MessageSource msg, long entityId,
			CredentialManagement credMan,
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialRequirementManagement credReqMan,
			CredentialEditorRegistry credEditorReg, TokensManagement tokenMan,
			boolean disableAdminActions, boolean disable2ndFactorOptIn, NotificationPresenter notificationPresenter)

	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		this.disable2ndFactorOptIn = disable2ndFactorOptIn;
		this.enableAdminActions = !disableAdminActions;
		this.tokenMan = tokenMan;
		this.notificationPresenter = notificationPresenter;
		init();
	}

	private void init()
	{
		if (!disable2ndFactorOptIn)
			add2ndFactorOptInCompnent();

		loadCredentials();
		if (credentials.isEmpty())
		{
			add(new Span(msg.getMessage("CredentialChangeDialog.noCredentials")));
			return;
		}
		panels = new ArrayList<>();

		for (CredentialDefinition credDef : credentials.values())
		{
			SingleCredentialPanel panel = new SingleCredentialPanel(additionalAuthnHandler, msg, entityId,
					ecredMan, credMan, entityMan, credEditorReg, credDef, enableAdminActions, notificationPresenter);
			if (!panel.isEmptyEditor())
			{
				panels.add(panel);
			}
		}

		int last = panels.size();
		int credSize = panels.size();
		for (SingleCredentialPanel panel : panels)
		{
			if (last > 0 && last < credSize)
				add(new Hr());
			add(panel);
			last--;
		}

		add(new Hr());
		add(getTrustedDevicesComponent());

		setSizeFull();
	}

	private void add2ndFactorOptInCompnent()
	{
		Checkbox userOptInCheckBox = new Checkbox(msg.getMessage("CredentialChangeDialog.userMFAOptin"));
		userOptInCheckBox.setTooltipText(msg.getMessage("CredentialChangeDialog.userMFAOptinDesc"));
		FormLayout wrapper = new FormLayout();
		wrapper.add(userOptInCheckBox);
		add(wrapper);
		add(new Hr());
		userOptInCheckBox.addValueChangeListener(e -> setUserMFAOptin(e.getValue()));
		userOptInCheckBox.setValue(getUserOptInAttribute());
	}

	private Component getTrustedDevicesComponent()
	{
		TrustedDevicesComponent trustedDevicesComponent = new TrustedDevicesComponent(tokenMan, msg, entityId,
				notificationPresenter);
		trustedDevicesComponent.setVisible(false);
		VerticalLayout trustedDevicesWrapper = new VerticalLayout();
		trustedDevicesWrapper.setMargin(false);
		trustedDevicesWrapper.setSpacing(true);

		Button removeTrustedMachines = new Button(msg.getMessage("CredentialChangeDialog.removeTrustedDevices"));
		removeTrustedMachines.addClickListener(e -> trustedDevicesComponent.removeAll());

		Div showHideTrustedMachines = new Div();
		showHideTrustedMachines.addClickListener(e ->
		{
			showHideTrustedMachines.removeAll();
			if (trustedDevicesComponent.isVisible())
			{
				trustedDevicesComponent.setVisible(false);
				showHideTrustedMachines.add(VaadinIcon.ANGLE_DOWN.create());
			} else
			{
				trustedDevicesComponent.setVisible(true);
				showHideTrustedMachines.add(VaadinIcon.ANGLE_UP.create());
			}
		});
		showHideTrustedMachines.add(VaadinIcon.ANGLE_DOWN.create());
		Span showInfo = new Span(msg.getMessage("TrustedDevicesComponent.caption"));
		HorizontalLayout showHide = new HorizontalLayout();
		showHide.setMargin(false);
		showHide.add(showInfo, showHideTrustedMachines);


		trustedDevicesWrapper.add(removeTrustedMachines, showHide, trustedDevicesComponent);
		return trustedDevicesWrapper;
	}

	private void setUserMFAOptin(Boolean value)
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

	private boolean getUserOptInAttribute()
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


	public boolean isChanged()
	{
		for (SingleCredentialPanel panel : panels)
			if (panel.isChanged())
				return true;

		return false;
	}

	private void loadCredentials()
	{
		Entity entity;
		try
		{
			entity = entityMan.getEntity(new EntityParam(entityId));
		} catch (Exception e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.getEntityError"), e);
		}

		CredentialInfo ci = entity.getCredentialInfo();
		String credReqId = ci.getCredentialRequirementId();
		CredentialRequirements credReq = null;
		Collection<CredentialDefinition> allCreds = null;
		try
		{
			Collection<CredentialRequirements> allReqs = credReqMan.getCredentialRequirements();
			for (CredentialRequirements cr : allReqs)
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

		credentials = new HashMap<>();
		Set<String> required = credReq.getRequiredCredentials();
		for (CredentialDefinition credential : allCreds)
		{
			if (required.contains(credential.getName()))
				credentials.put(credential.getName(), credential);
		}
	}

	public interface Callback
	{
		void refresh();
	}

}
