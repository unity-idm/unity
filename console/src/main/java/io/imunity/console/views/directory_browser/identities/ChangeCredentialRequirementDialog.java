/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;

import java.util.Collection;

class ChangeCredentialRequirementDialog extends ConfirmDialog
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final EntityCredentialManagement eCredMan;
	private final CredentialRequirementManagement credReqMan;
	private final EntityWithLabel entity;
	private final String initialCR;
	private final Callback callback;
	
	private Select<String> credentialRequirement;
	
	ChangeCredentialRequirementDialog(MessageSource msg, EntityWithLabel entity, String initialCR,
			EntityCredentialManagement eCredMan, CredentialRequirementManagement credReqMan, 
			Callback callback, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.eCredMan = eCredMan;
		this.entity = entity;
		this.credReqMan = credReqMan;
		this.callback = callback;
		this.initialCR = initialCR;
		this.notificationPresenter = notificationPresenter;
		setHeader(msg.getMessage("CredentialRequirementDialog.caption"));
		setCancelable(true);
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setWidth("40em");
		setHeight("18em");
		add(getContents());
	}

	private FormLayout getContents()
	{
		credentialRequirement = new Select<>();
		credentialRequirement.setLabel(msg.getMessage("CredentialRequirementDialog.changeFor", entity));
		credentialRequirement.setWidthFull();
		Collection<CredentialRequirements> credReqs;
		try
		{
			credReqs = credReqMan.getCredentialRequirements();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetcredReq"));
			throw new IllegalStateException();
		}
		if (credReqs.isEmpty())
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.credReqMissing"));
			throw new IllegalStateException();
		}
		
		credentialRequirement.setItems(credReqs.stream().map(DescribedObjectROImpl::getName).toList());
		credentialRequirement.setValue(initialCR);

		FormLayout main = new FormLayout();
		FormLayout.FormItem formItem = main.addFormItem(credentialRequirement,
				msg.getMessage("CredentialRequirementDialog.credReq"));
		formItem.getStyle().set("align-items", "center");
		formItem.getStyle().set("--vaadin-form-item-label-width", "9em");
		return main;
	}

	private void onConfirm()
	{
		EntityParam entityParam = new EntityParam(entity.getEntity().getId());
		try
		{
			eCredMan.setEntityCredentialRequirements(entityParam,
					credentialRequirement.getValue());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirementDialog.changeError"), e.getMessage());
			return;
		}
		
		callback.onChanged();
		close();
	}
	
	interface Callback 
	{
		void onChanged();
	}
}