package tools.cipm.seff.pojotransformations.code2seff;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;

import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.OperationInterface;
import org.palladiosimulator.pcm.repository.OperationRequiredRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFinding.InterfacePortOperationTuple;

import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.cipm.seff.testutil.TestModelObjects;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Characterization tests for {@link InterfaceOfExternalCallFinderForPackageMapping}. This
 * class is intentionally left unedited here (kept exactly as it is for now, pending further
 * review), following the investigation into real TEAMMATES output where 57/507 ExternalCallActions
 * came back with both fields missing. These tests exist to make that finding concrete and
 * reproducible - not to fix or improve the behaviour, only to record it precisely.
 *
 * <p>Confirmed here: the class has two genuinely different "not found" shapes, not one -
 * see {@link #methodCorrespondsToASignatureButNoMatchingRequiredRoleExists_signatureSetRoleNull}
 * vs {@link #methodHasNoCorrespondenceAtAll_bothFieldsNull}. The {@code Statement} parameter on
 * {@code getCalledInterfacePort} is never read by the implementation, so {@code null} is passed
 * for it throughout.
 */
class InterfaceOfExternalCallFinderForPackageMappingTest {

	/** Id of an interface that no component in these tests ever declares it requires. */
	private static final String INTERFACE_NOBODY_REQUIRES = "interface-nobody-requires";

	/**
	 * The fully-successful case: a real signature correspondence exists and the component
	 * genuinely requires that interface, so both fields come back populated. The baseline
	 * every other case in this class is contrasted against.
	 */
	@Test
	void methodCorrespondsToASignatureWithAMatchingRequiredRole_bothFieldsPopulated(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		OperationInterface opInterface = TestModelObjects.interfaceWithId("shared-interface");
		OperationSignature signature = RepositoryFactory.eINSTANCE.createOperationSignature();
		signature.setInterface__OperationSignature(opInterface);
		view.addCorrespondenceBetween(method, signature, null);

		OperationRequiredRole matchingRole = RepositoryFactory.eINSTANCE.createOperationRequiredRole();
		matchingRole.setRequiredInterface__OperationRequiredRole(opInterface);
		BasicComponent ownComponent = RepositoryFactory.eINSTANCE.createBasicComponent();
		ownComponent.getRequiredRoles_InterfaceRequiringEntity().add(matchingRole);

		var finder = new InterfaceOfExternalCallFinderForPackageMapping(view, ownComponent);

		InterfacePortOperationTuple result = finder.getCalledInterfacePort(method, null);

		assertSame(signature, result.signature);
		assertSame(matchingRole, result.role);
	}

	/**
	 * The "half-empty" not-found shape: a signature correspondence exists, but the component
	 * doesn't require the interface that signature belongs to, so no matching role is ever
	 * found. signature ends up set while role stays null - a different shape from the fully
	 * empty tuple below, for what is arguably the same underlying situation ("not found").
	 */
	@Test
	void methodCorrespondsToASignatureButNoMatchingRequiredRoleExists_signatureSetRoleNull(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		OperationSignature signature = RepositoryFactory.eINSTANCE.createOperationSignature();
		signature.setInterface__OperationSignature(TestModelObjects.interfaceWithId(INTERFACE_NOBODY_REQUIRES));
		view.addCorrespondenceBetween(method, signature, null);

		// The component requires no interfaces at all - deliberately no matching role possible.
		BasicComponent ownComponent = RepositoryFactory.eINSTANCE.createBasicComponent();

		var finder = new InterfaceOfExternalCallFinderForPackageMapping(view, ownComponent);

		InterfacePortOperationTuple result = finder.getCalledInterfacePort(method, null);

		assertSame(signature, result.signature);
		assertNull(result.role);
	}

	/**
	 * The "fully empty" not-found shape: no correspondence of any kind exists for the method,
	 * so neither field is ever set. Contrast with the "half-empty" case above - same underlying
	 * "not found" situation, a genuinely different result shape.
	 */
	@Test
	void methodHasNoCorrespondenceAtAll_bothFieldsNull(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponent ownComponent = RepositoryFactory.eINSTANCE.createBasicComponent();
		var finder = new InterfaceOfExternalCallFinderForPackageMapping(view, ownComponent);

		InterfacePortOperationTuple result = finder.getCalledInterfacePort(method, null);

		assertNull(result.signature);
		assertNull(result.role);
	}

	/**
	 * The documented fallback path: when the method has no direct OperationSignature
	 * correspondence, but does correspond to a SEFF whose described service already is one,
	 * that signature is used instead.
	 */
	@Test
	void methodCorrespondsOnlyToASeffWhoseDescribedServiceIsASignature_signatureFoundViaSeff(
			@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		OperationSignature signature = RepositoryFactory.eINSTANCE.createOperationSignature();
		signature.setInterface__OperationSignature(TestModelObjects.interfaceWithId(INTERFACE_NOBODY_REQUIRES));
		ResourceDemandingSEFF seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		seff.setDescribedService__SEFF(signature);
		view.addCorrespondenceBetween(method, seff, null);

		BasicComponent ownComponent = RepositoryFactory.eINSTANCE.createBasicComponent();
		var finder = new InterfaceOfExternalCallFinderForPackageMapping(view, ownComponent);

		InterfacePortOperationTuple result = finder.getCalledInterfacePort(method, null);

		assertSame(signature, result.signature);
	}
}
