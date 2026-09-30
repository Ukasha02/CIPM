/**
 * Test-only helpers shared across the {@code tools.cipm.seff} test suite.
 *
 * <p>{@link tools.cipm.seff.testutil.CorrespondenceModelViews} builds a real, temp-file-backed
 * correspondence view - the canonical way any test in this suite should obtain one.
 *
 * <p>{@link tools.cipm.seff.testutil.TestModelObjects} builds small PCM/SoMoX domain objects
 * (an id-only BasicComponent or OperationInterface, an empty SourceCodeDecoratorRepository)
 * that tests need but production code has no reason to construct.
 *
 * <p>{@link tools.cipm.seff.testutil.JavaResourceRegistration} registers the ".java" resource
 * factory needed by tests that build a real .java-backed EMF Resource in a headless run.
 *
 * <p>Not shared here, deliberately: several test classes expose a protected production method
 * via a small private test-only subclass (e.g. {@code ExposedStrategy} in
 * {@code FunctionClassificationStrategyForPackageMappingTest} and its sibling in
 * {@code FunctionClassificationStrategyForCommitIntegrationTest}; the
 * {@code SoMoXFreeTransformation}-shaped subclasses in
 * {@code ClassMethodBodyChangedTransformationTest} and its {@code extended}/{@code finegrained}
 * siblings). Each such subclass extends a genuinely different production superclass with a
 * different constructor and a different exposed-method set, so each is written out per test
 * class rather than forced into one shared generic base.
 */
package tools.cipm.seff.testutil;
