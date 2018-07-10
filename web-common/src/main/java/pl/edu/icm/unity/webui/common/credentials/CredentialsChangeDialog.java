/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;


/**
 * Allows to change all entity credentials.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class CredentialsChangeDialog extends AbstractDialog
{
	private CredentialManagement credMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	private CredentialRequirementManagement credReqMan;
	private AuthenticationFlowManagement authnFlowMan;
	private TokensManagement tokenMan;
	
	private Callback callback;
	private long entityId;
	private boolean simpleMode;
	private CredentialsPanel ui;
	private AdditionalAuthnHandler additionalAuthnHandler;
	
	@Autowired
	public CredentialsChangeDialog(AdditionalAuthnHandler additionalAuthnHandler, UnityMessageSource msg, CredentialManagement credMan, 
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialRequirementManagement credReqMan,AuthenticationFlowManagement authnFlowMan, 
			CredentialEditorRegistry credEditorReg, TokensManagement tokenMan)
	{
		super(msg, msg.getMessage("CredentialChangeDialog.caption"), msg.getMessage("close"));
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.ecredMan = ecredMan;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.credReqMan = credReqMan;
		this.credMan = credMan;
		this.authnFlowMan = authnFlowMan;
		this.tokenMan = tokenMan;
		setSizeEm(45, 55);
	}

	public CredentialsChangeDialog init(long entityId, boolean simpleMode, Callback callback)
	{
		this.entityId = entityId;
		this.callback = callback;
		this.simpleMode = simpleMode;
		return this;
	}
	
	@Override
	protected Component getContents() throws Exception
	{
		try
		{
			ui = new CredentialsPanel(additionalAuthnHandler, msg,  entityId,  credMan, 
					 ecredMan,  entityMan,
					 credReqMan,
					 credEditorReg, authnFlowMan, tokenMan, simpleMode);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"), e);
			throw e;
		}
		
		return ui;
	}

	@Override
	protected void onConfirm()
	{
		callback.onClose(ui.isChanged());
		close();
	}
	
	public interface Callback
	{
		public void onClose(boolean changed);
	}
}
