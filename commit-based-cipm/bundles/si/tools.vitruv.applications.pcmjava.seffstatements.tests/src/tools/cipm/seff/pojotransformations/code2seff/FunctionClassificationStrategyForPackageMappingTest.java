package tools.cipm.seff.pojotransformations.code2seff;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.InterfaceMethod;
import org.emftext.language.java.members.MembersFactory;
import org.emftext.language.java.members.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.RepositoryFactory;

import tools.cipm.seff.BasicComponentFinding;
import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.cipm.seff.testutil.TestModelObjects;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link FunctionClassificationStrategyForPackageMapping#isExternalCall} and
 * {@link FunctionClassificationStrategyForPackageMapping#isLibraryCall}. Both are protected,
 * SoMoX-facing methods - reached here via a tiny test-only subclass that exposes them as
 * public, the same "expose a protected method" pattern already sketched (unused) elsewhere in
 * this codebase. Neither method calls into real SoMoX behaviour - both are pure
 * correspondence-model / BasicComponentFinding logic, so a hand-written one-line fake for
 * BasicComponentFinding is enough; no SoMoX object is ever touched.
 */
class FunctionClassificationStrategyForPackageMappingTest {

	/** Exposes the two protected methods under test as public, changing nothing else. */
	private static class ExposedStrategy extends FunctionClassificationStrategyForPackageMapping {
		ExposedStrategy(BasicComponentFinding basicComponentFinding,
				EditableCorrespondenceModelView<Correspondence> correspondenceModel, BasicComponent basicComponent) {
			super(basicComponentFinding, correspondenceModel, basicComponent);
		}

		boolean callIsExternalCall(Method method) {
			return isExternalCall(method);
		}

		boolean callIsLibraryCall(Method method) {
			return isLibraryCall(method);
		}
	}

	// ---- isExternalCall ----

	/**
	 * A method that corresponds to an OperationSignature is external - the signature
	 * correspondence short-circuits before the component finder is ever consulted.
	 */
	@Test
	void methodCorrespondsToAnOperationSignature_isExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		OperationSignature signature = RepositoryFactory.eINSTANCE.createOperationSignature();
		view.addCorrespondenceBetween(method, signature, null);
		// Never consulted for this scenario: the signature correspondence short-circuits first.
		BasicComponentFinding unusedFinder = (m, correspondenceModel) -> {
			throw new AssertionError("should not be called when a signature correspondence already matched");
		};
		ExposedStrategy strategy = new ExposedStrategy(unusedFinder, view, TestModelObjects.ownComponent());

		assertTrue(strategy.callIsExternalCall(method));
	}

	/** A method whose own component differs from the strategy's component is external. */
	@Test
	void methodBelongsToADifferentComponent_isExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponent otherComponent = TestModelObjects.otherComponent();
		BasicComponentFinding finder = (m, correspondenceModel) -> otherComponent;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertTrue(strategy.callIsExternalCall(method));
	}

	/** A method whose own component is the strategy's own component is not external. */
	@Test
	void methodBelongsToTheOwnComponent_isNotExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponentFinding finder = (m, correspondenceModel) -> TestModelObjects.ownComponent();
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertFalse(strategy.callIsExternalCall(method));
	}

	/** A method whose component cannot be found at all is not external. */
	@Test
	void methodsComponentCannotBeFound_isNotExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertFalse(strategy.callIsExternalCall(method));
	}

	/** A method that isn't a ClassMethod is not external - the instanceof check fails before the finder is ever consulted. */
	@Test
	void methodIsNotAClassMethod_isNotExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		InterfaceMethod method = MembersFactory.eINSTANCE.createInterfaceMethod();
		// Never consulted: the instanceof ClassMethod check fails before the finder is used.
		BasicComponentFinding unusedFinder = (m, correspondenceModel) -> {
			throw new AssertionError("should not be called for a non-ClassMethod");
		};
		ExposedStrategy strategy = new ExposedStrategy(unusedFinder, view, TestModelObjects.ownComponent());

		assertFalse(strategy.callIsExternalCall(method));
	}

	// ---- isLibraryCall ----

	/** A method whose component cannot be found at all is classified as a library call. */
	@Test
	void methodsComponentCannotBeFound_isLibraryCall(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertTrue(strategy.callIsLibraryCall(method));
	}

	/** A method whose own component is the strategy's own component is not a library call. */
	@Test
	void methodBelongsToTheOwnComponent_isNotLibraryCall(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponentFinding finder = (m, correspondenceModel) -> TestModelObjects.ownComponent();
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertFalse(strategy.callIsLibraryCall(method));
	}

	/**
	 * Current code still returns true here, logging a warning that this "should not happen" -
	 * characterizing that behaviour, not asserting it's the ideal outcome.
	 */
	@Test
	void methodBelongsToADifferentComponent_isLibraryCallWithWarning(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponent otherComponent = TestModelObjects.otherComponent();
		BasicComponentFinding finder = (m, correspondenceModel) -> otherComponent;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertTrue(strategy.callIsLibraryCall(method));
	}
}
