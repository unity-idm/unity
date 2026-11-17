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
`admin-automation`, `home`, `upman`, `authentication`, `registration`, `association`, `saml`,
`oauth`, `introspection`, `public`, or `public-policies`. The runner fails when no tests are found.
Current coverage includes:

- Admin directory: constrained string-type lifecycle, valid/malformed attribute-type imports, mandatory
  attribute classes, nested group lifecycle, account disable/reactivate, password replacement, deletion,
  attribute-value editor round trips and invalid edits, and identity creation/removal with mapping checks.
- Admin authentication: six credential-definition editors and their information views, five local
  authenticator editors, required-field validation, credential-requirement lifecycle and user assignment,
  and realm settings/service references for all three remember-me policies.
- Admin settings: identity self-service limits and running build information.
- Admin automation: scheduled rule creation/editing, invalid schedules, and saved/ad-hoc execution
  with account-state and fresh-login checks for target and control users.
- Home: profile save/reload/reset and image upload/replacement, read-only authorization roles, scheduled
  removal with login cancellation, invalid grace periods, immediate removal/disable/block modes,
  verified email management, OTP/SMS/WebAuthn enrollment, MFA opt-in, trusted devices, trusted applications,
  sensitive-profile reauthentication, and account association.
- UpMan: project switching and member filtering, subgroup lifecycle/visibility, subproject delegation,
  project/subproject roles, member removal with self-removal cancellation, invitations and request review,
  and scope checks across all four tabs.
- Authentication and public flows: session expiry/logout, password renewal, email-password and SMS recovery, enquiries,
  signup validation, confirmation/invitation errors, policy renewal, and workflow completion redirects.
- Protocol flows: OAuth consent and device authorization, SAML consent and identity selection,
  remembered allow/deny decisions, redirected MFA retry, and attribute-introspection results/retry.
- Selected AdminUI follow-up: restricted contents-manager permissions, one public registration-form
  editor workflow, and one User Home service editor workflow.
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
workflow actions use the browser. Local notification facilities capture rendered email/SMS messages
while preserving production address resolution. Virtual authenticators provide WebAuthn credentials.
Password recovery captures the real CAPTCHA answer through a test-only Spring bean; image rendering,
refresh, and answer verification use the production implementation. The answer stays on the server.
A recovery-only fixture uses the backend's code-lifetime constructor to exercise expired codes
without a long wait. Valid-code cases use the production lifetime.
A small Polish/narrow-viewport pass covers Home, UpMan, signup, and interactive login language switching.

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

Home removal cases inspect account state and removal deadlines, then use fresh browsers to verify
login cancellation or rejection. UpMan cases preserve unrelated project/member snapshots and check
new-session management access after granting or revoking roles. Ordinary members, unrelated users,
and managers of another project exercise direct navigation to every UpMan tab.

The shared fixture uses an in-memory database, a fresh Spring context per execution, and only
explicit default content plus scenario-specific API setup. It enables AdminUI, Home, and UpMan.
The old integration scenarios retain their existing loopback fixture. Each browser has its own
profile, and different actors use separate browsers. Execution is sequential, with a five-minute
fork timeout and bounded browser waits. The initial nightly target is one sequential worker completing
within 60 minutes, using JUnit reports and existing failure artifacts. Port 2443 remains fixed;
run the regular and nightly browser suites separately. If parallel workers become useful later,
isolate them in separate containers or network namespaces.

On failure, `target/selenium-failures/<test-class>-<unique-id>/` contains the test and fixture
identifier, screenshots, DOM, current URLs, browser console, capabilities (including browser and
driver versions), and server log. Capture and cleanup errors are attached to the original failure.
JUnit XML and per-execution timings are in `target/surefire-reports/`. Retain the Maven log on
failure; delete it after a successful run once it is no longer needed.

## Optional screenshots for visual review

Add `-Dunity.selenium.screenshots=true` to either browser runner's Maven command to capture
named checkpoints, including successful cases. The default is disabled. Each execution usually
captures one meaningful screen; selected multi-screen workflows capture two. Parameterized
executions have separate directories, and captures can select another actor's browser or the
current popup. Capture waits for Flow requests, fonts, and two rendering frames. It preserves
the test's viewport and scroll position, including the narrow Polish layouts.

Each runner writes `target/selenium-screenshots/` containing:

- `index.html`: a gallery with the test, scenario, checkpoint, page URL, and browser version.
- `manifest.jsonl`: one JSON object per PNG, with the JUnit invocation ID, scenario, checkpoint,
  relative image path, URL, browser/version, and capture time for scripted or model-assisted review.
- `<test-class>/<invocation-id>/<sequence>-<checkpoint>.png`: viewport screenshots.

Clear this directory before a new capture run (or use Maven `clean`) to avoid mixing runs.
For example, after installing the same revision's application artifacts:

```sh
rm -rf ui-tests/target/selenium-screenshots
mvn -Pnightly-ui -pl ui-tests verify -Dunity.selenium.screenshots=true \
  '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' \
  -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost= > nightly-ui-build.log 2>&1
```

Archive the whole directory alongside the matching JUnit reports. The review mode reports capture
errors as test failures so a broken capture cannot silently produce an incomplete gallery.
Failure diagnostics continue to be collected independently in `target/selenium-failures/`.

After a major Vaadin update or periodically, a reviewer can use the manifest to group images by UI
family and inspect them with an image-capable, less expensive model. Look for missing styles/icons,
overlapping or clipped controls, unreadable text, unexpected blank areas, and broken dialog or narrow
layouts. Supply the checkpoint and scenario with each image; report suspected oddities with its
image path and confirm them in the browser. This is a qualitative review, without pixel baselines
or automatic visual pass/fail decisions.

When extending a test, call `screenshot("Meaningful checkpoint")` after the relevant assertions
and before saving, closing, or redirecting away from that screen. Use
`screenshot(otherBrowser, "Meaningful checkpoint")` for another actor. Avoid capturing each click.
Disabled capture performs no additional waits, browser commands, or file writes.

Successful browser client-certificate login is outside the selected scope. Missing-certificate
fallback is covered.
