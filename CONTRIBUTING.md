# Contributing

Thank you for your interest in contributing to Quilt Enigma Plugin!

We recommend discussing your contribution with other members of the community - either directly in your pull request,
or in our other community spaces. We're always happy to help if you need us!

### Proposers

Proposers should generate names that
- follow [Quilt Mappings conventions](https://github.com/QuiltMC/quilt-mappings/blob/25w06a/CONVENTIONS.md)
- consistently generate high-quality names

Proposed names contribute to mapping stats, so it's better to leave things unmapped and easier to find
than to propose names that need to be manually overriden.

### Tests

All proposers should be tested by an independent test class in `src/test/` in the same package as the proposer class.

Most tests will require an obfuscated jar as input. *Do not* use other tests' jars or input classes;
each test should be completely independent.

To create the source for your obfuscated jar, create a directory under `src/testInputs/` named after your
proposer class converted to `lowerCamelCase`, for example `MyNameProposer` -> `myNameProposer`.  

Gradle will automatically create a source set from your directory and setup task dependencies to ensure your obfuscated
jar is present for tests.

Create input classes in your new test input source set,
for example `src/testInputs/myNameProposer/java/com/example/MyProposerTestInput.java`

Now on your test class in `src/test/`, implement `ConventionalNameProposerTest` and implement its
`getTarget()` and `getTargetId()` methods so that they return the class and identifier of your proposer.

Finally, in each test method, use `createAsserter()` to create a `ProposalAsserter` for your obfuscated jar.  
Use the asserter to assert the presence/absence of mappings, and use its `remapper()` if you need to manually
insert mappings in your test.

### Descriptors, entries, and keeping track of obfuscated names
When asserting mappings, you'll need to create obfuscated `Entry`s representing the obfuscated name of the
class/member you're checking.  
`TestUtil` has factory methods to streamline this process, and `CommonDescriptors` has reusable descriptors to pass to them.  

You'll also likely need some of the obfuscated names of your test input classes. You can find them in the txt mapping file in
`build/test-obf/` which is created by your test input obf task
(ex: `myNameProposerTestObf` would create `myNameProposer-test-obf.txt`).

However if you add or edit test input classes, obfuscated names may need to be updated.  
Use variable names and comments to make it easier to track obfuscated names in your test class back to their test input sources.  
For example, you might create a constant `private static final String MY_PROPOSER_TEST_INPUT_NAME = "a/a/a";` where the field name
makes it clear that it represents the obfuscated name of `MyProposerTestInput`.
