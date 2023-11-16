/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.maintenance.audit_log.IdentityFormatter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ListOfElementsWithActions;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.identities.MembershipFormatter;

import java.util.Collection;
import java.util.Map;

@PrototypeComponent
class EntityDetailsPanel extends FormLayout
{
	private final MessageSource msg;
	private final IdentityFormatter idFormatter;
	private final Span id;
	private final Span status;
	private final Span scheduledAction;
	private final FormItem scheduledActionFormItem;
	private final ListOfElementsWithActions<String> identities;
	private final Span credReq;
	private final Html credStatus;
	private final ListOfElementsWithActions<String> groups;
	
	EntityDetailsPanel(MessageSource msg, IdentityFormatter idFormatter)
	{
		this.msg = msg;
		this.idFormatter = idFormatter;
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		id = new Span();
		addFormItem(id, msg.getMessage("IdentityDetails.id"));

		status = new Span();
		addFormItem(status, msg.getMessage("IdentityDetails.status"));
		
		scheduledAction = new Span();
		scheduledActionFormItem = addFormItem(scheduledAction, msg.getMessage("IdentityDetails.expiration"));

		identities = new ListOfElementsWithActions<>(this::labelConverter);
		identities.setAddSeparatorLine(false);
		addFormItem(identities, msg.getMessage("IdentityDetails.identities"));

		credReq = new Span();
		addFormItem(credReq, msg.getMessage("IdentityDetails.credReq"));

		credStatus = new Html("<div></div>");
		addFormItem(credStatus, msg.getMessage("IdentityDetails.credStatus"));

		groups = new ListOfElementsWithActions<>(this::labelConverter); 
		groups.setAddSeparatorLine(false);
		addFormItem(groups, msg.getMessage("IdentityDetails.groups"));
	}
	
	private Html labelConverter(String value)
	{
		return new Html("<div>" + value + "</div>");
	}
	
	void setInput(EntityWithLabel entityWithLabel, Collection<GroupMembership> groups)
	{
		id.setText(entityWithLabel.toString());
		Entity entity = entityWithLabel.getEntity();
		
		status.setText(msg.getMessage("EntityState." + entity.getState().toString()));
		
		EntityScheduledOperation operation = entity.getEntityInformation().getScheduledOperation();
		if (operation != null)
		{
			scheduledActionFormItem.setVisible(true);
			String action = msg.getMessage("EntityScheduledOperationWithDate." + operation,
					entity.getEntityInformation().getScheduledOperationTime());
			scheduledAction.setText(action);
		} else
		{
			scheduledActionFormItem.setVisible(false);
		}
		
		identities.clearContents();
		for (Identity id: entity.getIdentities())
			identities.addEntry(idFormatter.toString(id));
		
		CredentialInfo credInf = entity.getCredentialInfo();
		credReq.setText(credInf.getCredentialRequirementId());
		
		credStatus.setHtmlContent("<div></div>");
		for (Map.Entry<String, CredentialPublicInformation> cred: credInf.getCredentialsState().entrySet())
		{
			String status = msg.getMessage("CredentialStatus." + 
					cred.getValue().getState().toString());
			credStatus.setHtmlContent("<div>" + msg.getMessage("IdentityDetails.credStatusValue", cred.getKey(), status) + "</div>");
		}
		credStatus.setVisible(!credInf.getCredentialsState().entrySet().isEmpty());
			
		
		this.groups.clearContents();
		for (GroupMembership groupM: groups)
			this.groups.addEntry(MembershipFormatter.toString(msg, groupM));
	}
}
