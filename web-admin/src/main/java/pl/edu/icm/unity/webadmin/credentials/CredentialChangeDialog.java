/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.MapComboBox;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.VerticalLayout;

/**
 * Allows to change a credential.
 * @author K. Benedyczak
 */
public class CredentialChangeDialog extends AbstractDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialChangeDialog.class);
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private CredentialEditorRegistry credEditorReg;
	private Entity entity;
	private Callback callback;
	private boolean changed = false;
	
	private Map<String, CredentialDefinition> credentials;
	
	private Panel statuses;
	private MapComboBox<CredentialDefinition> credential;
	private TextField type;
	private TextField status;
	private DescriptionTextArea description;
	private Panel editor;
	private Button update;
	private CredentialEditor credEditor;
	
	public CredentialChangeDialog(UnityMessageSource msg, Entity entity, AuthenticationManagement authnMan, 
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg, Callback callback)
	{
		super(msg, msg.getMessage("CredentialChangeDialog.caption"), msg.getMessage("close"));
		this.defaultSizeUndfined = true;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.entity = entity;
		this.credEditorReg = credEditorReg;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		loadCredentials();
		
		statuses = new Panel(msg.getMessage("CredentialChangeDialog.statusAll"));
		
		Panel credentialPanel = new Panel(msg.getMessage("CredentialChangeDialog.credentialPanel"));
		
		credential = new MapComboBox<CredentialDefinition>(msg.getMessage("CredentialChangeDialog.credential"),
				credentials, credentials.keySet().iterator().next());
		credential.setImmediate(true);
		credential.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				updateSelectedCredential();
			}
		});
		type = new TextField(msg.getMessage("CredentialChangeDialog.credType"));
		description = new DescriptionTextArea(msg.getMessage("CredentialChangeDialog.description"), true, "");
		status = new TextField(msg.getMessage("CredentialChangeDialog.status"));
		editor = new Panel(msg.getMessage("CredentialChangeDialog.value"));
		update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				updateCredential();
			}
		});
		FormLayout fl = new FormLayout(type, description, status, editor, update);
		fl.setMargin(true);
		credentialPanel.setContent(fl);
		
		Label spacer = new Label();
		spacer.setHeight(2, Unit.EM);
		VerticalLayout ret = new VerticalLayout(statuses, spacer, credential, credentialPanel);
		ret.setSpacing(true);
		updateStatus();
		updateSelectedCredential();
		return ret;
	}

	@Override
	protected void onConfirm()
	{
		callback.onClose(changed);
		close();
	}
	
	private void updateSelectedCredential()
	{
		CredentialDefinition chosen = credential.getSelectedValue();
		description.setValue(chosen.getDescription());
		type.setReadOnly(false);
		type.setValue(chosen.getTypeId());
		type.setReadOnly(true);
		status.setReadOnly(false);
		Map<String, LocalCredentialState> s = entity.getCredentialInfo().getCredentialsState();
		status.setValue(msg.getMessage("CredentialStatus."+s.get(chosen.getName()).toString()));
		status.setReadOnly(true);
		credEditor = credEditorReg.getEditor(chosen.getTypeId());
		editor.setContent(credEditor.getEditor(chosen.getJsonConfiguration()));
	}
	
	private void updateCredential()
	{
		String secrets;
		try
		{
			secrets = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			return;
		}
		CredentialDefinition credDef = credential.getSelectedValue();
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			idsMan.setEntityCredential(entityP, credDef.getName(), secrets);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			return;
		}
		changed = true;
		try
		{
			entity = idsMan.getEntity(entityP);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialChangeDialog.entityRefreshError"), e);
		}
		
		updateStatus();
	}
	
	
	private void updateStatus()
	{
		FormLayout contents = new FormLayout();
		contents.setMargin(true);
		contents.setSpacing(true);
		
		Map<String, LocalCredentialState> state = entity.getCredentialInfo().getCredentialsState();
		for (Map.Entry<String, LocalCredentialState> s: state.entrySet())
		{
			Label label = new Label(s.getKey());
			if (s.getValue() == LocalCredentialState.correct)
				label.setIcon(Images.ok.getResource());
			else if (s.getValue() == LocalCredentialState.outdated)
				label.setIcon(Images.warn.getResource());
			else
				label.setIcon(Images.error.getResource());
			label.setDescription(msg.getMessage("CredentialStatus."+s.getValue().toString()));
			contents.addComponents(label);
		}
		
		contents.addComponent(new Label("<hr/>", ContentMode.HTML));
		
		String overalStatus = msg.getMessage("AuthenticationState."
				+ entity.getCredentialInfo().getAuthenticationState().toString());
		contents.addComponent(new Label(msg.getMessage("CredentialChangeDialog.overallStatus", overalStatus)));
		
		statuses.setContent(contents);
		updateSelectedCredential();
	}
	
	private void loadCredentials() throws Exception
	{
		CredentialInfo ci = entity.getCredentialInfo();
		String credReqId = ci.getCredentialRequirementId();
		CredentialRequirements credReq = null;
		Collection<CredentialDefinition> allCreds = null;
		try
		{
			Collection<CredentialRequirements> allReqs = authnMan.getCredentialRequirements();
			for (CredentialRequirements cr: allReqs)
				if (credReqId.equals(cr.getName()))
					credReq = cr;
			
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialChangeDialog.cantGetCredReqs"), e);
			throw e;
		}
		
		if (credReq == null)
		{
			ErrorPopup.showError(msg.getMessage("CredentialChangeDialog.noCredReqDef"), "");
			log.fatal("Can not find credential requirement information, for the one set for the entity: " + credReqId);
			throw new IllegalStateException("");
		}
		
		try
		{
			allCreds = authnMan.getCredentialDefinitions();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialChangeDialog.cantGetCredDefs"), e);
			throw e;
		}
		
		credentials = new HashMap<String, CredentialDefinition>();
		Set<String> required = credReq.getRequiredCredentials();
		for (CredentialDefinition credential: allCreds)
		{
			if (required.contains(credential.getName()))
				credentials.put(credential.getName(), credential);
		}
	}
	
	public interface Callback
	{
		public void onClose(boolean changed);
	}
}
