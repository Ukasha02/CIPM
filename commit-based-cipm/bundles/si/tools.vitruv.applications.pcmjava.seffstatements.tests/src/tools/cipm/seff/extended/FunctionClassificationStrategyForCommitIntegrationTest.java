package tools.cipm.seff.extended;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.emftext.language.java.classifiers.Class;
import org.emftext.language.java.classifiers.ClassifiersFactory;
import org.emftext.language.java.containers.CompilationUnit;
import org.emftext.language.java.containers.ContainersFactory;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.emftext.language.java.members.Method;
import org.junit.jupiter.api.BeforeEach;
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
 * Tests {@link FunctionClassificationStrategyForCommitIntegration#isExternalCall}. Two honest
 * limits on what's black-box testable here: readRestClientApiPackages() reads a process-wide
 * settings singleton that doesn't exist in a bare test, so the "package IS configured as a
 * REST client API" branch can never actually fire through the public constructor (it safely
 * defaults to an empty list, guarded against the missing singleton) - not attempted. And this method calls
 * method.getContainingCompilationUnit().getNamespacesAsString() with no null-check, unlike its
 * sibling classes elsewhere in this codebase that do guard the same kind of lookup - a real
 * latent NullPointerException risk, characterized below rather than worked around.
 */
class FunctionClassificationStrategyForCommitIntegrationTest {

	@TempDir
	Path tempDir;

	/** Every test needs a correspondence view - what's registered in it, and the method used, varies. */
	private EditableCorrespondenceModelView<Correspondence> view;

	@BeforeEach
	void setUp() {
		view = CorrespondenceModelViews.newEditableView(tempDir);
	}

	/** Exposes the protected isExternalCall under test as public, changing nothing else. */
	private static class ExposedStrategy extends FunctionClassificationStrategyForCommitIntegration {
		ExposedStrategy(BasicComponentFinding basicComponentFinding,
				EditableCorrespondenceModelView<Correspondence> correspondenceModel, BasicComponent basicComponent) {
			super(basicComponentFinding, correspondenceModel, basicComponent);
		}

		boolean callIsExternalCall(Method method) {
			return isExternalCall(method);
		}
	}

	/** A ClassMethod attached to a real CompilationUnit with the given package namespaces. */
	private static ClassMethod methodInPackage(List<String> namespaces) {
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		Class containingClass = ClassifiersFactory.eINSTANCE.createClass();
		containingClass.getMembers().add(method);
		CompilationUnit compilationUnit = ContainersFactory.eINSTANCE.createCompilationUnit();
		compilationUnit.getNamespaces().addAll(namespaces);
		compilationUnit.getClassifiers().add(containingClass);
		return method;
	}

	/**
	 * When the base class's own check already classifies the call as external (via a signature
	 * correspondence), the REST-package check is never even consulted.
	 */
	@Test
	void superClassifiesTheCallAsExternal_isExternal() {
		ClassMethod method = methodInPackage(List.of("com", "example"));
		OperationSignature signature = RepositoryFactory.eINSTANCE.createOperationSignature();
		view.addCorrespondenceBetween(method, signature, null);
		BasicComponentFinding unusedFinder = (m, correspondenceModel) -> {
			throw new AssertionError("super.isExternalCall already returned true; the REST-package check must not run");
		};
		ExposedStrategy strategy = new ExposedStrategy(unusedFinder, view, TestModelObjects.ownComponent());

		assertTrue(strategy.callIsExternalCall(method));
	}

	/** A method with no package namespace at all is not external. */
	@Test
	void methodHasNoNamespace_isNotExternal() {
		ClassMethod method = methodInPackage(List.of());
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertFalse(strategy.callIsExternalCall(method));
	}

	/** A method whose package isn't configured as a REST client API is not external. */
	@Test
	void methodsPackageIsNotAConfiguredRestClientPackage_isNotExternal() {
		ClassMethod method = methodInPackage(List.of("com", "example"));
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertFalse(strategy.callIsExternalCall(method));
	}

	/**
	 * Characterizes a real gap: unlike BasicComponentForPackageMappingFinder (which explicitly
	 * checks for a null containing compilation unit before using it), this method does not -
	 * so a method with no containing compilation unit throws, rather than returning false.
	 * Recorded as current behaviour, not asserted to be intended.
	 */
	@Test
	void methodHasNoContainingCompilationUnit_throwsNullPointerException() {
		ClassMethod detachedMethod = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, TestModelObjects.ownComponent());

		assertThrows(NullPointerException.class, () -> strategy.callIsExternalCall(detachedMethod));
	}
}
