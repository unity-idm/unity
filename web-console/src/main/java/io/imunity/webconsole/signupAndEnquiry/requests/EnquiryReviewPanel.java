/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.requests;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilderV8;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows enquiry response contents and provides a possibility to edit it.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
class EnquiryReviewPanel extends RequestReviewPanelBase
{
	private static final String UNKNOWN_IDENTITY = "DELETED USER";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryReviewPanel.class);
	private Label entity;
	private EntityManagement identitiesManagement;
	
	@Autowired
	EnquiryReviewPanel(MessageSource msg, AttributeHandlerRegistryV8 handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, EntityManagement identitiesManagement, 
			IdentityFormatter idFormatter, GroupsManagement groupMan, PolicyDocumentManagement policyDocMan,
			PolicyAgreementRepresentationBuilderV8 policyAgreementRepresentationBuilder)
	{
		super(msg, handlersRegistry, idTypesRegistry, idFormatter, groupMan, policyDocMan, policyAgreementRepresentationBuilder);
		this.identitiesManagement = identitiesManagement;
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(new MarginInfo(true, false));
		entity = new Label();
		entity.setCaption(msg.getMessage("EnquiryReviewPanel.enquirySubmitter"));
		main.addComponent(entity);
		super.addStandardComponents(main,  msg.getMessage("EnquiryReviewPanel.groupsChanges"));
		setCompositionRoot(main);	
	}
	
	EnquiryResponse getUpdatedRequest()
	{
		EnquiryResponse ret = new EnquiryResponse();
		super.fillRequest(ret);
		return ret;
	}
	
	void setInput(EnquiryResponseState requestState, EnquiryForm form)
	{
		super.setInput(requestState, form);
		boolean unknwonIdentity = false;
		try
		{
			identitiesManagement.getEntity(new EntityParam(requestState.getEntityId()));
		} catch (Exception e)
		{
			log.warn("Can not establish entity ", e);
			unknwonIdentity = true;
		}
		
		String label = unknwonIdentity ? UNKNOWN_IDENTITY : getEntityLabel(requestState.getEntityId());	
		label += " [" + requestState.getEntityId() + "]";
		entity.setValue(label);
		
		if (!unknwonIdentity)
		{
			setGroupEntries(getGroupEntries(requestState, form));
		}
	}
	
	private String getEntityLabel(long entity)
	{
		String label = null;
		try
		{
			label = identitiesManagement.getEntityLabel(new EntityParam(entity));
		} catch (Exception e)
		{
			log.warn("Can not establish entity label", e);
		}
		if (label == null)
			label = "";
		return label;
	}
	
	private List<Component> getGroupEntries(EnquiryResponseState requestState, EnquiryForm form)
	{
		List<Group> allUserGroups = new ArrayList<>();
		try
		{
			allUserGroups.addAll(identitiesManagement
					.getGroupsForPresentation(new EntityParam(requestState.getEntityId())));
		} catch (Exception e)
		{
			log.error("Can not establish entities groups", e);
			NotificationPopup.showError(msg, msg.getMessage("EnquiryReviewPanel.errorEstablishGroups"), e);
		}

		return super.getGroupEntries(requestState, form, allUserGroups, form.getType().equals(EnquiryType.STICKY));
	}
}



