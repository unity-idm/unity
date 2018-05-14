/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Allows to change all entity credentials.
 * @author K. Benedyczak
 */
public class CredentialsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialsPanel.class);
	private CredentialManagement credMan;
	private CredentialRequirementManagement credReqMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	private UnityMessageSource msg;
	private Entity entity;
	private final long entityId;
	private final boolean simpleMode;
	
	private Map<String, CredentialDefinition> credentials;
	private List<SingleCredentialPanel> panels;
	
	
	/**
	 * 
	 * @param msg
	 * @param entityId
	 * @param authnMan
	 * @param ecredMan
	 * @param credEditorReg
	 * @param simpleMode if true then admin-only action buttons (credential reset/outdate) are not shown.
	 * @throws Exception
	 */
	public CredentialsPanel(UnityMessageSource msg, long entityId, CredentialManagement credMan, 
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialRequirementManagement credReqMan,
			CredentialEditorRegistry credEditorReg, boolean simpleMode) 
					throws Exception
	{
		this.msg = msg;
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		this.simpleMode = simpleMode;
		init();
	}
	
	
	private void init() throws Exception
	{
		loadCredentials();
		if (credentials.size() == 0)
		{
			addComponent(new Label(
					msg.getMessage("CredentialChangeDialog.noCredentials")));
			return;
		}
		panels = new ArrayList<>();	
		for (CredentialDefinition credDef : credentials.values())
		{
			SingleCredentialPanel panel = new SingleCredentialPanel(msg, entityId,
					ecredMan, credMan, entityMan, credEditorReg, credDef, simpleMode,
					true);
			if (!panel.isEmptyEditor())
			{
				panels.add(panel);
			}
		}
		
		int last = panels.size();
		int credSize = panels.size();
		for (SingleCredentialPanel panel: panels)
		{
			if (last > 0 && last < credSize)
				addComponent(HtmlTag.horizontalLine());
			addComponent(panel);
			last--;
		}
		
		setSizeFull();
	}
		
	public boolean isChanged()
	{	
		for (SingleCredentialPanel panel : panels)
			if (panel.isChanged())
				return true;
		
		return false;
	}
	
	public boolean isCredentialRequirementEmpty()
	{
		return credentials.isEmpty();
	}	
	
	private void loadCredentials() throws Exception
	{
		try
		{
			entity = entityMan.getEntity(new EntityParam(entityId));
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
			Collection<CredentialRequirements> allReqs = credReqMan.getCredentialRequirements();
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
			allCreds = credMan.getCredentialDefinitions();
		} catch (EngineException e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredDefs"), e);
		}
		
		credentials = new HashMap<>();
		Set<String> required = credReq.getRequiredCredentials();
		for (CredentialDefinition credential: allCreds)
		{
			if (required.contains(credential.getName()))
				credentials.put(credential.getName(), credential);
		}
	}
}
