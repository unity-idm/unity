/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.idp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfiguration;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotNullComboBox;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementConfigurationEditor;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementConfigurationList;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * Common Idp service editor policy agreements tab. Produces
 * {@link IdpPolicyAgreementsConfiguration}. Can be used directly with the
 * vaadin binder
 * 
 * @author P.Piernik
 *
 */
public class PolicyAgreementsTab extends CustomField<IdpPolicyAgreementsConfiguration> implements EditorTab
{
	private MessageSource msg;
	private Collection<PolicyDocumentWithRevision> policyDocuments;
	private Binder<IdpPolicyAgreementsConfigurationVaadinBean> binder;
	private VerticalLayout main;

	public PolicyAgreementsTab(MessageSource msg, Collection<PolicyDocumentWithRevision> policyDocuments)
	{
		setCaption(msg.getMessage("IdpServiceEditorBase.policyAgreements"));
		setIcon(Images.check_square.getResource());

		this.msg = msg;
		this.policyDocuments = policyDocuments;
		init();
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.POLICY_AGREEMENTS.toString();
	}

	private void init()
	{
		PolicyAgreementConfigurationList list = new PolicyAgreementConfigurationList(msg,
				() -> new PolicyAgreementConfigurationEditor(msg, policyDocuments));

		binder = new Binder<>(IdpPolicyAgreementsConfigurationVaadinBean.class);
		I18nTextField title = new I18nTextField(msg, msg.getMessage("PolicyAgreementTab.title"));
		title.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		binder.forField(title).withValidator((v, c) -> {

			if (!list.getValue().isEmpty() && (v == null || v.isEmpty()))
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("title");

		I18nTextField info = new I18nTextField(msg, msg.getMessage("PolicyAgreementTab.info"));
		info.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		binder.forField(info).bind("information");

		IntStepper width = new IntStepper();
		width.setWidth(3, Unit.EM);
		width.setStyleName("u-maxWidth3");
		width.setMinValue(1);
		
		ComboBox<String> widthUnit = new NotNullComboBox<>();
		widthUnit.setWidth (6, Unit.EM);
		
		widthUnit.setItems(Stream.of(Unit.values()).map(Unit::toString));
		binder.forField(width)
				.bind("width");
		binder.forField(widthUnit).bind("widthUnit");

		binder.forField(list).withValidator((v, c) -> {
			for (PolicyAgreementConfiguration con : v)
			{
				if (con == null)
					return ValidationResult.error("");
			}
			return ValidationResult.ok();
		}).bind("agreements");
		main = new VerticalLayout();
		FormLayout header = new FormLayout();
		header.setMargin(false);
		header.addComponent(title);
		header.addComponent(info);
		
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.setCaption(msg.getMessage("PolicyAgreementTab.width"));
		wrapper.addComponent(width);
		wrapper.addComponent(widthUnit);
		wrapper.setComponentAlignment(width, Alignment.MIDDLE_CENTER);
		wrapper.setComponentAlignment(widthUnit, Alignment.MIDDLE_CENTER);
		header.addComponent(wrapper);
		main.addComponent(header);
		main.addComponent(list);
		binder.addValueChangeListener(e -> fireEvent(new ValueChangeEvent<>(this, getValue(), true)));
	}

	@Override
	public IdpPolicyAgreementsConfiguration getValue()
	{
		if (binder.validate().hasErrors())
			return null;
		IdpPolicyAgreementsConfigurationVaadinBean bean = binder.getBean();
		if (bean == null)
			return null;
		return new IdpPolicyAgreementsConfiguration(bean.getTitle(), bean.getInformation(), bean.getWidth(),
				bean.getWidthUnit(), bean.getAgreements());
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	protected void doSetValue(IdpPolicyAgreementsConfiguration value)
	{
		binder.setBean(new IdpPolicyAgreementsConfigurationVaadinBean(value));
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	public static class IdpPolicyAgreementsConfigurationVaadinBean
	{
		private I18nString title;
		private I18nString information;
		private List<PolicyAgreementConfiguration> agreements;
		private int width;
		private String widthUnit;

		public IdpPolicyAgreementsConfigurationVaadinBean()
		{

		}

		public IdpPolicyAgreementsConfigurationVaadinBean(IdpPolicyAgreementsConfiguration source)
		{
			this.agreements = new ArrayList<>();
			if (source == null)
				return;
			this.agreements.addAll(source.agreements);
			this.title = source.title;
			this.information = source.information;
			this.width = source.width;
			this.widthUnit = source.widthUnit;
		}

		public I18nString getTitle()
		{
			return title;
		}

		public void setTitle(I18nString title)
		{
			this.title = title;
		}

		public List<PolicyAgreementConfiguration> getAgreements()
		{
			return agreements;
		}

		public void setAgreements(List<PolicyAgreementConfiguration> agreements)
		{
			this.agreements = agreements;
		}

		public I18nString getInformation()
		{
			return information;
		}

		public void setInformation(I18nString information)
		{
			this.information = information;
		}

		public int getWidth()
		{
			return width;
		}

		public void setWidth(int width)
		{
			this.width = width;
		}

		public String getWidthUnit()
		{
			return widthUnit;
		}

		public void setWidthUnit(String widthUnit)
		{
			this.widthUnit = widthUnit;
		}
	}
}
