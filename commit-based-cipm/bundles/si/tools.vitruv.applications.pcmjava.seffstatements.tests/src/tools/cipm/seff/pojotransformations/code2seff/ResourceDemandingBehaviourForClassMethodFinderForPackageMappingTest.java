package tools.cipm.seff.pojotransformations.code2seff;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
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

	@Test
	void getCorrespondingRDSEFForClassMethod_noCorrespondence_returnsNull(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		var finder = new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(view);

		assertNull(finder.getCorrespondingRDSEFForClassMethod(method));
	}

	@Test
	void getCorrespondingRDSEFForClassMethod_oneCorresponds_returnsIt(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		ResourceDemandingSEFF seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		view.addCorrespondenceBetween(method, seff, null);
		var finder = new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(view);

		assertSame(seff, finder.getCorrespondingRDSEFForClassMethod(method));
	}

	/**
	 * No exact "which one wins" assertion - the order isn't a contract this class or
	 * CorrespondenceModelUtil documents, so asserting a specific winner would risk a flaky
	 * test. What matters, and is verified: it picks one of the genuine candidates rather than
	 * crashing (the warning-logged path).
	 */
	@Test
	void getCorrespondingRDSEFForClassMethod_multipleCorrespond_returnsOneOfThem(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		ResourceDemandingSEFF first = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		ResourceDemandingSEFF second = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		view.addCorrespondenceBetween(method, first, null);
		view.addCorrespondenceBetween(method, second, null);
		var finder = new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(view);

		assertTrue(List.of(first, second).contains(finder.getCorrespondingRDSEFForClassMethod(method)));
	}

	@Test
	void getCorrespondingResourceDemandingInternalBehaviour_noCorrespondence_returnsNull(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		var finder = new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(view);

		assertNull(finder.getCorrespondingResourceDemandingInternalBehaviour(method));
	}

	@Test
	void getCorrespondingResourceDemandingInternalBehaviour_oneCorresponds_returnsIt(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		ResourceDemandingInternalBehaviour internalBehaviour = SeffFactory.eINSTANCE
				.createResourceDemandingInternalBehaviour();
		view.addCorrespondenceBetween(method, internalBehaviour, null);
		var finder = new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(view);

		assertSame(internalBehaviour, finder.getCorrespondingResourceDemandingInternalBehaviour(method));
	}

	/**
	 * Both lookups share one private helper - this proves that sharing doesn't cross-contaminate
	 * results: with a method corresponding to both a SEFF and an internal behaviour at once,
	 * each public method must still return only its own type, not the other one.
	 */
	@Test
	void methodCorrespondsToBothTypes_eachLookupReturnsOnlyItsOwnType(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		ResourceDemandingSEFF seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		ResourceDemandingInternalBehaviour internalBehaviour = SeffFactory.eINSTANCE
				.createResourceDemandingInternalBehaviour();
		view.addCorrespondenceBetween(method, seff, null);
		view.addCorrespondenceBetween(method, internalBehaviour, null);
		var finder = new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(view);

		assertSame(seff, finder.getCorrespondingRDSEFForClassMethod(method));
		assertSame(internalBehaviour, finder.getCorrespondingResourceDemandingInternalBehaviour(method));
	}
}
