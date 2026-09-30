package tools.cipm.seff.testutil;

import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;

import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.model.CorrespondenceModelFactory;
import tools.vitruv.change.correspondence.model.PersistableCorrespondenceModel;
import tools.vitruv.change.correspondence.view.CorrespondenceModelViewFactory;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Builds a real (not faked) correspondence model view backed by a temporary file, for tests
 * that need to register or look up correspondences. Uses Vitruv's own factories - this is
 * cheap enough, and exercises real semantics, that faking it was never worthwhile (see the
 * unit test plan's Decision 1).
 */
public final class CorrespondenceModelViews {

	private CorrespondenceModelViews() {
	}

	/**
	 * Creates a fresh, empty, in-memory-backed correspondence view. Pass a JUnit 5
	 * {@code @TempDir}-provided directory; a new backing file is placed inside it.
	 */
	public static EditableCorrespondenceModelView<Correspondence> newEditableView(Path tempDir) {
		URI modelUri = URI.createFileURI(tempDir.resolve("correspondences.xmi").toString());
		PersistableCorrespondenceModel model = CorrespondenceModelFactory.createPersistableCorrespondenceModel(modelUri);
		return CorrespondenceModelViewFactory.createEditableCorrespondenceModelView(model);
	}
}
