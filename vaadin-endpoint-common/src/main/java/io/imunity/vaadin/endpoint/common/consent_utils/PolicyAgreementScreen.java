/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentation;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class PolicyAgreementScreen extends VerticalLayout
{
	private final MessageSource msg;
	private final PolicyAgreementManagement policyAgreementDecider;
	private final NotificationPresenter notificationPresenter;
	private final List<PolicyAgreementRepresentation> agreements;
	private final PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;

	private Button submitButton;
	private H4 titleLabel;
	private H5 infoLabel;
	private VerticalLayout contents;
	private Runnable submitHandler;
	public PolicyAgreementScreen(MessageSource msg,
	                             PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder,
	                             PolicyAgreementManagement policyAgreementDecider, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
		this.policyAgreementDecider = policyAgreementDecider;
		this.notificationPresenter = notificationPresenter;
		agreements = new ArrayList<>();
		init();
	}

	public PolicyAgreementScreen withAgreements(List<PolicyAgreementConfiguration> agreementsConfigs)
	{
		for (PolicyAgreementConfiguration config : agreementsConfigs)
		{
			PolicyAgreementRepresentation agrRep = policyAgreementRepresentationBuilder
					.getAgreementRepresentation(config);
			agrRep.addValueChangeListener(this::refreshSubmitButton);
			agreements.add(agrRep);
			contents.add(agrRep);
			contents.setAlignItems(Alignment.CENTER);
		}
		refreshSubmitButton();

		return this;
	}

	public PolicyAgreementScreen withTitle(I18nString title)
	{
		titleLabel.setText(title.getValue(msg));
		titleLabel.setVisible(true);
		return this;
	}

	public PolicyAgreementScreen withInfo(I18nString info)
	{
		infoLabel.setText(info.getValue(msg));
		infoLabel.setVisible(true);
		return this;
	}

	public PolicyAgreementScreen withWidth(float width, String unit)
	{
		contents.setWidth(width, Unit.getUnitFromSymbol(unit));
		return this;
	}
	
	public PolicyAgreementScreen withSubmitHandler(Runnable submitHandler)
	{
		this.submitHandler = submitHandler;
		return this;
	}

	public PolicyAgreementScreen init()
	{
		setMargin(false);
		setSpacing(false);
		setWidthFull();
		setAlignItems(Alignment.CENTER);
		contents = new VerticalLayout();
		titleLabel = new H4();
		titleLabel.setVisible(false);
		infoLabel = new H5();
		infoLabel.setVisible(false);
		Label space = new Label();
		contents.add(titleLabel);
		contents.add(infoLabel);
		contents.add(space);

		submitButton = new Button(msg.getMessage("submit"));
		submitButton.addClickListener(e -> submit());
		submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		add(contents, submitButton);
		refreshSubmitButton();
		setSizeFull();
		return this;
	}

	private void submit()
	{
		try
		{
			policyAgreementDecider.submitDecisions(
					new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
					agreements.stream()
							.map(PolicyAgreementRepresentation::getDecision)
							.collect(Collectors.toList())
			);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("PolicyAgreementScreen.submitError"), e.getMessage());
			return;
		}

		if (submitHandler != null)
		{
			submitHandler.run();
		}
	}

	private void refreshSubmitButton()
	{
		submitButton.setEnabled(agreements.stream().allMatch(PolicyAgreementRepresentation::isValid));
	}
}
