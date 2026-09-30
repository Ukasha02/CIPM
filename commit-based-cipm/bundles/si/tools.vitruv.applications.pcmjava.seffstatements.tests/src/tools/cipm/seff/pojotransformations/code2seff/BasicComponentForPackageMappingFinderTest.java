package tools.cipm.seff.pojotransformations.code2seff;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.emftext.language.java.classifiers.Class;
import org.emftext.language.java.classifiers.ClassifiersFactory;
import org.emftext.language.java.containers.CompilationUnit;
import org.emftext.language.java.containers.ContainersFactory;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.cipm.seff.testutil.JavaResourceRegistration;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link BasicComponentForPackageMappingFinder}. Its main lookup walks up the method's
 * package hierarchy one segment at a time, re-using the same internally-built Package object
 * at every step - that object is never exposed to callers, so a test can't reliably register
 * a "found at this level" correspondence against it without guessing at internals. Rather than
 * write a test built on a guess, this class covers only the two paths fully reliable from
 * outside: no containing compilation unit at all, and a full walk up to the root with nothing
 * found anywhere (which still proves the recursion terminates cleanly rather than looping or
 * throwing).
 */
class BasicComponentForPackageMappingFinderTest {

	private final BasicComponentForPackageMappingFinder finder = new BasicComponentForPackageMappingFinder();

	/**
	 * The finder builds its own internal ResourceSet (createPackage's "dummy resource", used to
	 * give the package object a resolvable URI). That ResourceSet - like any ResourceSet that
	 * doesn't set up its own local factory registry - falls back to the shared, JVM-wide
	 * Resource.Factory.Registry.INSTANCE for anything not explicitly overridden. In a full IDE
	 * launch, some bundle's Activator registers a .java factory there automatically; in this
	 * headless test run (useUIHarness=false) nothing does, so createResource(...) would
	 * otherwise silently return null. Registering it here, once, covers both the finder's
	 * internal ResourceSet and the one built directly in these tests.
	 */
	@BeforeAll
	static void registerJavaResourceFactoryGlobally() {
		JavaResourceRegistration.ensureJavaExtensionRegistered();
	}

	@Test
	void methodHasNoContainingCompilationUnit_returnsNull(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod detachedMethod = MembersFactory.eINSTANCE.createClassMethod();

		assertNull(finder.findBasicComponentForMethod(detachedMethod, view));
	}

	@Test
	void noCorrespondenceAnywhereInThePackageHierarchy_returnsNullAfterWalkingToTheRoot(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		Class containingClass = ClassifiersFactory.eINSTANCE.createClass();
		containingClass.getMembers().add(method);
		CompilationUnit compilationUnit = ContainersFactory.eINSTANCE.createCompilationUnit();
		compilationUnit.getNamespaces().addAll(List.of("com", "example"));
		compilationUnit.getClassifiers().add(containingClass);
		// A real backing resource is required - createPackage(...) reads cu.eResource().getURI().
		// Resolves via the factory registered globally in registerJavaResourceFactoryGlobally().
		ResourceSetImpl resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.createResource(URI.createURI("test:/com/example/Foo.java"));
		resource.getContents().add(compilationUnit);

		// No correspondences registered anywhere - the walk from ["com", "example"] down to
		// [] must terminate cleanly with null, not loop forever or throw.
		assertNull(finder.findBasicComponentForMethod(method, view));
	}
}
