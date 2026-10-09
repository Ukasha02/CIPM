package tools.cipm.seff.pojotransformations.code2seff;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFinding;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFindingFactory;
import org.somox.sourcecodedecorator.SourceCodeDecoratorRepository;

import tools.cipm.seff.BasicComponentFinding;
import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.cipm.seff.testutil.TestModelObjects;
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

	@TempDir
	Path tempDir;

	/** Shared across the three tests that need them; the first test needs neither. */
	private EditableCorrespondenceModelView<Correspondence> view;
	private BasicComponent basicComponent;

	@BeforeEach
	void setUp() {
		view = CorrespondenceModelViews.newEditableView(tempDir);
		basicComponent = RepositoryFactory.eINSTANCE.createBasicComponent();
	}

	/** Returns the package-mapping-specific component finder. */
	@Test
	void createBasicComponentFinding_returnsThePackageMappingFinder() {
		assertInstanceOf(BasicComponentForPackageMappingFinder.class, factory.createBasicComponentFinding());
	}

	/** Returns a factory that itself produces the package-mapping-specific external-call finder. */
	@Test
	void createInterfaceOfExternalCallFindingFactory_producesAPackageMappingFinder() {
		SourceCodeDecoratorRepository sourceCodeDecoratorRepository = TestModelObjects
				.newSourceCodeDecoratorRepository();

		InterfaceOfExternalCallFindingFactory findingFactory = factory
				.createInterfaceOfExternalCallFindingFactory(view, basicComponent);
		InterfaceOfExternalCallFinding finding = findingFactory
				.createInterfaceOfExternalCallFinding(sourceCodeDecoratorRepository, basicComponent);

		assertInstanceOf(InterfaceOfExternalCallFinderForPackageMapping.class, finding);
	}

	/** Returns the package-mapping-specific behaviour finder. */
	@Test
	void createResourceDemandingBehaviourForClassMethodFinding_returnsThePackageMappingFinder() {
		assertInstanceOf(ResourceDemandingBehaviourForClassMethodFinderForPackageMapping.class,
				factory.createResourceDemandingBehaviourForClassMethodFinding(view));
	}

	/** Returns the package-mapping-specific classification strategy. */
	@Test
	void createAbstractFunctionClassificationStrategy_returnsThePackageMappingStrategy() {
		BasicComponentFinding unusedFinder = (method, correspondenceModel) -> null;

		assertInstanceOf(FunctionClassificationStrategyForPackageMapping.class,
				factory.createAbstractFunctionClassificationStrategy(view, unusedFinder, basicComponent));
	}
}
