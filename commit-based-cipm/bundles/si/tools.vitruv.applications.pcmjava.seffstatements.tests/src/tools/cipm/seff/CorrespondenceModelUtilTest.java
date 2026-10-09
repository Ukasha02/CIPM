package tools.cipm.seff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.junit.jupiter.api.BeforeEach;
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
 * throughout - the "" vs null tag question is still an open design question and out of scope
 * here.
 */
class CorrespondenceModelUtilTest {

	@TempDir
	Path tempDir;

	private EditableCorrespondenceModelView<Correspondence> view;
	private ClassMethod method;
	private BasicComponent component;

	/** Every test needs a method corresponding to a component - what's checked on top of that varies. */
	@BeforeEach
	void setUp() {
		view = CorrespondenceModelViews.newEditableView(tempDir);
		method = MembersFactory.eINSTANCE.createClassMethod();
		component = RepositoryFactory.eINSTANCE.createBasicComponent();
		view.addCorrespondenceBetween(method, component, null);
	}

	/** Returns the one corresponding object whose type matches what was asked for. */
	@Test
	void getCorrespondingEObjects_returnsTheCorrespondingObjectOfTheRequestedType() {
		var result = CorrespondenceModelUtil.getCorrespondingEObjects(view, method, BasicComponent.class);

		assertEquals(1, result.size());
		assertTrue(result.contains(component));
	}

	/** A second correspondence of a different, unrequested type on the same object is ignored. */
	@Test
	void getCorrespondingEObjects_ignoresCorrespondencesOfAnUnrequestedType() {
		StartAction unrelatedTypeCorrespondence = SeffFactory.eINSTANCE.createStartAction();
		view.addCorrespondenceBetween(method, unrelatedTypeCorrespondence, null);

		var result = CorrespondenceModelUtil.getCorrespondingEObjects(view, method, BasicComponent.class);

		assertEquals(1, result.size());
		assertTrue(result.contains(component));
	}

	/** An object with no correspondences at all returns an empty list, not null. */
	@Test
	void getCorrespondingEObjects_noCorrespondences_returnsEmptyList() {
		ClassMethod methodWithNoCorrespondences = MembersFactory.eINSTANCE.createClassMethod();

		var result = CorrespondenceModelUtil.getCorrespondingEObjects(view, methodWithNoCorrespondences,
				BasicComponent.class);

		assertTrue(result.isEmpty());
	}

	/** After removal, the object has no correspondences left and a follow-up lookup finds nothing. */
	@Test
	void removeCorrespondencesFor_removesAllCorrespondencesOfThatObject() {
		CorrespondenceModelUtil.removeCorrespondencesFor(view, method);

		assertFalse(view.hasCorrespondences(method));
		assertTrue(CorrespondenceModelUtil.getCorrespondingEObjects(view, method, BasicComponent.class).isEmpty());
	}
}
