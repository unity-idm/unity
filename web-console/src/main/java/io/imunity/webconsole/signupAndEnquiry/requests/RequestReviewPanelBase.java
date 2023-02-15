/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.requests;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.registration.GroupDiffUtils;
import pl.edu.icm.unity.engine.api.registration.RequestedGroupDiff;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements.DisableMode;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilderV8;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Shows request contents and provides a possibility to edit it. Base for
 * extending by request type specific components.
 * 
 * @author K. Benedyczak
 */
class RequestReviewPanelBase extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RequestReviewPanelBase.class);
	private static final int MAX_TEXT_LENGHT = 300;
	
	protected MessageSource msg;
	private AttributeHandlerRegistryV8 handlersRegistry;
	private UserRequestState<?> requestState;
	private IdentityTypesRegistry idTypesRegistry;
	private GroupsManagement groupMan;
	private PolicyDocumentManagement policyDocMan;
	private PolicyAgreementRepresentationBuilderV8 policyAgreementRepresentationBuilder;

	private ListOfSelectableElements attributes;
	private ListOfSelectableElements groups;
	private ListOfElementsWithActions<String> identities;
	private ListOfElementsWithActions<String> optins;
	private ListOfElementsWithActions<String> policyAgreements;
	private Label comment;
	private Panel attributesPanel;
	private Panel groupsPanel;
	private Panel optinsP;
	private Panel policyAgreementsP;
	private Panel commentP;
	private Panel identitiesP;
	private IdentityFormatter idFormatter;

	RequestReviewPanelBase(MessageSource msg, AttributeHandlerRegistryV8 handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, IdentityFormatter idFormatter, GroupsManagement groupMan,
			PolicyDocumentManagement policyDocMan,
			PolicyAgreementRepresentationBuilderV8 policyAgreementRepresentationBuilder)
	{
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.idTypesRegistry = idTypesRegistry;
		this.idFormatter = idFormatter;
		this.groupMan = groupMan;
		this.policyDocMan = policyDocMan;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
	}

	protected void addStandardComponents(Layout main, String groupsTitle)
	{
		identities = new ListOfElementsWithActions<>(new ListOfElementsWithActions.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new HtmlSimplifiedLabel(value);
			}
		});
		identities.setAddSeparatorLine(false);
		identitiesP = new SafePanel(msg.getMessage("RequestReviewPanel.requestedIdentities"), identities);

		attributes = new ListOfSelectableElements(null,
				new Label(msg.getMessage("RequestReviewPanel.requestedAttributeIgnore")),
				DisableMode.WHEN_SELECTED);
		attributes.setWidth(100, Unit.PERCENTAGE);
		attributesPanel = new SafePanel(msg.getMessage("RequestReviewPanel.requestedAttributes"),
				new VerticalLayout(attributes));

		groups = new ListOfSelectableElements(null,
				new Label(msg.getMessage("RequestReviewPanel.requestedGroupsIgnore")),
				DisableMode.WHEN_SELECTED);
		groupsPanel = new SafePanel(groupsTitle, new VerticalLayout(groups));

		optins = new ListOfElementsWithActions<>(new ListOfElementsWithActions.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		optins.setAddSeparatorLine(false);
		optinsP = new SafePanel(msg.getMessage("RequestReviewPanel.optins"), optins);

		policyAgreements = new ListOfElementsWithActions<>(
				new ListOfElementsWithActions.LabelConverter<String>()
				{
					@Override
					public Label toLabel(String value)
					{
						Label l = new Label();
						l.setContentMode(ContentMode.HTML);
						l.setValue(value);
						return l;
						
					}
				});
		policyAgreements.setAddSeparatorLine(false);
		policyAgreementsP = new SafePanel(msg.getMessage("RequestReviewPanel.agreements"), policyAgreements);

		comment = new Label();
		comment.addStyleName(Styles.margin.toString());
		commentP = new SafePanel(msg.getMessage("RequestReviewPanel.comment"), comment);

		main.addComponents(identitiesP, attributesPanel, groupsPanel, commentP, optinsP, policyAgreementsP);
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
		ret.setGroupSelections(new ArrayList<>(orig.getGroupSelections().size()));
		for (int i = 0; i < orig.getGroupSelections().size(); i++)
		{
			GroupSelection origSelection = orig.getGroupSelections().get(i);
			boolean ignore = groups.getSelection().size() > i && groups.getSelection().get(i).getValue();
			ret.getGroupSelections().add(ignore ? null : origSelection);
		}

		ret.setAttributes(new ArrayList<Attribute>(attributes.getSelection().size()));
		for (int i = 0, j = 0; i < orig.getAttributes().size(); i++)
		{
			if (orig.getAttributes().get(i) == null)
				ret.getAttributes().add(null);
			else if (!attributes.getSelection().get(j++).getValue())
				ret.getAttributes().add(orig.getAttributes().get(i));
			else
				ret.getAttributes().add(null);
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
			comment.setValue(comments);
		} else
			commentP.setVisible(false);
		
		identities.clearContents();
		for (IdentityParam idParam: request.getIdentities())
		{
			if (idParam == null)
				continue;
			try
			{
				identities.addEntry(idFormatter.toString(idParam, 
						idTypesRegistry.getByName(idParam.getTypeId())));
			} catch (Exception e)
			{
				throw new IllegalStateException("Ups, have request in DB with unsupported id type.", e);
			}
		}
		identitiesP.setVisible(identities.getComponentCount() > 0);
		
		optins.clearContents();
		for (int i=0; i<request.getAgreements().size(); i++)
		{
			Selection selection = request.getAgreements().get(i);
			if (form.getAgreements().size() <= i)
				break;
			AgreementRegistrationParam agreementText = form.getAgreements().get(i);
			String info = (selection.isSelected()) ? msg.getMessage("RequestReviewPanel.accepted") : 
				msg.getMessage("RequestReviewPanel.notAccepted");
			String agreementTextStr = agreementText.getText().getValue(msg);
			String aText = (agreementTextStr.length() > MAX_TEXT_LENGHT) ? 
					agreementTextStr.substring(0, MAX_TEXT_LENGHT) + "[...]" : agreementTextStr;
			optins.addEntry(info + ": " +  aText);
		}
		optinsP.setVisible(!optins.getElements().isEmpty());
		
		policyAgreements.clearContents();
		for (int i = 0; i < request.getPolicyAgreements().size(); i++)
		{
			PolicyAgreementDecision policyAgreementDecision = request.getPolicyAgreements().get(i);
			if (form.getPolicyAgreements().size() <= i)
				break;
			PolicyAgreementConfiguration policyAgreementConfig = form.getPolicyAgreements().get(i);
			String info = policyAgreementDecision == null ?
					msg.getMessage("RequestReviewPanel.skipped")
					: "[" + msg.getMessage("PolicyAgreementAcceptanceStatus."
							+ policyAgreementDecision.acceptanceStatus.toString()) + "]";
			String agreementDocs = "["
					+ String.join(", ", policyAgreementConfig.documentsIdsToAccept.stream()
							.map(d -> getPolicyDocName(d)).collect(Collectors.toList()))
					+ "]";
			String agreementText = policyAgreementRepresentationBuilder
					.getAgreementRepresentationText(policyAgreementConfig);
			String agreementTextWithDoc = agreementText + " " + agreementDocs;
			String aText = (agreementTextWithDoc.length() > MAX_TEXT_LENGHT)
					? agreementTextWithDoc.substring(0, MAX_TEXT_LENGHT) + "[...]"
					: agreementTextWithDoc;
			policyAgreements.addEntry(info + ": " + aText);
		}
		policyAgreementsP.setVisible(policyAgreements.getComponentCount() > 0);
		
		attributes.clearEntries();
		for (Attribute ap: request.getAttributes())
		{
			if (ap == null)
				continue;
			Component rep = handlersRegistry.getRepresentation(ap,
					AttributeViewerContext.builder().withImageScaleHeight(500)
							.withImageScaleWidth(500).withCustomWidth(100)
							.withCustomWidthUnit(Unit.PERCENTAGE).build());
			attributes.addEntry(rep, false);
		}
		attributesPanel.setVisible(!attributes.isEmpty());
	}

	protected void setGroupEntries(List<Component> list)
	{
		groups.clearEntries();
		for (Component c : list)
		{
			groups.addEntry(c, false);
		}
		groupsPanel.setVisible(!groups.isEmpty());
	}

	private String getGroupDisplayedName(GroupsManagement groupMan, String path)
	{
		try
		{
			return groupMan.getContents(path, GroupContents.METADATA).getGroup().getDisplayedName()
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

		for (int i = 0; i < request.getGroupSelections().size(); i++)
		{
			GroupSelection selection = request.getGroupSelections().get(i);
			if (form.getGroupParams().size() <= i)
				break;
			HorizontalLayout wrapper = new HorizontalLayout();
			wrapper.setSpacing(false);
			wrapper.setMargin(false);
			if (selection.getExternalIdp() != null)
				wrapper.addComponent(new Label("[from: " + selection.getExternalIdp() + "]"));
			wrapper.addComponent(getSingleGroupEntryComponent(
					GroupDiffUtils.getSingleGroupDiff(groupMan.getGroupsByWildcard("/**"),
							allUserGroups, selection, form.getGroupParams().get(i)),
					showRemoved));
			groupEntries.add(wrapper);
		}
		return groupEntries;
	}

	private Component getSingleGroupEntryComponent(RequestedGroupDiff diff, boolean showRemoved)
	{
		HorizontalLayout main = new HorizontalLayout();
		main.setSpacing(true);
		main.setMargin(false);

		addGroupLabel(main, diff.toAdd, Styles.success.toString());

		if (showRemoved)
		{
			addGroupLabel(main, diff.toRemove, Styles.error.toString());
		}

		addGroupLabel(main, diff.remain, Styles.bold.toString());

		return main;
	}

	private void addGroupLabel(HorizontalLayout layout, Set<String> value, String style)
	{
		if (value == null || value.isEmpty())
			return;
		Label l = new Label(value.stream().sorted().map(g -> getGroupDisplayedName(groupMan, g))
				.collect(Collectors.toList()).toString());
		l.setStyleName(style);
		layout.addComponent(l);
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
