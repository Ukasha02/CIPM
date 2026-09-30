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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.RepositoryFactory;

import tools.cipm.seff.BasicComponentFinding;
import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link FunctionClassificationStrategyForCommitIntegration#isExternalCall}. Two honest
 * limits on what's black-box testable here: readRestClientApiPackages() reads a process-wide
 * settings singleton that doesn't exist in a bare test, so the "package IS configured as a
 * REST client API" branch can never actually fire through the public constructor (it safely
 * defaults to an empty list, per commit A5's guard) - not attempted. And this method calls
 * method.getContainingCompilationUnit().getNamespacesAsString() with no null-check, unlike its
 * sibling classes elsewhere in this codebase that do guard the same kind of lookup - a real
 * latent NullPointerException risk, characterized below rather than worked around.
 */
class FunctionClassificationStrategyForCommitIntegrationTest {

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

	@Test
	void superClassifiesTheCallAsExternal_isExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodInPackage(List.of("com", "example"));
		OperationSignature signature = RepositoryFactory.eINSTANCE.createOperationSignature();
		view.addCorrespondenceBetween(method, signature, null);
		BasicComponentFinding unusedFinder = (m, correspondenceModel) -> {
			throw new AssertionError("super.isExternalCall already returned true; the REST-package check must not run");
		};
		ExposedStrategy strategy = new ExposedStrategy(unusedFinder, view, componentWithId("own-component"));

		assertTrue(strategy.callIsExternalCall(method));
	}

	@Test
	void methodHasNoNamespace_isNotExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodInPackage(List.of());
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, componentWithId("own-component"));

		assertFalse(strategy.callIsExternalCall(method));
	}

	@Test
	void methodsPackageIsNotAConfiguredRestClientPackage_isNotExternal(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodInPackage(List.of("com", "example"));
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, componentWithId("own-component"));

		assertFalse(strategy.callIsExternalCall(method));
	}

	/**
	 * Characterizes a real gap: unlike BasicComponentForPackageMappingFinder (which explicitly
	 * checks for a null containing compilation unit before using it), this method does not -
	 * so a method with no containing compilation unit throws, rather than returning false.
	 * Recorded as current behaviour, not asserted to be intended.
	 */
	@Test
	void methodHasNoContainingCompilationUnit_throwsNullPointerException(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod detachedMethod = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		ExposedStrategy strategy = new ExposedStrategy(finder, view, componentWithId("own-component"));

		assertThrows(NullPointerException.class, () -> strategy.callIsExternalCall(detachedMethod));
	}

	private static BasicComponent componentWithId(String id) {
		BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
		component.setId(id);
		return component;
	}
}
