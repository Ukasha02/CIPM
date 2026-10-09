package tools.cipm.seff.pojotransformations.code2seff;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.seff.ResourceDemandingInternalBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;

import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link ResourceDemandingBehaviourForClassMethodFinderForPackageMapping}. Implements a
 * SoMoX interface but never calls SoMoX - both public methods delegate to one private helper
 * doing pure correspondence-model lookups.
 */
class ResourceDemandingBehaviourForClassMethodFinderForPackageMappingTest {

	@TempDir
	Path tempDir;

	private EditableCorrespondenceModelView<Correspondence> view;
	private ClassMethod method;
	private ResourceDemandingBehaviourForClassMethodFinderForPackageMapping finder;

	/** Every test needs a method and a finder over a fresh view - only what the method corresponds to varies. */
	@BeforeEach
	void setUp() {
		view = CorrespondenceModelViews.newEditableView(tempDir);
		method = MembersFactory.eINSTANCE.createClassMethod();
		finder = new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(view);
	}

	/** A method with no corresponding SEFF returns null. */
	@Test
	void getCorrespondingRDSEFForClassMethod_noCorrespondence_returnsNull() {
		assertNull(finder.getCorrespondingRDSEFForClassMethod(method));
	}

	/** A method with exactly one corresponding SEFF returns it. */
	@Test
	void getCorrespondingRDSEFForClassMethod_oneCorresponds_returnsIt() {
		ResourceDemandingSEFF seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		view.addCorrespondenceBetween(method, seff, null);

		assertSame(seff, finder.getCorrespondingRDSEFForClassMethod(method));
	}

	/**
	 * No exact "which one wins" assertion - the order isn't a contract this class or
	 * CorrespondenceModelUtil documents, so asserting a specific winner would risk a flaky
	 * test. What matters, and is verified: it picks one of the genuine candidates rather than
	 * crashing (the warning-logged path).
	 */
	@Test
	void getCorrespondingRDSEFForClassMethod_multipleCorrespond_returnsOneOfThem() {
		ResourceDemandingSEFF first = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		ResourceDemandingSEFF second = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		view.addCorrespondenceBetween(method, first, null);
		view.addCorrespondenceBetween(method, second, null);

		assertTrue(List.of(first, second).contains(finder.getCorrespondingRDSEFForClassMethod(method)));
	}

	/** A method with no corresponding internal behaviour returns null. */
	@Test
	void getCorrespondingResourceDemandingInternalBehaviour_noCorrespondence_returnsNull() {
		assertNull(finder.getCorrespondingResourceDemandingInternalBehaviour(method));
	}

	/** A method with exactly one corresponding internal behaviour returns it. */
	@Test
	void getCorrespondingResourceDemandingInternalBehaviour_oneCorresponds_returnsIt() {
		ResourceDemandingInternalBehaviour internalBehaviour = SeffFactory.eINSTANCE
				.createResourceDemandingInternalBehaviour();
		view.addCorrespondenceBetween(method, internalBehaviour, null);

		assertSame(internalBehaviour, finder.getCorrespondingResourceDemandingInternalBehaviour(method));
	}

	/**
	 * Both lookups share one private helper - this proves that sharing doesn't cross-contaminate
	 * results: with a method corresponding to both a SEFF and an internal behaviour at once,
	 * each public method must still return only its own type, not the other one.
	 */
	@Test
	void methodCorrespondsToBothTypes_eachLookupReturnsOnlyItsOwnType() {
		ResourceDemandingSEFF seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		ResourceDemandingInternalBehaviour internalBehaviour = SeffFactory.eINSTANCE
				.createResourceDemandingInternalBehaviour();
		view.addCorrespondenceBetween(method, seff, null);
		view.addCorrespondenceBetween(method, internalBehaviour, null);

		assertSame(seff, finder.getCorrespondingRDSEFForClassMethod(method));
		assertSame(internalBehaviour, finder.getCorrespondingResourceDemandingInternalBehaviour(method));
	}
}
