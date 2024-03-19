/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities.credentials;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
public class CredentialsChangeDialog extends DialogWithActionFooter
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final CredentialManagement credMan;
	private final EntityCredentialManagement ecredMan;
	private final EntityManagement entityMan;
	private final CredentialEditorRegistry credEditorReg;
	private final CredentialRequirementManagement credReqMan;
	private final TokensManagement tokenMan;
	private final AdditionalAuthnHandler additionalAuthnHandler;

	private Callback callback;
	private long entityId;
	private boolean simpleMode;
	private CredentialsPanel ui;

	CredentialsChangeDialog(AdditionalAuthnHandler additionalAuthnHandler, MessageSource msg,
			CredentialManagement credMan,
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialRequirementManagement credReqMan,
			CredentialEditorRegistry credEditorReg, TokensManagement tokenMan,
			NotificationPresenter notificationPresenter)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.ecredMan = ecredMan;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.credReqMan = credReqMan;
		this.credMan = credMan;
		this.tokenMan = tokenMan;
		this.notificationPresenter = notificationPresenter;
		setWidth("62em");
		setHeight("55em");
		setActionButton(msg.getMessage("close"), this::onConfirm);
		setCancelButtonVisible(false);
		setHeaderTitle(msg.getMessage("CredentialChangeDialog.caption"));
	}

	public CredentialsChangeDialog init(long entityId, boolean simpleMode, Callback callback)
	{
		this.entityId = entityId;
		this.callback = callback;
		this.simpleMode = simpleMode;
		add(getContents());
		return this;
	}

	private Component getContents()
	{
		ui = new CredentialsPanel(additionalAuthnHandler, msg, entityId, credMan,
				ecredMan, entityMan,
				credReqMan,
				credEditorReg, tokenMan, simpleMode, false, notificationPresenter);
		return ui;
	}

	private void onConfirm()
	{
		callback.onClose(ui.isChanged());
		close();
	}

	public interface Callback
	{
		void onClose(boolean changed);
	}
}
