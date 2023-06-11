/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.Collection;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Allows to change credential requirement for an entity
 * @author K. Benedyczak
 */
class ChangeCredentialRequirementDialog extends AbstractDialog
{
	private EntityCredentialManagement eCredMan;
	private CredentialRequirementManagement credReqMan;
	private final EntityWithLabel entity;
	private final String initialCR;
	protected Callback callback;
	
	private ComboBox<String> credentialRequirement;
	
	ChangeCredentialRequirementDialog(MessageSource msg, EntityWithLabel entity, String initialCR,
			EntityCredentialManagement eCredMan, CredentialRequirementManagement credReqMan, 
			Callback callback)
	{
		super(msg, msg.getMessage("CredentialRequirementDialog.caption"));
		this.eCredMan = eCredMan;
		this.entity = entity;
		this.credReqMan = credReqMan;
		this.callback = callback;
		this.initialCR = initialCR;
		setSizeEm(38, 18);
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("CredentialRequirementDialog.changeFor", entity));
		info.setWidth(100, Unit.PERCENTAGE);
		credentialRequirement = new ComboBox<>(msg.getMessage("CredentialRequirementDialog.credReq"));
		Collection<CredentialRequirements> credReqs;
		try
		{
			credReqs = credReqMan.getCredentialRequirements();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetcredReq"));
			throw new IllegalStateException();
		}
		if (credReqs.isEmpty())
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.credReqMissing"));
			throw new IllegalStateException();
		}
		
		credentialRequirement.setItems(credReqs.stream().map(cr -> cr.getName()));
		credentialRequirement.setSelectedItem(initialCR);
		credentialRequirement.setEmptySelectionAllowed(false);
		
		FormLayout main = new CompactFormLayout();
		main.addComponents(info, credentialRequirement);
		return main;
	}

	@Override
	protected void onConfirm()
	{
		EntityParam entityParam = new EntityParam(entity.getEntity().getId());
		try
		{
			eCredMan.setEntityCredentialRequirements(entityParam, 
					(String)credentialRequirement.getValue());
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirementDialog.changeError"), e);
			return;
		}
		
		callback.onChanged();
		close();
	}
	
	interface Callback 
	{
		public void onChanged();
	}
}
