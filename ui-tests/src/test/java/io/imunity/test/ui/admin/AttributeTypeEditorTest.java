package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

@Tag("admin-directory")
class AttributeTypeEditorTest extends UserAccountTestBase
{
	private final String typeName = "ui-editor-" + UUID.randomUUID();
	private TestUser user;

	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement attributeTypes;

	@AfterEach
	void removeType() throws Exception
	{
		if (attributeTypes.getAttributeTypesAsMap().containsKey(typeName))
		{
			if (user != null && !attributes.getAttributes(user.entity(), "/", typeName).isEmpty())
				attributes.removeAttribute(user.entity(), "/", typeName);
			attributeTypes.removeAttributeType(typeName, false);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("editorCases")
	void shouldCreateEditUseAndDeleteConstrainedStringType(EditorCase editorCase) throws Exception
	{
		user = createUser("Attribute lifecycle owner");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/attribute-type/new");
		page.fill(editorControl("RegularAttributeTypeEditor.name"), typeName);
		page.select(editorControl("RegularAttributeTypeEditor.syntax"), "string");
		page.fill(editorControl("StringSyntaxEditor.minimumLength"), "3");
		page.fill(editorControl("StringSyntaxEditor.regexp"), editorCase.regexp());
		page.check(editorControl("StringSyntaxEditor.multiline"), editorCase.multiline());

		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/attribute-types");
		driver.get(baseUrl + "/admin/attribute-type/edit/" + typeName);
		assertThat(page.value(editorControl("RegularAttributeTypeEditor.name"))).isEqualTo(typeName);
		assertThat(page.value(editorControl("RegularAttributeTypeEditor.syntax"))).isEqualTo("string");
		assertThat(page.value(editorControl("StringSyntaxEditor.minimumLength"))).isEqualTo("3");
		assertThat(page.value(editorControl("StringSyntaxEditor.regexp"))).isEqualTo(editorCase.regexp());
		assertThat(Boolean.parseBoolean(page.visible(editorControl("StringSyntaxEditor.multiline"))
				.getDomProperty("checked")))
				.isEqualTo(editorCase.multiline());

		AttributeType stored = attributeTypes.getAttributeTypesAsMap().get(typeName);
		StringAttributeSyntax syntax = new StringAttributeSyntax();
		syntax.setSerializedConfiguration(stored.getValueSyntaxConfiguration());
		assertThat(syntax.getMinLength()).isEqualTo(3);
		assertThat(syntax.getRegexp()).isEqualTo(editorCase.regexp());
		assertThat(syntax.isEditWithTextArea()).isEqualTo(editorCase.multiline());

		page.fill(editorControl("StringSyntaxEditor.minimumLength"), "4");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/attribute-types");
		driver.get(baseUrl + "/admin/attribute-type/edit/" + typeName);
		assertThat(page.value(editorControl("StringSyntaxEditor.minimumLength"))).isEqualTo("4");
		assertThat(attributeTypes.getAttributeTypesAsMap().get(typeName).getValueSyntaxConfiguration()
				.get("minLength").asInt()).isEqualTo(4);
		screenshot("Saved attribute type editor");
		assertThatThrownBy(() -> attributes.setAttribute(user.entity(), StringAttribute.of(typeName, "/", "Ab")))
				.isInstanceOf(IllegalAttributeValueException.class);
		attributes.setAttribute(user.entity(), StringAttribute.of(typeName, "/", "Alpha"));
		assertThat(attributes.getAttributes(user.entity(), "/", typeName)).singleElement()
				.satisfies(attribute -> assertThat(attribute.getValues()).containsExactly("Alpha"));
		attributes.removeAttribute(user.entity(), "/", typeName);
		assertThat(attributes.getAttributes(user.entity(), "/", typeName)).isEmpty();
		driver.get(baseUrl + "/admin/attribute-types");
		page.fill(testId("AttributeTypesView.search"), typeName);
		new GridPage(driver).action("AttributeTypesView.types", "attribute-" + typeName, "vaadin:menu");
		new AdminPage(driver).chooseMenuItem("Remove");
		page.click(By.cssSelector("[data-testid='AttributeTypesView.remove'] [slot='confirm-button']"));
		waitForElementNotPresent(testId("AttributeTypesView.remove"));
		waitForPageLoadByURL("/attribute-types");
		assertThat(attributeTypes.getAttributeTypesAsMap()).doesNotContainKey(typeName);
		driver.navigate().refresh();
		page.fill(testId("AttributeTypesView.search"), typeName);
		assertThat(page.visible(testId("AttributeTypesView.types")).getText()).doesNotContain(typeName);
	}

	private static By editorControl(String name)
	{
		return testId("RegularAttributeTypeEditor", name);
	}

	private static Stream<EditorCase> editorCases()
	{
		return Stream.of(
				EditorCase.builder().name("single-line string").regexp("[A-Z][a-z]+").build(),
				EditorCase.builder().name("multiline string").regexp("[\\s\\S]+").multiline(true).build());
	}

	record EditorCase(String name, String regexp, boolean multiline)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return name;
		}

		static final class Builder
		{
			private String name;
			private String regexp;
			private boolean multiline;

			Builder name(String name)
			{
				this.name = name;
				return this;
			}

			Builder regexp(String regexp)
			{
				this.regexp = regexp;
				return this;
			}

			Builder multiline(boolean multiline)
			{
				this.multiline = multiline;
				return this;
			}

			EditorCase build()
			{
				return new EditorCase(name, regexp, multiline);
			}
		}
	}
}
