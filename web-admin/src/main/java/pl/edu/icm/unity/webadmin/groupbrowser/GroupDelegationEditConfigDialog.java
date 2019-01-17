/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.groupbrowser;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

/**
 * Edit dialog for {@link GroupDelegationConfiguration}.
 * 
 * @author P.Piernik
 *
 */
public class GroupDelegationEditConfigDialog extends AbstractDialog
{
	private Consumer<GroupDelegationConfiguration> callback;
	private GroupDelegationConfiguration toEdit;
	private TextField logoUrl;
	private CheckBox enableDelegation;
	private ComboBox<String> registrationFormCombo;
	private ComboBox<String> signupEnquiryFormCombo;
	private ComboBox<String> stickyEnquiryFormCombo;
	private ChipsWithDropdown<String> attributes;
	private Binder<DelegationConfiguration> binder;

	private RegistrationsManagement registrationMan;
	private EnquiryManagement enquiryMan;
	private AttributeTypeManagement attrTypeMan;

	public GroupDelegationEditConfigDialog(UnityMessageSource msg, RegistrationsManagement registrationMan,
			EnquiryManagement enquiryMan, AttributeTypeManagement attrTypeMan,
			GroupDelegationConfiguration toEdit, Consumer<GroupDelegationConfiguration> callback)
	{
		super(msg, msg.getMessage("GroupDelegationEditConfigDialog.caption"), msg.getMessage("ok"),
				msg.getMessage("cancel"));
		this.toEdit = toEdit;
		this.callback = callback;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.attrTypeMan = attrTypeMan;
	}

	private void enableEdit(boolean enabled)
	{
		logoUrl.setEnabled(enabled);
		registrationFormCombo.setEnabled(enabled);
		signupEnquiryFormCombo.setEnabled(enabled);
		stickyEnquiryFormCombo.setEnabled(enabled);
		attributes.setEnabled(enabled);
	}

	@Override
	protected Component getContents() throws Exception
	{

		enableDelegation = new CheckBox(
				msg.getMessage("GroupDelegationEditConfigDialog.enableDelegationCaption"));
		enableDelegation.addValueChangeListener(e -> {
			enableEdit(e.getValue());
		});
		logoUrl = new TextField(msg.getMessage("GroupDelegationEditConfigDialog.logoUrlCaption"));
		logoUrl.setWidth(100, Unit.PERCENTAGE);
		registrationFormCombo = new ComboBox<String>(
				msg.getMessage("GroupDelegationEditConfigDialog.registrationForm"));

		List<RegistrationForm> forms = registrationMan.getForms();
		registrationFormCombo.setItems(forms.stream().map(f -> f.getName()).collect(Collectors.toList()));

		signupEnquiryFormCombo = new ComboBox<String>(
				msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiry"));
		List<EnquiryForm> enquires = enquiryMan.getEnquires();
		signupEnquiryFormCombo.setItems(enquires.stream().map(f -> f.getName()).collect(Collectors.toList()));

		stickyEnquiryFormCombo = new ComboBox<String>(
				msg.getMessage("GroupDelegationEditConfigDialog.stickyEnquiry"));
		stickyEnquiryFormCombo.setItems(enquires.stream().filter(f -> f.getType().equals(EnquiryType.STICKY))
				.map(f -> f.getName()).collect(Collectors.toList()));

		attributes = new ChipsWithDropdown<>();
		attributes.setCaption(msg.getMessage("GroupDelegationEditConfigDialog.attributes"));
		attributes.setMaxSelection(4);

		Collection<AttributeType> attributeTypes = attrTypeMan.getAttributeTypes();
		attributes.setItems(attributeTypes.stream().map(a -> a.getName()).collect(Collectors.toList()));
		if (toEdit.attributes != null)
		{
			attributes.setSelectedItems(toEdit.attributes.stream().collect(Collectors.toList()));
		}

		binder = new Binder<>(DelegationConfiguration.class);
		binder.forField(enableDelegation).bind("enabled");
		binder.forField(logoUrl).bind("logoUrl");
		binder.forField(registrationFormCombo).bind("registrationForm");
		binder.forField(stickyEnquiryFormCombo).bind("stickyEnquiryForm");
		binder.forField(signupEnquiryFormCombo).bind("signupEnquiryForm");
		binder.setBean(new DelegationConfiguration(toEdit));
		enableEdit(toEdit.enabled);

		FormLayout main = new FormLayout();
		main.addComponents(enableDelegation, logoUrl, registrationFormCombo, signupEnquiryFormCombo,
				stickyEnquiryFormCombo, attributes);
		return main;
	}

	@Override
	protected void onConfirm()
	{
		try
		{

			DelegationConfiguration groupDelConfig = binder.getBean();

			validateRegistrationForm(groupDelConfig.registrationForm);

			GroupDelegationConfiguration config = new GroupDelegationConfiguration(
					groupDelConfig.isEnabled(), groupDelConfig.getLogoUrl(),
					groupDelConfig.getRegistrationForm(), groupDelConfig.getSignupEnquiryForm(),
					groupDelConfig.getStickyEnquiryForm(), attributes.getSelectedItems());

			callback.accept(config);
			close();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("GroupDelegationEditConfigDialog.cannotUpdate"),
					e);
		}
	}

