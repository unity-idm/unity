package io.imunity.test.ui.admin;

import java.util.stream.Stream;

record AttributeWidgetCase(String name, String syntax, Widget widget, String initial, String updated, String invalid)
{
	enum Widget
	{
		TEXT, MULTILINE, BOOLEAN, ENUM, DATE, DATETIME, ZONED_DATETIME, EMAIL, MOBILE, POLICY,
		IMAGE, LINKED_IMAGE, IMAGE_URL
	}

	static Stream<AttributeWidgetCase> cases()
	{
		return Stream.of(
				builder("string", Widget.TEXT).values("First value", "Updated value").invalid("x").build(),
				builder("string", Widget.MULTILINE).name("multiline string")
						.values("First line\nSecond line", "Updated first\nUpdated second").invalid("x").build(),
				builder("integer", Widget.TEXT).values("42", "73").invalid("9").build(),
				builder("floatingPoint", Widget.TEXT).values("1.25", "3.75").invalid("21").build(),
				builder("json", Widget.MULTILINE).values("{\"enabled\":true,\"level\":1}",
						"{\"enabled\":false,\"level\":2}").invalid("{broken").build(),
				builder("boolean", Widget.BOOLEAN).values("false", "true").build(),
				builder("enumeration", Widget.ENUM).values("blue", "green").invalid("").build(),
				builder("date", Widget.DATE).values("2030-05-08", "2031-06-09").invalid("").build(),
				builder("datetime", Widget.DATETIME).values("2030-05-08T12:30:00", "2031-06-09T16:45:00")
						.invalid("").build(),
				builder("zonedDatetime", Widget.ZONED_DATETIME)
						.values("2030-05-08T12:30:00+02:00[Europe/Warsaw]", "2031-01-09T16:45:00Z[UTC]")
						.invalid("").build(),
				builder("verifiableEmail", Widget.EMAIL).values("first@example.org", "second@example.org")
						.invalid("missing-at").build(),
				builder("verifiableMobileNumber", Widget.MOBILE).values("+12025550101", "+442079460018")
						.invalid("not-a-number").build(),
				builder("policyagreement", Widget.POLICY).values("ACCEPTED", "NOT_ACCEPTED").build(),
				builder("image", Widget.IMAGE).values("initial", "updated").invalid("missing").build(),
				builder("publicLinkableImage", Widget.LINKED_IMAGE).name("public image upload")
						.values("initial", "updated").invalid("missing").build(),
				builder("publicLinkableImage", Widget.IMAGE_URL).name("public image URL")
						.values("https://example.org/first.png", "https://example.org/second.png")
						.invalid("invalid-url").build());
	}

	boolean uploadsImage()
	{
		return widget == Widget.IMAGE || widget == Widget.LINKED_IMAGE;
	}

	boolean usesImage()
	{
		return uploadsImage() || widget == Widget.IMAGE_URL;
	}

	static Builder builder(String syntax, Widget widget)
	{
		return new Builder(syntax, widget);
	}

	@Override
	public String toString()
	{
		return name;
	}

	static final class Builder
	{
		private String name;
		private final String syntax;
		private final Widget widget;
		private String initial;
		private String updated;
		private String invalid;

		private Builder(String syntax, Widget widget)
		{
			name = syntax;
			this.syntax = syntax;
			this.widget = widget;
		}

		Builder name(String name)
		{
			this.name = name;
			return this;
		}

		Builder values(String initial, String updated)
		{
			this.initial = initial;
			this.updated = updated;
			return this;
		}

		Builder invalid(String invalid)
		{
			this.invalid = invalid;
			return this;
		}

		AttributeWidgetCase build()
		{
			return new AttributeWidgetCase(name, syntax, widget, initial, updated, invalid);
		}
	}
}
