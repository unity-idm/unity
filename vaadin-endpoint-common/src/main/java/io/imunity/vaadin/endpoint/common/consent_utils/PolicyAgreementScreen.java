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
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;

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
	private final Runnable submitHandler;

	private Button submitButton;
	private H4 titleLabel;
	private H5 infoLabel;
	private VerticalLayout contents;

	PolicyAgreementScreen(
			MessageSource msg, PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder,
			PolicyAgreementManagement policyAgreementDecider, NotificationPresenter notificationPresenter,
	        Runnable submitHandler
	) {
		this.msg = msg;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
		this.policyAgreementDecider = policyAgreementDecider;
		this.notificationPresenter = notificationPresenter;
		this.submitHandler = submitHandler;
		agreements = new ArrayList<>();
		init();
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

	private void withAgreements(List<PolicyAgreementConfiguration> agreementsConfigs)
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
	}

	public static PolicyAgreementScreenBuilder builder()
	{
		return new PolicyAgreementScreenBuilder();
	}
	public static final class PolicyAgreementScreenBuilder
	{
		private MessageSource msg;
		private PolicyAgreementManagement policyAgreementDecider;
		private NotificationPresenter notificationPresenter;
		private PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;
		private List<PolicyAgreementConfiguration> agreementsConfig;
		private I18nString title;
		private I18nString info;
		private String width;
		private Runnable submitHandler;

		private PolicyAgreementScreenBuilder()
		{
		}

		public PolicyAgreementScreenBuilder withMsg(MessageSource msg)
		{
			this.msg = msg;
			return this;
		}

		public PolicyAgreementScreenBuilder withPolicyAgreementDecider(PolicyAgreementManagement policyAgreementDecider)
		{
			this.policyAgreementDecider = policyAgreementDecider;
			return this;
		}

		public PolicyAgreementScreenBuilder withNotificationPresenter(NotificationPresenter notificationPresenter)
		{
			this.notificationPresenter = notificationPresenter;
			return this;
		}

		public PolicyAgreementScreenBuilder withAgreements(List<PolicyAgreementConfiguration> agreementsConfig)
		{
			this.agreementsConfig = agreementsConfig;
			return this;
		}

		public PolicyAgreementScreenBuilder withPolicyAgreementRepresentationBuilder(PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder)
		{
			this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
			return this;
		}

		public PolicyAgreementScreenBuilder withTitle(I18nString title)
		{
			this.title = title;
			return this;
		}

		public PolicyAgreementScreenBuilder withInfo(I18nString info)
		{
			this.info = info;
			return this;
		}

		public PolicyAgreementScreenBuilder withWidth(float width, String unit)
		{
			this.width = width + Unit.getUnitFromSymbol(unit).getSymbol();
			return this;
		}

		public PolicyAgreementScreenBuilder withSubmitHandler(Runnable submitHandler)
		{
			this.submitHandler = submitHandler;
			return this;
		}

		public PolicyAgreementScreen build()
		{
			PolicyAgreementScreen policyAgreementScreen = new PolicyAgreementScreen(msg, policyAgreementRepresentationBuilder, policyAgreementDecider, notificationPresenter, submitHandler);
			if(title != null)
			{
				policyAgreementScreen.titleLabel.setText(title.getValue(msg));
				policyAgreementScreen.titleLabel.setVisible(true);
			}
			if(info != null)
			{
				policyAgreementScreen.infoLabel.setText(info.getValue(msg));
				policyAgreementScreen.infoLabel.setVisible(true);
			}
			if(width != null)
			{
				policyAgreementScreen.contents.setWidth(width);
			}
			policyAgreementScreen.withAgreements(agreementsConfig);
			return policyAgreementScreen;
		}
	}
}