	private void validateRegistrationForm(String registrationFormId) throws FormValidationException, EngineException
	{
		if (registrationFormId == null)
			return;

		RegistrationForm form = registrationMan.getForms().stream()
				.filter(f -> f.getName().equals(registrationFormId)).findFirst().orElse(null);
		if (form == null)
		{
			throw new FormValidationException("Illegal registration form");
		}

		if (form.getGroupParams() == null || form.getGroupParams().size() != 1)
		{
			throw new FormValidationException(
					"Registration form should have configured one group selection param");
		}

		if (form.getIdentityParams() == null || form.getIdentityParams().size() != 1
				|| !form.getIdentityParams().get(0).getIdentityType().equals(EmailIdentity.ID))
		{
			throw new FormValidationException(
					"Registration form should have configured one email identity param");
		}

		RegistrationFormNotifications notConfig = form.getNotificationsConfiguration();
		if (notConfig == null || notConfig.getInvitationTemplate() == null
				|| notConfig.getInvitationTemplate().isEmpty())
		{
			throw new FormValidationException(
					"Registration form should have configured notification invitation template");
		}
	}

	// for binder only
	public static class DelegationConfiguration
	{
		private boolean enabled;
		private String logoUrl;
		private String registrationForm;
		private String signupEnquiryForm;
		private String stickyEnquiryForm;

		public DelegationConfiguration(GroupDelegationConfiguration org)
		{
			setEnabled(org.enabled);
			setLogoUrl(org.logoUrl);
			setRegistrationForm(org.registrationForm);
			setSignupEnquiryForm(org.signupEnquiryForm);
			setStickyEnquiryForm(org.stickyEnquiryForm);
		}

		public boolean isEnabled()
		{
			return enabled;
		}

		public void setEnabled(boolean enabled)
		{
			this.enabled = enabled;
		}

		public String getLogoUrl()
		{
			return logoUrl;
		}

		public void setLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
		}

		public String getRegistrationForm()
		{
			return registrationForm;
		}

		public void setRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
		}

		public String getSignupEnquiryForm()
		{
			return signupEnquiryForm;
		}

		public void setSignupEnquiryForm(String signupEnquiryForm)
		{
			this.signupEnquiryForm = signupEnquiryForm;
		}

		public String getStickyEnquiryForm()
		{
			return stickyEnquiryForm;
		}

		public void setStickyEnquiryForm(String stickyEnquiryForm)
		{
			this.stickyEnquiryForm = stickyEnquiryForm;
		}
	}
}
