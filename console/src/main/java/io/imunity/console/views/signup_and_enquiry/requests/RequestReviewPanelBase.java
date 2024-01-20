/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.requests;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.console.views.maintenance.audit_log.IdentityFormatter;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.Panel;
import io.imunity.vaadin.endpoint.common.consent_utils.ListOfSelectableElements;
import io.imunity.vaadin.endpoint.common.consent_utils.ListOfSelectableElements.DisableMode;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ListOfElementsWithActions;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseRegistrationInput;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.Selection;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.registration.GroupDiffUtils;
import pl.edu.icm.unity.engine.api.registration.RequestedGroupDiff;

/**
 * Shows request contents and provides a possibility to edit it. Base for
 * extending by request type specific components.
 * 
 * @author K. Benedyczak
 */
class RequestReviewPanelBase extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RequestReviewPanelBase.class);
	private static final int MAX_TEXT_LENGHT = 300;
	
	protected final MessageSource msg;
	private final AttributeHandlerRegistry handlersRegistry;
	private final IdentityTypesRegistry idTypesRegistry;
	private final GroupsManagement groupMan;
	private final PolicyDocumentManagement policyDocMan;
	private final PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;

	private UserRequestState<?> requestState;
	private ListOfSelectableElements attributes;
	private ListOfSelectableElements groups;
	private ListOfElementsWithActions<String> identities;
	private ListOfElementsWithActions<String> optins;
	private ListOfElementsWithActions<String> policyAgreements;
	private NativeLabel comment;
	private Panel attributesPanel;
	private Panel groupsPanel;
	private Panel optinsP;
	private Panel policyAgreementsP;
	private Panel commentP;
	private Panel identitiesP;
	private IdentityFormatter idFormatter;

	RequestReviewPanelBase(MessageSource msg, AttributeHandlerRegistry handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, IdentityFormatter idFormatter, GroupsManagement groupMan,
			PolicyDocumentManagement policyDocMan,
			PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder)
	{
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.idTypesRegistry = idTypesRegistry;
		this.idFormatter = idFormatter;
		this.groupMan = groupMan;
		this.policyDocMan = policyDocMan;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
	}

	protected void addStandardComponents(String groupsTitle)
	{
		identities = new ListOfElementsWithActions<>(new ListOfElementsWithActions.LabelConverter<String>()
		{
			@Override
			public Html toLabel(String value)
			{
				return new Html("<div>" +  value + "</div>");
			}
		});
		identities.setAddSeparatorLine(false);
		identities.setPadding(true);
		identitiesP = new Panel(msg.getMessage("RequestReviewPanel.requestedIdentities"));
		identitiesP.setWidthFull();
		identitiesP.setMargin(false);
		identitiesP.add(identities);

		attributes = new ListOfSelectableElements(null,
				getIgnoreLabel(new NativeLabel(msg.getMessage("RequestReviewPanel.requestedAttributeIgnore"))),
				DisableMode.WHEN_SELECTED);
		attributes.setWidth(100, Unit.PERCENTAGE);
		attributesPanel = new Panel(msg.getMessage("RequestReviewPanel.requestedAttributes"));
		attributesPanel.add(attributes);
		attributesPanel.setWidthFull();
		attributesPanel.setMargin(false);
		attributes.setPadding(true);

		groups = new ListOfSelectableElements(null,
				getIgnoreLabel(new NativeLabel(msg.getMessage("RequestReviewPanel.requestedGroupsIgnore"))),
				DisableMode.WHEN_SELECTED);
		groupsPanel = new Panel(groupsTitle);
		groupsPanel.add(groups);
		groupsPanel.setWidthFull();
		groupsPanel.setMargin(false);
		groups.setPadding(true);

		optins = new ListOfElementsWithActions<>(new ListOfElementsWithActions.LabelConverter<String>()
		{
			@Override
			public NativeLabel toLabel(String value)
			{
				return new NativeLabel(value);
			}
		});
		optins.setAddSeparatorLine(false);
		optins.setPadding(true);
		optinsP = new Panel(msg.getMessage("RequestReviewPanel.optins"));
		optinsP.add(optins);
		optinsP.setWidthFull();
		optinsP.setMargin(false);

		policyAgreements = new ListOfElementsWithActions<>(new ListOfElementsWithActions.LabelConverter<String>()
		{
			@Override
			public Html toLabel(String value)
			{
				return new Html("<div>" +  value + "</div>");
			}
		});
		policyAgreements.setPadding(true);
		policyAgreements.setAddSeparatorLine(false);
		policyAgreementsP = new Panel(msg.getMessage("RequestReviewPanel.agreements"));
		policyAgreementsP.add(policyAgreements);
		policyAgreementsP.setWidthFull();
		policyAgreementsP.setMargin(false);

		comment = new NativeLabel();
		commentP = new Panel(msg.getMessage("RequestReviewPanel.comment"));
		commentP.add(comment);
		commentP.setWidthFull();
		commentP.setMargin(false);

		add(identitiesP, attributesPanel, groupsPanel, commentP, optinsP, policyAgreementsP);
	}

	HorizontalLayout getIgnoreLabel(NativeLabel label)
	{
		HorizontalLayout layout = new HorizontalLayout(label);
		layout.setMargin(false);
		layout.setSpacing(false);
		layout.setPadding(false);
		layout.setWidthFull();
		layout.setJustifyContentMode(JustifyContentMode.END);
		return layout;
	}

	protected void fillRequest(BaseRegistrationInput ret)
	{
		BaseRegistrationInput orig = requestState.getRequest();
		ret.setAgreements(orig.getAgreements());
		ret.setComments(orig.getComments());
		ret.setCredentials(orig.getCredentials());
		ret.setFormId(orig.getFormId());
		ret.setIdentities(orig.getIdentities());
		ret.setUserLocale(orig.getUserLocale());
		ret.setPolicyAgreements(orig.getPolicyAgreements());
		ret.setGroupSelections(new ArrayList<>(orig.getGroupSelections()
				.size()));
		for (int i = 0; i < orig.getGroupSelections()
				.size(); i++)
		{
			GroupSelection origSelection = orig.getGroupSelections()
					.get(i);
			boolean ignore = groups.getSelection()
					.size() > i && groups.getSelection()
							.get(i)
							.getValue();
			ret.getGroupSelections()
					.add(ignore ? null : origSelection);
		}

		ret.setAttributes(new ArrayList<Attribute>(attributes.getSelection()
				.size()));
		for (int i = 0, j = 0; i < orig.getAttributes()
				.size(); i++)
		{
			if (orig.getAttributes()
					.get(i) == null)
				ret.getAttributes()
						.add(null);
			else if (!attributes.getSelection()
					.get(j++)
					.getValue())
				ret.getAttributes()
						.add(orig.getAttributes()
								.get(i));
			else
				ret.getAttributes()
						.add(null);
		}
	}

	protected void setInput(UserRequestState<?> requestState, BaseForm form)
	{
		this.requestState = requestState;
		BaseRegistrationInput request = requestState.getRequest();
		String comments = request.getComments();
		if (comments != null && !comments.equals(""))
		{
			commentP.setVisible(true);
			comment.setText(comments);
		} else
			commentP.setVisible(false);

		identities.clearContents();
		for (IdentityParam idParam : request.getIdentities())
		{
			if (idParam == null)
				continue;
			try
			{
				identities.addEntry(idFormatter.toString(idParam, idTypesRegistry.getByName(idParam.getTypeId())));
			} catch (Exception e)
			{
				throw new IllegalStateException("Ups, have request in DB with unsupported id type.", e);
			}
		}
		identitiesP.setVisible(!identities.getElements()
				.isEmpty());

		optins.clearContents();
		for (int i = 0; i < request.getAgreements()
				.size(); i++)
		{
			Selection selection = request.getAgreements()
					.get(i);
			if (form.getAgreements()
					.size() <= i)
				break;
			AgreementRegistrationParam agreementText = form.getAgreements()
					.get(i);
			String info = (selection.isSelected()) ? msg.getMessage("RequestReviewPanel.accepted")
					: msg.getMessage("RequestReviewPanel.notAccepted");
			String agreementTextStr = agreementText.getText()
					.getValue(msg);
			String aText = (agreementTextStr.length() > MAX_TEXT_LENGHT)
					? agreementTextStr.substring(0, MAX_TEXT_LENGHT) + "[...]"
					: agreementTextStr;
			optins.addEntry(info + ": " + aText);
		}
		optinsP.setVisible(!optins.getElements()
				.isEmpty());
		
		policyAgreements.clearContents();
		for (int i = 0; i < request.getPolicyAgreements()
				.size(); i++)
		{
			PolicyAgreementDecision policyAgreementDecision = request.getPolicyAgreements()
					.get(i);
			if (form.getPolicyAgreements()
					.size() <= i)
				break;
			PolicyAgreementConfiguration policyAgreementConfig = form.getPolicyAgreements()
					.get(i);
			String info = policyAgreementDecision == null ? msg.getMessage("RequestReviewPanel.skipped")
					: "[" + msg.getMessage(
							"PolicyAgreementAcceptanceStatus." + policyAgreementDecision.acceptanceStatus.toString())
							+ "]";
			String agreementDocs = "[" + String.join(", ", policyAgreementConfig.documentsIdsToAccept.stream()
					.map(d -> getPolicyDocName(d))
					.collect(Collectors.toList())) + "]";
			String agreementText = policyAgreementRepresentationBuilder
					.getAgreementRepresentationText(policyAgreementConfig);
			String agreementTextWithDoc = agreementText + " " + agreementDocs;
			String aText = (agreementTextWithDoc.length() > MAX_TEXT_LENGHT)
					? agreementTextWithDoc.substring(0, MAX_TEXT_LENGHT) + "[...]"
					: agreementTextWithDoc;
			policyAgreements.addEntry(info + ": " + aText);
		}
		policyAgreementsP.setVisible(!policyAgreements.getElements()
				.isEmpty());
		attributes.clearEntries();
		for (Attribute ap : request.getAttributes())
		{
			if (ap == null)
				continue;
			VerticalLayout rep = handlersRegistry.getRepresentation(ap, AttributeViewerContext.builder()
					.withImageScaleHeight(500)
					.withImageScaleWidth(500)
					.withCustomWidth(100)
					.withCustomWidthUnit(Unit.PERCENTAGE)
					.build());
			rep.setPadding(false);
			rep.setMargin(false);
			attributes.addEntry(rep, false);
		}
		attributes.setCheckBoxesVisible(requestState.getStatus() == RegistrationRequestStatus.pending);
		attributes.setHeadersVisible(requestState.getStatus() == RegistrationRequestStatus.pending);

		attributesPanel.setVisible(!attributes.isEmpty());
	}

	protected void setGroupEntries(List<Component> list)
	{
		groups.clearEntries();
		for (Component c : list)
		{
			groups.addEntry(c, false);
		}
		groups.setCheckBoxesVisible(requestState.getStatus() == RegistrationRequestStatus.pending);
		groups.setHeadersVisible(requestState.getStatus() == RegistrationRequestStatus.pending);
		groupsPanel.setVisible(!groups.isEmpty());
	}

	private String getGroupDisplayedName(GroupsManagement groupMan, String path)
	{
		try
		{
			return groupMan.getContents(path, GroupContents.METADATA)
					.getGroup()
					.getDisplayedName()
					.getValue(msg);
		} catch (Exception e)
		{
			log.warn("Can not get group displayed name for group " + path);
			return path;
		}
	}

	protected List<Component> getGroupEntries(UserRequestState<?> requestState, BaseForm form,
			List<Group> allUserGroups, boolean showRemoved)
	{
		List<Component> groupEntries = new ArrayList<>();
		BaseRegistrationInput request = requestState.getRequest();

		for (int i = 0; i < request.getGroupSelections()
				.size(); i++)
		{
			GroupSelection selection = request.getGroupSelections()
					.get(i);
			if (form.getGroupParams()
					.size() <= i)
				break;
			HorizontalLayout wrapper = new HorizontalLayout();
			wrapper.setSpacing(false);
			wrapper.setMargin(false);
			if (selection.getExternalIdp() != null)
				wrapper.add(new NativeLabel("[from: " + selection.getExternalIdp() + "]"));
			wrapper.add(requestState.getStatus() == RegistrationRequestStatus.pending ? getSingleGroupEntryComponent(GroupDiffUtils.getSingleGroupDiff(
					groupMan.getGroupsByWildcard("/**"), allUserGroups, selection, form.getGroupParams()
							.get(i)),
					showRemoved) : getSingleGroupEntryComponent(selection));
			groupEntries.add(wrapper);
		}
		return groupEntries;
	}

	private Component getSingleGroupEntryComponent(GroupSelection selection)
	{
		HorizontalLayout main = new HorizontalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		addGroupLabel(main, selection.getSelectedGroups().stream().collect(Collectors.toSet()), null);
		return main;
	}
	
	private Component getSingleGroupEntryComponent(RequestedGroupDiff diff, boolean showRemoved)
	{
		HorizontalLayout main = new HorizontalLayout();
		main.setSpacing(true);
		main.setMargin(false);

		addGroupLabel(main, diff.toAdd, CssClassNames.SUCCESS.getName());

		if (showRemoved)
		{
			addGroupLabel(main, diff.toRemove, CssClassNames.ERROR.getName());
		}

		addGroupLabel(main, diff.remain, CssClassNames.BOLD.getName());

		return main;
	}

	private void addGroupLabel(HorizontalLayout layout, Set<String> value, String style)
	{
		if (value == null || value.isEmpty())
			return;
		NativeLabel l = new NativeLabel(value.stream()
				.sorted()
				.map(g -> getGroupDisplayedName(groupMan, g))
				.collect(Collectors.toList())
				.toString());
		if (style != null)
			l.addClassName(style);
		layout.add(l);
	}

	private String getPolicyDocName(Long id)
	{
		try
		{
			return policyDocMan.getPolicyDocument(id).name;
		} catch (Exception e)
		{
			log.warn("Can not get policy document name for id " + id);
			return String.valueOf(id);
		}
	}

}
