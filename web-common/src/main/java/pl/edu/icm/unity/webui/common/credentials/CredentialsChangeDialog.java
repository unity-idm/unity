/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;


/**
 * Allows to change a credential.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class CredentialsChangeDialog extends AbstractDialog
{
	private CredentialManagement credMan;
	private CredentialRequirementManagement credReqMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	
	private Callback callback;
	private long entityId;
	private boolean simpleMode;
	private CredentialsPanel ui;
	
	@Autowired
	public CredentialsChangeDialog(UnityMessageSource msg, CredentialManagement credMan, 
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialRequirementManagement credReqMan, CredentialEditorRegistry credEditorReg)
	{
		super(msg, msg.getMessage("CredentialChangeDialog.caption"), msg.getMessage("close"));
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityMan = entityMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		setSize(50, 80);
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
			ui = new CredentialsPanel(msg, entityId, credMan, ecredMan, entityMan, credReqMan,
					credEditorReg, simpleMode);
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
