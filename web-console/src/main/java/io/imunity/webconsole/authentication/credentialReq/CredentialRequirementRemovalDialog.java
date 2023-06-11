/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.authentication.credentialReq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Confirmation dialog for credential requirement removal. User must also set the replacement credential requirement
 * and replacement authn state.
 * 
 * @author K. Benedyczak
 */
class CredentialRequirementRemovalDialog extends AbstractDialog
{
	private Callback callback;
	private ComboBox<String> replacementCR;
	private Collection<CredentialRequirements> allCRs;
	private HashSet<String> removedCr;

	CredentialRequirementRemovalDialog(MessageSource msg, HashSet<String> removedCr, 
			Collection<CredentialRequirements> allCRs, Callback callback) 
	{
		super(msg, msg.getMessage("CredentialRequirements.removalCaption"));
		this.allCRs = allCRs;
		this.removedCr = removedCr;
		this.callback = callback;
		setSizeEm(40, 20);
	}

	interface Callback 
	{
		public void onConfirm(String replacementCR);
	}

	@Override
	protected Component getContents() throws WrongArgumentException
	{
		FormLayout vl = new CompactFormLayout();
		vl.setSpacing(true);		
		String confirmText = MessageUtils.createConfirmFromStrings(msg, removedCr);
		Label info = new Label(msg.getMessage("CredentialRequirements.removalConfirm", confirmText));
		info.setWidth(100, Unit.PERCENTAGE);
		vl.addComponent(info);
		
		
		List<String> crs = new ArrayList<>();
		for (CredentialRequirements cr: allCRs)
			if (!removedCr.contains(cr.getName()))
				crs.add(cr.getName());
		Collections.sort(crs);
		if (crs.isEmpty())
		{
			NotificationPopup.showError(msg.getMessage("CredentialRequirements.removalError"), 
					msg.getMessage("CredentialRequirements.cantRemoveLast"));
			throw new WrongArgumentException("");
		}
		replacementCR = new ComboBox<>(msg.getMessage("CredentialRequirements.replacement"), crs);
		replacementCR.setSelectedItem(crs.get(0));
		replacementCR.setEmptySelectionAllowed(false);
		vl.addComponent(replacementCR);
		return vl;
	}

	@Override
	protected void onConfirm()
	{
		close();
		callback.onConfirm((String)replacementCR.getValue());
	}
}