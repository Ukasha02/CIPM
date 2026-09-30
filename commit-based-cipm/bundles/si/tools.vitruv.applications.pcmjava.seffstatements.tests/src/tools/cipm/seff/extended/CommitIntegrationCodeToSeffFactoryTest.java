package tools.cipm.seff.extended;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFinding;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFindingFactory;
import org.somox.sourcecodedecorator.SourceCodeDecoratorRepository;

import tools.cipm.seff.BasicComponentFinding;
import tools.cipm.seff.pojotransformations.code2seff.InterfaceOfExternalCallFinderForPackageMapping;
import tools.cipm.seff.pojotransformations.code2seff.ResourceDemandingBehaviourForClassMethodFinderForPackageMapping;
import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.cipm.seff.testutil.TestModelObjects;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link CommitIntegrationCodeToSeffFactory}. Two of its four methods have their own
 * logic; the other two delegate to a held PojoJava2PcmCodeToSeffFactory instance instead of
 * duplicating it (commit C2's composition swap, replacing inheritance). These tests confirm
 * the delegated methods still produce the same concrete types they did before that swap.
 */
class CommitIntegrationCodeToSeffFactoryTest {

	private final CommitIntegrationCodeToSeffFactory factory = new CommitIntegrationCodeToSeffFactory();

	@Test
	void createBasicComponentFinding_returnsTheCommitIntegrationFinder() {
		assertInstanceOf(BasicComponentForCommitIntegrationFinder.class, factory.createBasicComponentFinding());
	}

	@Test
	void createInterfaceOfExternalCallFindingFactory_delegatesToThePackageMappingDefault(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		BasicComponent basicComponent = RepositoryFactory.eINSTANCE.createBasicComponent();
		SourceCodeDecoratorRepository sourceCodeDecoratorRepository = TestModelObjects
				.newSourceCodeDecoratorRepository();

		InterfaceOfExternalCallFindingFactory findingFactory = factory
				.createInterfaceOfExternalCallFindingFactory(view, basicComponent);
		InterfaceOfExternalCallFinding finding = findingFactory
				.createInterfaceOfExternalCallFinding(sourceCodeDecoratorRepository, basicComponent);

		assertInstanceOf(InterfaceOfExternalCallFinderForPackageMapping.class, finding);
	}

	@Test
	void createResourceDemandingBehaviourForClassMethodFinding_delegatesToThePackageMappingDefault(
			@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);

		assertInstanceOf(ResourceDemandingBehaviourForClassMethodFinderForPackageMapping.class,
				factory.createResourceDemandingBehaviourForClassMethodFinding(view));
	}

	@Test
	void createAbstractFunctionClassificationStrategy_returnsTheCommitIntegrationStrategy(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		BasicComponentFinding unusedFinder = (method, correspondenceModel) -> null;
		BasicComponent basicComponent = RepositoryFactory.eINSTANCE.createBasicComponent();

		assertInstanceOf(FunctionClassificationStrategyForCommitIntegration.class,
				factory.createAbstractFunctionClassificationStrategy(view, unusedFinder, basicComponent));
	}
}
