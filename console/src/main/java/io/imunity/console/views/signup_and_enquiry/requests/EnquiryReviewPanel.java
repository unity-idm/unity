/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.requests;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;

import io.imunity.console.views.maintenance.audit_log.IdentityFormatter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

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
	
	private final EntityManagement identitiesManagement;
	private final NotificationPresenter notificationPresenter;
	
	private NativeLabel entity;

	@Autowired
	EnquiryReviewPanel(MessageSource msg, AttributeHandlerRegistry handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, EntityManagement identitiesManagement, IdentityFormatter idFormatter,
			GroupsManagement groupMan, PolicyDocumentManagement policyDocMan,
			PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder, NotificationPresenter notificationPresenter)
	{
		super(msg, handlersRegistry, idTypesRegistry, idFormatter, groupMan, policyDocMan,
				policyAgreementRepresentationBuilder);
		this.identitiesManagement = identitiesManagement;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI()
	{
		setMargin(false);
		setPadding(false);

		entity = new NativeLabel();

		FormLayout wrapper = new FormLayout();
		wrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		wrapper.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		wrapper.addFormItem(entity, msg.getMessage("EnquiryReviewPanel.enquirySubmitter"));

		add(wrapper);
		addStandardComponents(msg.getMessage("EnquiryReviewPanel.groupsChanges"));
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
		entity.setText(label);

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
			allUserGroups
					.addAll(identitiesManagement.getGroupsForPresentation(new EntityParam(requestState.getEntityId())));
		} catch (Exception e)
		{
			log.error("Can not establish entities groups", e);
			notificationPresenter.showError("", msg.getMessage("EnquiryReviewPanel.errorEstablishGroups"));
		}

		return super.getGroupEntries(requestState, form, allUserGroups, form.getType()
						.equals(EnquiryType.STICKY));
	}	
}
