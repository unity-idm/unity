/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.select.Select;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.Collection;

class ChangeCredentialRequirementDialog extends DialogWithActionFooter
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
		super(msg::getMessage);
		this.msg = msg;
		this.eCredMan = eCredMan;
		this.entity = entity;
		this.credReqMan = credReqMan;
		this.callback = callback;
		this.initialCR = initialCR;
		this.notificationPresenter = notificationPresenter;
		setHeaderTitle(msg.getMessage("CredentialRequirementDialog.caption"));
		setActionButton(msg.getMessage("ok"), this::onConfirm);
		setWidth("40em");
		setHeight("18em");
		add(new NativeLabel(msg.getMessage("CredentialRequirementDialog.changeFor", entity)));
		add(getContents());
	}

	private FormLayout getContents()
	{
		credentialRequirement = new Select<>();
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
		main.setWidthFull();
	    main.addFormItem(credentialRequirement,
				msg.getMessage("CredentialRequirementDialog.credReq"));

		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		
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
