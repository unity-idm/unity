/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.util.ArrayList;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements;
import pl.edu.icm.unity.webui.common.ListOfSelectableElements.DisableMode;
import pl.edu.icm.unity.webui.common.SafePanel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Shows request contents and provides a possibility to edit it.
 * 
 * @author K. Benedyczak
 */
public class RequestReviewPanel extends CustomComponent
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersRegistry;
	private RegistrationRequestState requestState;
	
	private ListOfSelectableElements attributes;
	private ListOfSelectableElements groups;
	private ListOfElements<String> identities;
	private ListOfElements<String> agreements;
	private DescriptionTextArea comment;
	private Label code;
	
	public RequestReviewPanel(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry)
	{
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		
		identities = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		identities.setAddSeparatorLine(false);
		Panel identitiesP = new SafePanel(msg.getMessage("RequestReviewPanel.requestedIdentities"), identities);
		identitiesP.addStyleName(Reindeer.PANEL_LIGHT);
		
		Label aLabel = new Label(msg.getMessage("RequestReviewPanel.requestedAttributes"));
		aLabel.addStyleName(Styles.bold.toString());
		attributes = new ListOfSelectableElements(aLabel,
				new Label(msg.getMessage("RequestReviewPanel.requestedAttributeIgnore")), DisableMode.WHEN_SELECTED);
		
		Label gLabel = new Label(msg.getMessage("RequestReviewPanel.requestedGroups"));
		gLabel.addStyleName(Styles.bold.toString());
		groups = new ListOfSelectableElements(gLabel,
				new Label(msg.getMessage("RequestReviewPanel.requestedGroupsIgnore")), DisableMode.WHEN_SELECTED);
		
		agreements = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		agreements.setAddSeparatorLine(false);
		Panel agreementsP = new SafePanel(msg.getMessage("RequestReviewPanel.agreements"), agreements);
		agreementsP.addStyleName(Reindeer.PANEL_LIGHT);
		
		
		comment = new DescriptionTextArea(true, "");
		Panel commentP = new SafePanel(msg.getMessage("RequestReviewPanel.comment"), comment);
		commentP.addStyleName(Reindeer.PANEL_LIGHT);
		
		code = new Label(msg.getMessage("RequestReviewPanel.codeValid"));
		code.addStyleName(Styles.bold.toString());
		
		
		main.addComponents(code, identitiesP, attributes, groups, commentP, agreementsP);
		setCompositionRoot(main);
	}
	
	public RegistrationRequest getUpdatedRequest()
	{
		RegistrationRequest orig = requestState.getRequest();
		RegistrationRequest ret = new RegistrationRequest();
		ret.setAgreements(orig.getAgreements());
		ret.setComments(orig.getComments());
		ret.setCredentials(orig.getCredentials());
		ret.setFormId(orig.getFormId());
		ret.setIdentities(orig.getIdentities());
		ret.setRegistrationCode(orig.getRegistrationCode());
		
		ret.setGroupSelections(new ArrayList<Selection>(orig.getGroupSelections().size()));
		for (int i=0, j=0; i<orig.getGroupSelections().size(); i++)
		{
			if (orig.getGroupSelections().get(i).isSelected())
			{
				ret.getGroupSelections().add(new Selection(!groups.getSelection().get(j).getValue()));
				j++;
			} else
			{
				ret.getGroupSelections().add(new Selection(false));
			}
		}
		
		ret.setAttributes(new ArrayList<Attribute<?>>(attributes.getSelection().size()));
		for (int i=0, j=0; i<orig.getAttributes().size(); i++)
		{
			if (orig.getAttributes().get(i) == null)
				ret.getAttributes().add(null);
			else if (!attributes.getSelection().get(j++).getValue())
				ret.getAttributes().add(orig.getAttributes().get(i));
			else
				ret.getAttributes().add(null);
		}
		return ret;
	}
	
	public void setInput(RegistrationRequestState requestState, RegistrationForm form)
	{
		this.requestState = requestState;
		RegistrationRequest request = requestState.getRequest();
		String comments = request.getComments();
		if (comments != null && !comments.equals(""))
		{
			comment.setVisible(true);
			comment.setValue(comments);
		} else
			comment.setVisible(false);
		
		code.setVisible(request.getRegistrationCode() != null);
		
		identities.clearContents();
		for (IdentityParam idParam: request.getIdentities())
		{
			if (idParam == null)
				continue;
			String representation = idParam.toString();
			if (idParam.getRemoteIdp() != null)
				representation = "[from: " + idParam.getRemoteIdp() + "] " + representation;
			identities.addEntry(representation);
		}
		
		agreements.clearContents();
		for (int i=0; i<request.getAgreements().size(); i++)
		{
			Selection selection = request.getAgreements().get(i);
			if (form.getAgreements().size() <= i)
				break;
			AgreementRegistrationParam agreementText = form.getAgreements().get(i);
			String info = (selection.isSelected()) ? msg.getMessage("RequestReviewPanel.accepted") : 
				msg.getMessage("RequestReviewPanel.notAccepted");
			String aText = (agreementText.getText().length() > 100) ? 
					agreementText.getText().substring(0, 100) + "[...]" : agreementText.getText();
			agreements.addEntry(info + ": " +  aText);
		}
		
		attributes.clearEntries();
		for (Attribute<?> ap: request.getAttributes())
		{
			if (ap == null)
				continue;
			Label attrInfo = new Label();
			String representation = handlersRegistry.getSimplifiedAttributeRepresentation(ap, 80);
			if (ap.getRemoteIdp() != null)
				representation = "[from: " + ap.getRemoteIdp() + "] " + representation;
			attrInfo.setValue(representation);
			attributes.addEntry(attrInfo, false);
		}
		
		groups.clearEntries();
		for (int i=0; i<request.getGroupSelections().size(); i++)
		{
			Selection selection = request.getGroupSelections().get(i);
			if (!selection.isSelected())
				continue;
			if (form.getGroupParams().size() <= i)
				break;
			GroupRegistrationParam groupParam = form.getGroupParams().get(i);
			String groupEntry = selection.getExternalIdp() == null ? groupParam.getGroupPath() :
				"[from: " + selection.getExternalIdp() + "] " + groupParam.getGroupPath();
			groups.addEntry(new Label(groupEntry), false);
		}
	}
}
