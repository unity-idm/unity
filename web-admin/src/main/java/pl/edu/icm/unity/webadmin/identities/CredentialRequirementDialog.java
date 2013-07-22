/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;

/**
 * Allows to change credential requirement for an entity
 * @author K. Benedyczak
 */
public class CredentialRequirementDialog extends AbstractDialog
{
	private IdentitiesManagement identitiesMan;
	private AuthenticationManagement authnMan;
	private final long entityId;
	private final String initialCR;
	protected Callback callback;
	
	private ComboBox credentialRequirement;
	
	public CredentialRequirementDialog(UnityMessageSource msg, long entityId, String initialCR,
			IdentitiesManagement identitiesMan, AuthenticationManagement authnMan, Callback callback)
	{
		super(msg, msg.getMessage("CredentialRequirementDialog.caption"));
		this.identitiesMan = identitiesMan;
		this.entityId = entityId;
		this.authnMan = authnMan;
		this.callback = callback;
		this.initialCR = initialCR;
		this.defaultSizeUndfined = true;
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("CredentialRequirementDialog.changeFor", entityId));
		credentialRequirement = new ComboBox(msg.getMessage("CredentialRequirementDialog.credReq"));
		Collection<CredentialRequirements> credReqs;
		try
		{
			credReqs = authnMan.getCredentialRequirements();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.cantGetcredReq"));
			throw new IllegalStateException();
		}
		if (credReqs.isEmpty())
		{
			ErrorPopup.showError(msg.getMessage("error"),
					msg.getMessage("EntityCreation.credReqMissing"));
			throw new IllegalStateException();
		}
		for (CredentialRequirements cr: credReqs)
		{
			credentialRequirement.addItem(cr.getName());
		}
		credentialRequirement.select(initialCR);
		credentialRequirement.setNullSelectionAllowed(false);
		
		FormLayout main = new FormLayout();
		main.addComponents(info, credentialRequirement);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		EntityParam entity = new EntityParam(entityId);
		try
		{
			identitiesMan.setEntityCredentialRequirements(entity, 
					(String)credentialRequirement.getValue());
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialRequirementDialog.changeError"), e);
			return;
		}
		
		callback.onChanged();
		close();
	}
	
	public interface Callback 
	{
		public void onChanged();
	}
}
