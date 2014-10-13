/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.MapComboBox;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.VerticalLayout;

/**
 * Allows to change a credential.
 * @author K. Benedyczak
 */
public class CredentialsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialsPanel.class);
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private CredentialEditorRegistry credEditorReg;
	private UnityMessageSource msg;
	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private final boolean simpleMode;
	private boolean askAboutCurrent;
	
	private Map<String, CredentialDefinition> credentials;
	
	private Panel statuses;
	private MapComboBox<CredentialDefinition> credential;
	private TextField type;
	private TextField status;
	private DescriptionTextArea description;
	private Panel credentialStateInfo;
	private Panel editor;
	private Button update;
	private Button clear;
	private Button invalidate;
	private CredentialEditor credEditor;
	
	/**
	 * 
	 * @param msg
	 * @param entityId
	 * @param authnMan
	 * @param idsMan
	 * @param credEditorReg
	 * @param simpleMode if true then admin-only action buttons (credential reset/outdate) are not shown.
	 * @throws Exception
	 */
	public CredentialsPanel(UnityMessageSource msg, long entityId, AuthenticationManagement authnMan, 
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg, boolean simpleMode) 
					throws Exception
	{
		this.msg = msg;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.entityId = entityId;
		this.credEditorReg = credEditorReg;
		this.simpleMode = simpleMode;
		init();
	}

	private void init() throws Exception
	{
		loadCredentials();
		
		statuses = new Panel(msg.getMessage("CredentialChangeDialog.statusAll"));
		
		Panel credentialPanel = new Panel();
		
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
		credentialStateInfo = new Panel(msg.getMessage("CredentialChangeDialog.credentialStateInfo"));
		editor = new Panel(msg.getMessage("CredentialChangeDialog.value"));
		
		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setSpacing(true);
		clear = new Button(msg.getMessage("CredentialChangeDialog.clear"));
		clear.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				changeCredentialStatus(LocalCredentialState.notSet);
			}
		});
		if (!simpleMode)
			buttonsBar.addComponent(clear);
		invalidate = new Button(msg.getMessage("CredentialChangeDialog.invalidate"));
		invalidate.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				changeCredentialStatus(LocalCredentialState.outdated);
			}
		});
		if (!simpleMode)
			buttonsBar.addComponent(invalidate);
		update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				updateCredential();
			}
		});
		buttonsBar.addComponent(update);
		
		FormLayout fl = new FormLayout(type, description, status, credentialStateInfo, editor, buttonsBar);
		fl.setMargin(true);
		credentialPanel.setContent(fl);
		
		Label spacer = new Label();
		spacer.setHeight(2, Unit.EM);
		addComponents(statuses, spacer);
		if (credentials.size() > 1)
			addComponent(credential);
		else
			addComponent(new Label(msg.getMessage("CredentialChangeDialog.credentialSingle", 
					credentials.values().iterator().next().getName())));
		addComponent(credentialPanel);
		setSpacing(true);
		updateStatus();
		updateSelectedCredential();
	}

	public boolean isChanged()
	{
		return changed;
	}
	
	private void updateSelectedCredential()
	{
		CredentialDefinition chosen = credential.getSelectedValue();
		description.setValue(chosen.getDescription());
		type.setReadOnly(false);
		type.setValue(chosen.getTypeId());
		type.setReadOnly(true);
		status.setReadOnly(false);
		Map<String, CredentialPublicInformation> s = entity.getCredentialInfo().getCredentialsState();
		CredentialPublicInformation credPublicInfo = s.get(chosen.getName());
		status.setValue(msg.getMessage("CredentialStatus."+credPublicInfo.getState().toString()));
		status.setReadOnly(true);
		credEditor = credEditorReg.getEditor(chosen.getTypeId());
		FormLayout credLayout = new FormLayout();
		credLayout.setMargin(true);
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			askAboutCurrent = idsMan.isCurrentCredentialRequiredForChange(entityP, 
					chosen.getTypeId());
		} catch (EngineException e)
		{
			log.debug("Got exception when asking about possibility to "
					+ "change the credential without providing the existing one."
					+ " Most probably the subsequent credential change will also fail.", e);
			askAboutCurrent = true;
		}
		credLayout.addComponents(credEditor.getEditor(askAboutCurrent, chosen.getJsonConfiguration(), true).getComponents());
		editor.setContent(credLayout);
		Component viewer = credEditor.getViewer(credPublicInfo.getExtraInformation());
		if (viewer == null)
		{
			credentialStateInfo.setVisible(false);
		} else
		{
			credentialStateInfo.setContent(viewer);
			credentialStateInfo.setVisible(true);
		}
		if (credPublicInfo.getState() == LocalCredentialState.notSet)
		{
			clear.setEnabled(false);
			invalidate.setEnabled(false);
		} else if (credPublicInfo.getState() == LocalCredentialState.outdated)
		{
			clear.setEnabled(true);
			invalidate.setEnabled(false);
		} else
		{
			clear.setEnabled(true);
			invalidate.setEnabled(true);
		}
	}
	
	private void updateCredential()
	{
		String secrets, currentSecrets = null;
		try
		{
			if (askAboutCurrent)
				currentSecrets = credEditor.getCurrentValue();
			secrets = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			return;
		}
		CredentialDefinition credDef = credential.getSelectedValue();
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			if (askAboutCurrent)
				idsMan.setEntityCredential(entityP, credDef.getName(), secrets, currentSecrets);
			else
				idsMan.setEntityCredential(entityP, credDef.getName(), secrets);
		} catch (IllegalPreviousCredentialException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			credEditor.setCredentialError(null);
			credEditor.setPreviousCredentialError(e.getMessage());
			return;
		}  catch (IllegalCredentialException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			credEditor.setPreviousCredentialError(null);
			credEditor.setCredentialError(e.getMessage());
			return;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			return;
		}
		changed = true;
		loadEntity(entityP);
		updateStatus();
	}

	private void changeCredentialStatus(LocalCredentialState desiredState)
	{
		CredentialDefinition credDef = credential.getSelectedValue();
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			idsMan.setEntityCredentialStatus(entityP, credDef.getName(), desiredState);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
			return;
		}
		changed = true;
		loadEntity(entityP);
		updateStatus();
	}

	
	private void loadEntity(EntityParam entityP)
	{
		try
		{
			entity = idsMan.getEntity(entityP);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("CredentialChangeDialog.entityRefreshError"), e);
		}
	}
	
	private void updateStatus()
	{
		FormLayout contents = new FormLayout();
		contents.setMargin(true);
		contents.setSpacing(true);
		
		Map<String, CredentialPublicInformation> state = entity.getCredentialInfo().getCredentialsState();
		for (Map.Entry<String, CredentialPublicInformation> s: state.entrySet())
		{
			Label label = new Label(s.getKey());
			if (s.getValue().getState() == LocalCredentialState.correct)
				label.setIcon(Images.ok.getResource());
			else if (s.getValue().getState() == LocalCredentialState.outdated)
				label.setIcon(Images.warn.getResource());
			else
				label.setIcon(Images.error.getResource());
			label.setDescription(msg.getMessage("CredentialStatus."+
				s.getValue().getState().toString()));
			contents.addComponents(label);
		}
		
		contents.addComponent(new Label("<hr/>", ContentMode.HTML));
		
		statuses.setContent(contents);
		updateSelectedCredential();
	}
	
	private void loadCredentials() throws Exception
	{
		try
		{
			entity = idsMan.getEntity(new EntityParam(entityId));
		} catch (Exception e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.getEntityError"), e);
		}
		
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
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredReqs"), e);
		}
		
		if (credReq == null)
		{
			log.fatal("Can not find credential requirement information, for the one set for the entity: " 
					+ credReqId);
			throw new InternalException(msg.getMessage("CredentialChangeDialog.noCredReqDef"));
		}
		
		try
		{
			allCreds = authnMan.getCredentialDefinitions();
		} catch (EngineException e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredDefs"), e);
		}
		
		credentials = new HashMap<String, CredentialDefinition>();
		Set<String> required = credReq.getRequiredCredentials();
		for (CredentialDefinition credential: allCreds)
		{
			if (required.contains(credential.getName()))
				credentials.put(credential.getName(), credential);
		}
		if (credentials.size() == 0)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.noCredentials"));
		}
	}
}
