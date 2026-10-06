package tools.cipm.seff.extended;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;

import org.emftext.language.java.classifiers.Class;
import org.emftext.language.java.classifiers.ClassifiersFactory;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;

import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link BasicComponentForCommitIntegrationFinder}. Its lookup key is the method's
 * *containing class*, not the method itself - so the correspondence has to be registered on
 * the class in these tests, matching what the finder actually queries for.
 */
class BasicComponentForCommitIntegrationFinderTest {

	private final BasicComponentForCommitIntegrationFinder finder = new BasicComponentForCommitIntegrationFinder();

	/** When the method's containing class corresponds to a component, that component is returned. */
	@Test
	void methodsContainingClassCorrespondsToAComponent_returnsThatComponent(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		Class containingClass = ClassifiersFactory.eINSTANCE.createClass();
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		containingClass.getMembers().add(method);
		BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
		view.addCorrespondenceBetween(containingClass, component, null);

		assertSame(component, finder.findBasicComponentForMethod(method, view));
	}

	/** When the containing class has no corresponding component, the lookup returns null. */
	@Test
	void noCorrespondenceForTheContainingClass_returnsNull(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		Class containingClass = ClassifiersFactory.eINSTANCE.createClass();
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		containingClass.getMembers().add(method);

		assertNull(finder.findBasicComponentForMethod(method, view));
	}
}
