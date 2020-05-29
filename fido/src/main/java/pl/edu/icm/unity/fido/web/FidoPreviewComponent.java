/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.fido.web;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import static pl.edu.icm.unity.fido.web.FidoCredentialInfoWrapper.CredentialState.DELETED;
import static pl.edu.icm.unity.fido.web.FidoCredentialInfoWrapper.CredentialState.NEW;

/**
 * Displays information about single FidoCredential public key.
 *
 * @author R. Ledzinski
 */
class FidoPreviewComponent extends CustomComponent
{
	private final UnityMessageSource msg;
	private final FidoCredentialInfoWrapper credential;

	public FidoPreviewComponent(final UnityMessageSource msg,
								final FidoCredentialInfoWrapper credential,
								final Runnable deleteCallback)
	{
		super();
		this.msg = msg;
		this.credential = credential;

		HorizontalLayout rootLayout = new HorizontalLayout();
		FormLayout mainForm = getMainForm();
		VerticalLayout statusLayout = getStatusLayout(deleteCallback);

		rootLayout.setMargin(false);
		rootLayout.setSpacing(false);
		rootLayout.addComponents(mainForm, statusLayout);
		rootLayout.setWidth("100%");

		setCompositionRoot(rootLayout);
	}

	private FormLayout getMainForm()
	{
		Label attestationType = new Label();
		attestationType.setCaption(msg.getMessage("Fido.attestationType"));
		attestationType.setValue(msg.getMessage("Fido." + credential.getCredential().getAttestationFormat()));

		Label creationTime = new Label();
		creationTime.setCaption(msg.getMessage("Fido.created"));
		creationTime.setValue(credential.getRegistrationTimestamp().toString());

		Label authenticatorInfo = new Label();
		authenticatorInfo.setCaption(msg.getMessage("Fido.authenticatorInfo"));
		authenticatorInfo.setValue("-"); // FIXME get some useful information from attestation data

		if (credential.getState() == DELETED)
		{
			attestationType.addStyleName("u-deletedElement");
			creationTime.addStyleName("u-deletedElement");
			authenticatorInfo.addStyleName("u-deletedElement");
		}

		return new CompactFormLayout(attestationType, creationTime, authenticatorInfo);
	}

	private VerticalLayout getStatusLayout(final Runnable deleteCallback)
	{
		VerticalLayout statusLayout = new VerticalLayout();
		statusLayout.setMargin(false);
		statusLayout.setSpacing(false);

		Label credState = new Label();
		if (credential.getState() == NEW)
		{
			credState.setValue("*");
			credState.addStyleName("u-newElement");
		}

		boolean isDeleted = credential.getState() == DELETED;
		Button button = new Button();
		button.setDescription(isDeleted ? msg.getMessage("Fido.restoreDesc") : msg.getMessage("Fido.deleteDesc"));
		button.setIcon(isDeleted ? Images.recycle.getResource() : Images.delete.getResource());
		button.addStyleName(Styles.vButtonLink.toString());
		button.addStyleName(Styles.toolbarButton.toString());
		button.addClickListener(event ->
		{
			credential.setState(isDeleted ? NEW : DELETED);
			deleteCallback.run();
		});

		statusLayout.addComponents(credState, button);
		statusLayout.setComponentAlignment(credState, Alignment.TOP_RIGHT);
		statusLayout.setComponentAlignment(button, Alignment.BOTTOM_RIGHT);
		statusLayout.setExpandRatio(credState, 1.0f);
		statusLayout.setHeight("100%");
		statusLayout.setWidth("100%");

		return statusLayout;
	}
}
