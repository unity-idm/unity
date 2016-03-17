/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;


/**
 * Builder of {@link EnquiryForm}.
 */
public class EnquiryFormBuilder extends BaseFormBuilder<EnquiryFormBuilder>
{
	private EnquiryForm instance;
	
	public EnquiryFormBuilder()
	{
		super(new EnquiryForm());
		instance = (EnquiryForm) super.getInstance();
	}
	
	public EnquiryForm build()
	{
		instance.validate();
		return instance;
	}
	
	public EnquiryFormBuilder withTargetGroups(String[] aValue)
	{
		instance.setTargetGroups(aValue);

		return (EnquiryFormBuilder) this;
	}

	public EnquiryFormBuilder withType(EnquiryType aValue)
	{
		instance.setType(aValue);

		return (EnquiryFormBuilder) this;
	}

	public EnquiryFormBuilder withNotificationsConfiguration(EnquiryFormNotifications aValue)
	{
		instance.setNotificationsConfiguration(aValue);

		return (EnquiryFormBuilder) this;
	}

	public EnquiryFormNotificationsBuilder withNotificationsConfiguration()
	{
		EnquiryFormNotifications obj = new EnquiryFormNotifications();

		withNotificationsConfiguration(obj);

		return new EnquiryFormNotificationsBuilder(obj);
	}

	public static class EnquiryFormNotificationsBuilder
	{
		private EnquiryFormNotifications instance;

		protected EnquiryFormNotificationsBuilder(EnquiryFormNotifications aInstance)
		{
			instance = aInstance;
		}

		protected EnquiryFormNotifications getInstance()
		{
			return instance;
		}

		public EnquiryFormNotificationsBuilder withSubmittedTemplate(String aValue)
		{
			instance.setSubmittedTemplate(aValue);

			return this;
		}

		public EnquiryFormNotificationsBuilder withEnquiryToFillTemplate(String aValue)
		{
			instance.setEnquiryToFillTemplate(aValue);

			return this;
		}

		public EnquiryFormNotificationsBuilder withChannel(String aValue)
		{
			instance.setChannel(aValue);

			return this;
		}

		public EnquiryFormNotificationsBuilder withAdminsNotificationGroup(String aValue)
		{
			instance.setAdminsNotificationGroup(aValue);

			return this;
		}
	}
}
