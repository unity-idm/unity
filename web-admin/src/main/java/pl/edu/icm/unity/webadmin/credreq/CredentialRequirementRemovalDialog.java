/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.EnumComboBox;

/**
 * Confirmation dialog for credential requirement removal. User must also set the replacement credential requirement
 * and replacement authn state.
 * 
 * @author K. Benedyczak
 */
public class CredentialRequirementRemovalDialog extends AbstractDialog
{
	private static final long serialVersionUID = 1L;
	private Callback callback;
	private ComboBox replacementCR;
	private EnumComboBox<LocalAuthenticationState> updatedState;
	private Collection<CredentialRequirements> allCRs;
	private String removedCr;

	public CredentialRequirementRemovalDialog(UnityMessageSource msg, String removedCr, 
			Collection<CredentialRequirements> allCRs, Callback callback) 
	{
		super(msg, msg.getMessage("CredentialRequirements.removalCaption"));
		this.allCRs = allCRs;
		this.removedCr = removedCr;
		this.callback = callback;
		this.defaultSizeUndfined = true;
	}

	public interface Callback 
	{
		public void onConfirm(String replacementCR, LocalAuthenticationState replacementState);
	}

	@Override
	protected Component getContents() throws WrongArgumentException
	{
		FormLayout vl = new FormLayout();
		vl.setSpacing(true);
		vl.addComponent(new Label(msg.getMessage("CredentialRequirements.removalConfirm", removedCr)));
		
		replacementCR = new ComboBox(msg.getMessage("CredentialRequirements.replacement"));
		List<String> crs = new ArrayList<String>();
		for (CredentialRequirements cr: allCRs)
			if (!cr.getName().equals(removedCr))
				crs.add(cr.getName());
		Collections.sort(crs);
		if (crs.size() == 0)
		{
			ErrorPopup.showError(msg.getMessage("CredentialRequirements.removalError"), 
					msg.getMessage("CredentialRequirements.cantRemoveLast"));
			throw new WrongArgumentException("");
		}
		for (String cr: crs)
			replacementCR.addItem(cr);
		replacementCR.select(crs.get(0));
		replacementCR.setNullSelectionAllowed(false);
		vl.addComponent(replacementCR);
		
		updatedState = new EnumComboBox<LocalAuthenticationState>(
				msg.getMessage("CredentialRequirements.replacementState"), msg,
				"CredentialState.", LocalAuthenticationState.class, 
				LocalAuthenticationState.outdated);
		vl.addComponent(updatedState);
		return vl;
	}

	@Override
	protected void onConfirm()
	{
		close();
		callback.onConfirm((String)replacementCR.getValue(), updatedState.getSelectedValue());
	}
}