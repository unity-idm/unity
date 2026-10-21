# Nightly Selenium runner

This module is selected only by the root `nightly-ui` profile. It uses the ordinary
`unity-ui-test-support` artifact with test scope and has no dependency on `integration-tests`.
Run commands from the repository root with Chrome and a compatible ChromeDriver available.

```sh
mvn -Pnightly-ui -pl ui-tests -am verify \
  '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' \
  -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost= > nightly-ui-build.log 2>&1
```

After installing upstream artifacts from the same revision, run an individual class with:

```sh
mvn -Pnightly-ui -pl ui-tests test -Dtest=AttributeTypeEditorTest \
  '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' \
  -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost= > nightly-ui-build.log 2>&1
```

Use `-Dgroups=<tag>` to select `admin-directory`, `admin-authentication`, `admin-settings`,
`admin-automation`, `home`, `upman`, or `public-policies`. The runner fails when no tests are found. Current coverage includes:

- Admin directory: constrained string-type lifecycle, valid/malformed attribute-type imports, mandatory
  attribute classes, nested group lifecycle, account disable/reactivate, password replacement, deletion,
  attribute-value editor round trips and invalid edits, and identity creation/removal with mapping checks.
- Admin authentication: six credential-definition editors and their information views, five local
  authenticator editors, required-field validation, credential-requirement lifecycle and user assignment,
  and realm settings/service references for all three remember-me policies.
- Admin settings: identity self-service limits and running build information.
- Admin automation: scheduled rule creation/editing, invalid schedules, and saved/ad-hoc execution
  with account-state and fresh-login checks for target and control users.
- Home: profile save/reload/reset with read-only authorization roles.
- UpMan: subgroup creation, displayed-name changes, visibility, and deletion with an unrelated project control.
- Public policies: English and Polish content without authentication, and unknown-document handling.

The credential-definition matrix covers `password`, `certificate`, `sms`, `otp`, `fido`, and
`private-key-jwt`. The certificate and client-public-key editors have no type-specific settings;
they round-trip common metadata. The local authenticator matrix covers `password`, `certificate`,
`sms`, `otp`, and `fido`, including localized login labels and credential bindings. Realm cases
cover `disallow`, `allowFor2ndFactor`, and `allowForWholeAuthn` remember-me policies. These realm
cases verify saved settings, service references, and login; they do not measure remember-me expiry.

The attribute-value matrix covers all fourteen registered syntaxes: `string`, `integer`,
`floatingPoint`, `boolean`, `enumeration`, `json`, `date`, `datetime`, `zonedDatetime`,
`verifiableEmail`, `verifiableMobileNumber`, `policyagreement`, `image`, and `publicLinkableImage`.
Separate cases exercise multiline strings and both public-image upload and URL entry. Invalid edits
preserve the previous stored value and can be corrected in the same dialog. Uploaded PNG fixtures
have known dimensions and pixels; the tests inspect stored image data and loaded browser previews.

The identity matrix covers `userName`, `email`, `identifier`, and `x500Name`. Each case adds an
identity, checks its persisted entity mapping, reloads it, and removes it without changing a control
account. Username/email aliases also authenticate in fresh browsers and stop working after removal.
Identifier and distinguished-name cases check identity resolution and preserve primary-username
login. Both matrices compare their inventories with the live editor registries.

Each execution gets independent fixture data. Stored outcomes are checked through engine APIs;
workflow actions use the browser. The public-policy locale is seeded with the supported language
cookie. This checks localized content; interactive language switching has separate coverage needs.

Both executable runners build a production frontend before tests. Their application dependencies
use compile scope so Vaadin can discover endpoint views and plugin resources. The support jar
stays test-scoped. Vaadin's Maven plugin manages the frontend packages; no separate npm command
is needed.

New Selenium hooks use component-qualified `data-testid` values, such as
`EntityCreationDialog.credentialRequirement` and `DialogWithActionFooter.confirm`.
These attributes can repeat across component instances. Scope reusable controls through the
owning dialog or editor, and use `data-group-path`, `data-attribute-name`, or `data-credential`
to identify repeated business objects. Request and member controls use `data-request-id` and
`data-member-id`; account actions and action parameters use `data-entity-id` and `data-parameter-name`.
Grid row part names identify entities, attribute values, automation rules, credential requirements,
and attribute types without changing their renderers.
The shared page helper selects visible controls when hidden menu instances
are also present, and re-resolves inputs if Vaadin replaces them while typing. Reserve HTML IDs
for elements requiring document-wide uniqueness. `VaadinPage.testId(...)` accepts an owner followed by descendant hooks for scoped
selectors.

The shared fixture uses an in-memory database, a fresh Spring context per execution, and only
explicit default content plus scenario-specific API setup. It enables AdminUI, Home, and UpMan.
The old integration scenarios retain their existing loopback fixture. Each browser has its own
profile, and different actors use separate browsers. Execution is sequential, with a five-minute
fork timeout and bounded browser waits. Port 2443 remains fixed: run simultaneous workers in
separate containers or network namespaces.

On failure, `target/selenium-failures/<test-class>-<unique-id>/` contains the test and fixture
identifier, screenshots, DOM, current URLs, browser console, capabilities (including browser and
driver versions), and server log. Capture and cleanup errors are attached to the original failure.
JUnit XML and per-execution timings are in `target/surefire-reports/`. Retain the Maven log on
failure; delete it after a successful run once it is no longer needed.

Scheduling, sharding, notification capture, broader feature coverage, and unattended reliability
validation remain future work.
