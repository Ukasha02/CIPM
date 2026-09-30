package tools.cipm.seff.pojotransformations.code2seff;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFinding;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFindingFactory;
import org.somox.sourcecodedecorator.SourceCodeDecoratorRepository;
import org.somox.sourcecodedecorator.SourcecodedecoratorFactory;

import tools.cipm.seff.BasicComponentFinding;
import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link PojoJava2PcmCodeToSeffFactory}: a pure "wiring" class with no logic of its own,
 * so every test here asks the same question - does this factory method hand back an instance
 * of the correct concrete type for the package-mapping variant - rather than checking any
 * behaviour.
 */
class PojoJava2PcmCodeToSeffFactoryTest {

	private final PojoJava2PcmCodeToSeffFactory factory = new PojoJava2PcmCodeToSeffFactory();

	@Test
	void createBasicComponentFinding_returnsThePackageMappingFinder() {
		assertInstanceOf(BasicComponentForPackageMappingFinder.class, factory.createBasicComponentFinding());
	}

	@Test
	void createInterfaceOfExternalCallFindingFactory_producesAPackageMappingFinder(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		BasicComponent basicComponent = RepositoryFactory.eINSTANCE.createBasicComponent();
		SourceCodeDecoratorRepository sourceCodeDecoratorRepository = SourcecodedecoratorFactory.eINSTANCE
				.createSourceCodeDecoratorRepository();

		InterfaceOfExternalCallFindingFactory findingFactory = factory
				.createInterfaceOfExternalCallFindingFactory(view, basicComponent);
		InterfaceOfExternalCallFinding finding = findingFactory
				.createInterfaceOfExternalCallFinding(sourceCodeDecoratorRepository, basicComponent);

		assertInstanceOf(InterfaceOfExternalCallFinderForPackageMapping.class, finding);
	}

	@Test
	void createResourceDemandingBehaviourForClassMethodFinding_returnsThePackageMappingFinder(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);

		assertInstanceOf(ResourceDemandingBehaviourForClassMethodFinderForPackageMapping.class,
				factory.createResourceDemandingBehaviourForClassMethodFinding(view));
	}

	@Test
	void createAbstractFunctionClassificationStrategy_returnsThePackageMappingStrategy(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		BasicComponentFinding unusedFinder = (method, correspondenceModel) -> null;
		BasicComponent basicComponent = RepositoryFactory.eINSTANCE.createBasicComponent();

		assertInstanceOf(FunctionClassificationStrategyForPackageMapping.class,
				factory.createAbstractFunctionClassificationStrategy(view, unusedFinder, basicComponent));
	}
}
