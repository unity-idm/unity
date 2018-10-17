/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.credreset.password;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 3rd step of credential reset pipeline. In this dialog user can choose verification method - email or mobile. 
 * @author P.Piernik
 *
 */
public class PasswordCredentialResetVerificationChoose3Dialog extends AbstractDialog
{	
	private enum VerificationMethod {Email, Mobile}
	
	private ComboBox<VerificationMethod> choose;
	private UnityMessageSource msg;
	private CredentialReset backend;
	private String username;
	private CredentialEditor credEditor;
	
	public PasswordCredentialResetVerificationChoose3Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor, 
			String username)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("continue"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.username = username;
		this.credEditor = credEditor;
		setSizeEm(40, 30);
	}

	@Override
	protected Component getContents() throws Exception
	{
		addStyleName("u-credreset-dialog");

		choose = new ComboBox<>();
		choose.setCaption(msg.getMessage("CredentialReset.chooseVerificationMethod"));
		choose.setItems(VerificationMethod.values());
		choose.setValue(VerificationMethod.Email);
		choose.setEmptySelectionAllowed(false);	
		choose.setItemCaptionGenerator(i -> msg.getMessage("CredentialReset." + i.toString()));
		
		VerticalLayout ret = new VerticalLayout(choose);
		ret.setMargin(false);
		ret.setComponentAlignment(choose,  Alignment.TOP_CENTER);
		return ret;
	}

	@Override
	protected void onConfirm()
	{
		CredentialResetStateVariable.inc();
		close();
		
		if (choose.getValue().equals(VerificationMethod.Email))
		{
			EmailCodePasswordCredentialReset4Dialog dialog4 = new EmailCodePasswordCredentialReset4Dialog(msg, backend, credEditor, username);
			dialog4.show();
		} else
		{
			//go to mobile
			CredentialResetStateVariable.inc();
			MobileCodePasswordCredentialReset5Dialog dialog5 = new MobileCodePasswordCredentialReset5Dialog(msg, backend, credEditor, username);
			dialog5.show();
		}	
	}
	
	@Override
	protected void onCancel()
	{
		CredentialResetStateVariable.reset();
		super.onCancel();
	}
}
