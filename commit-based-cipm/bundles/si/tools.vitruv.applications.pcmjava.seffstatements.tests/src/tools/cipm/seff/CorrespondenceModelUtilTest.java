package tools.cipm.seff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.StartAction;

import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link CorrespondenceModelUtil} against a real, in-memory correspondence model
 * (Vitruv's own CorrespondenceModelFactory / CorrespondenceModelViewFactory) rather than a
 * fake - this dependency has real behaviour worth exercising (getCorrespondingEObjects uses
 * parallelStream internally), and building one is cheap. Only the default (null) tag is used
 * throughout - the "" vs null tag question is still open with the supervisor and out of scope
 * here.
 */
class CorrespondenceModelUtilTest {

	@Test
	void getCorrespondingEObjects_returnsTheCorrespondingObjectOfTheRequestedType(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
		view.addCorrespondenceBetween(method, component, null);

		var result = CorrespondenceModelUtil.getCorrespondingEObjects(view, method, BasicComponent.class);

		assertEquals(1, result.size());
		assertTrue(result.contains(component));
	}

	@Test
	void getCorrespondingEObjects_ignoresCorrespondencesOfAnUnrequestedType(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
		StartAction unrelatedTypeCorrespondence = SeffFactory.eINSTANCE.createStartAction();
		view.addCorrespondenceBetween(method, component, null);
		view.addCorrespondenceBetween(method, unrelatedTypeCorrespondence, null);

		var result = CorrespondenceModelUtil.getCorrespondingEObjects(view, method, BasicComponent.class);

		assertEquals(1, result.size());
		assertTrue(result.contains(component));
	}

	@Test
	void getCorrespondingEObjects_noCorrespondences_returnsEmptyList(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod methodWithNoCorrespondences = MembersFactory.eINSTANCE.createClassMethod();

		var result = CorrespondenceModelUtil.getCorrespondingEObjects(view, methodWithNoCorrespondences,
				BasicComponent.class);

		assertTrue(result.isEmpty());
	}

	@Test
	void removeCorrespondencesFor_removesAllCorrespondencesOfThatObject(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
		view.addCorrespondenceBetween(method, component, null);

		CorrespondenceModelUtil.removeCorrespondencesFor(view, method);

		assertFalse(view.hasCorrespondences(method));
		assertTrue(CorrespondenceModelUtil.getCorrespondingEObjects(view, method, BasicComponent.class).isEmpty());
	}
}
