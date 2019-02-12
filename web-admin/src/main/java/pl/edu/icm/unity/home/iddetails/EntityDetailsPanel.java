/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.common.identities.MembershipFormatter;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

/**
 * Presents a complete and comprehensive information about a single entity. No editing is possible.
 * Targeted for admin user.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class EntityDetailsPanel extends FormLayout
{
	private UnityMessageSource msg;
	private IdentityFormatter idFormatter;
	private Label id;
	private Label status;
	private Label scheduledAction;
	private ListOfElementsWithActions<String> identities;
	private Label credReq;
	private HtmlLabel credStatus;
	private ListOfElementsWithActions<String> groups;
	
	@Autowired
	public EntityDetailsPanel(UnityMessageSource msg, IdentityFormatter idFormatter)
	{
		this.msg = msg;
		this.idFormatter = idFormatter;
		id = new Label();
		id.setCaption(msg.getMessage("IdentityDetails.id"));

		status = new Label();
		status.setCaption(msg.getMessage("IdentityDetails.status"));
		
		scheduledAction = new Label();
		scheduledAction.setCaption(msg.getMessage("IdentityDetails.expiration"));
		
		identities = new ListOfElementsWithActions<>(this::labelConverter);
		identities.setAddSeparatorLine(false);
		identities.setCaption(msg.getMessage("IdentityDetails.identities"));

		credReq = new Label();
		credReq.setCaption(msg.getMessage("IdentityDetails.credReq"));

		credStatus = new HtmlLabel(msg);
		credStatus.setCaption(msg.getMessage("IdentityDetails.credStatus"));

		groups = new ListOfElementsWithActions<>(this::labelConverter); 
		groups.setAddSeparatorLine(false);
		groups.setCaption(msg.getMessage("IdentityDetails.groups"));
		
		addComponents(id, status, scheduledAction, identities, credReq, credStatus, groups);
	}
	
	private Label labelConverter(String value) 
	{
		Label ret = new HtmlSimplifiedLabel(value);
		ret.setWidth(100, Unit.PERCENTAGE);
		return ret;
	}
	
	public void setInput(EntityWithLabel entityWithLabel, Collection<GroupMembership> groups)
	{
		id.setValue(entityWithLabel.toString());
		Entity entity = entityWithLabel.getEntity();
		
		status.setValue(msg.getMessage("EntityState." + entity.getState().toString()));
		
		EntityScheduledOperation operation = entity.getEntityInformation().getScheduledOperation();
		if (operation != null)
		{
			scheduledAction.setVisible(true);
			String action = msg.getMessage("EntityScheduledOperationWithDate." + operation.toString(), 
					entity.getEntityInformation().getScheduledOperationTime());
			scheduledAction.setValue(action);
		} else
		{
			scheduledAction.setVisible(false);
		}
		
		identities.clearContents();
		for (Identity id: entity.getIdentities())
			identities.addEntry(idFormatter.toString(id));
		
		CredentialInfo credInf = entity.getCredentialInfo();
		credReq.setValue(credInf.getCredentialRequirementId());
		
		credStatus.resetValue();
		for (Map.Entry<String, CredentialPublicInformation> cred: credInf.getCredentialsState().entrySet())
		{
			String status = msg.getMessage("CredentialStatus." + 
					cred.getValue().getState().toString());
			credStatus.addHtmlValueLine("IdentityDetails.credStatusValue", cred.getKey(), status);
		}
		credStatus.setVisible(!credInf.getCredentialsState().entrySet().isEmpty());
			
		
		this.groups.clearContents();
		for (GroupMembership groupM: groups)
			this.groups.addEntry(MembershipFormatter.toString(msg, groupM));
	}
}
