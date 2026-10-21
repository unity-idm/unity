package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

@Tag("admin-directory")
class AttributeTypeImportTest extends UserAccountTestBase
{
	private final String name = "ui-import-" + UUID.randomUUID();
	private TestUser user;
	@TempDir
	Path temporaryDirectory;

	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;

	@AfterEach
	void removeType() throws Exception
	{
		if (types.getAttributeTypesAsMap().containsKey(name))
		{
			if (user != null && !attributes.getAttributes(user.entity(), "/", name).isEmpty())
				attributes.removeAttribute(user.entity(), "/", name);
			types.removeAttributeType(name, false);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("importCases")
	void shouldImportValidTypesAndRejectMalformedFilesAtomically(ImportCase scenario) throws Exception
	{
		user = createUser("Imported attribute owner");
		AttributeType definition = new AttributeType(name, "string");
		StringAttributeSyntax syntax = new StringAttributeSyntax();
		syntax.setMinLength(3);
		definition.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		String json = switch (scenario.input())
		{
			case "valid" -> "[" + definition.toJson() + "]";
			case "malformed" -> "[{invalid json";
			case "partial" -> "[" + definition.toJson() + ", {\"name\":\"broken\"}]";
			default -> throw new IllegalArgumentException(scenario.input());
		};
		Path file = temporaryDirectory.resolve("attribute-types.json");
		Files.writeString(file, json);
		Set<String> before = Set.copyOf(types.getAttributeTypesAsMap().keySet());
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/attribute-type/import");
		page.visible(testId("ImportAttributeTypeEditor.upload")).getShadowRoot()
				.findElement(By.cssSelector("input[type='file']")).sendKeys(file.toAbsolutePath().toString());
		if (scenario.valid())
			assertThat(page.visible(testId("ImportAttributeTypeEditor.types")).getText()).contains(name);
		else
			assertThat(page.visible(By.cssSelector("vaadin-notification-card[theme~='error']")).getText())
					.contains("Can not parse attribute types");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/attribute-types");

		if (scenario.valid())
		{
			assertThat(types.getAttributeTypesAsMap().keySet()).containsAll(before).contains(name);
			AttributeType stored = types.getAttributeTypesAsMap().get(name);
			assertThat(stored.getValueSyntax()).isEqualTo("string");
			assertThat(stored.getValueSyntaxConfiguration().get("minLength").asInt()).isEqualTo(3);
			attributes.setAttribute(user.entity(), StringAttribute.of(name, "/", "imported-value"));
			assertThat(attributes.getAttributes(user.entity(), "/", name)).singleElement()
					.satisfies(attribute -> assertThat(attribute.getValues()).containsExactly("imported-value"));
			driver.get(baseUrl + "/admin/attribute-type/edit/" + name);
			assertThat(page.value(testId("StringSyntaxEditor.minimumLength"))).isEqualTo("3");
		}
		else
			assertThat(types.getAttributeTypesAsMap().keySet()).containsExactlyInAnyOrderElementsOf(before);
	}

	private static Stream<ImportCase> importCases()
	{
		return Stream.of(ImportCase.builder().input("valid").valid(true).build(),
				ImportCase.builder().input("malformed").build(), ImportCase.builder().input("partial").build());
	}

	record ImportCase(String input, boolean valid)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return input;
		}

		static final class Builder
		{
			private String input;
			private boolean valid;

			Builder input(String input)
			{
				this.input = input;
				return this;
			}

			Builder valid(boolean valid)
			{
				this.valid = valid;
				return this;
			}

			ImportCase build()
			{
				return new ImportCase(input, valid);
			}
		}
	}
}
