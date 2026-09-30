package tools.cipm.seff.testutil;

import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.OperationInterface;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.somox.sourcecodedecorator.SourceCodeDecoratorRepository;
import org.somox.sourcecodedecorator.SourcecodedecoratorFactory;

/**
 * Small factory helpers for PCM repository / SoMoX source-code-decorator objects that tests
 * need but production code has no reason to construct directly (an id-only BasicComponent or
 * OperationInterface, an empty SourceCodeDecoratorRepository). Centralizes the
 * "eINSTANCE.createX(); x.setId(id); return x;" idiom that several test classes previously
 * duplicated, so it's written - and can be verified correct - exactly once.
 */
public final class TestModelObjects {

	private TestModelObjects() {
	}

	/** Id several tests use for the BasicComponent standing in for "our own component". */
	public static final String OWN_COMPONENT_ID = "own-component";

	/** Id several tests use for a BasicComponent that is deliberately not the own component. */
	public static final String OTHER_COMPONENT_ID = "other-component";

	/** Creates a BasicComponent with the given id and nothing else set. */
	public static BasicComponent componentWithId(String id) {
		BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
		component.setId(id);
		return component;
	}

	/** Convenience for {@code componentWithId(OWN_COMPONENT_ID)}. */
	public static BasicComponent ownComponent() {
		return componentWithId(OWN_COMPONENT_ID);
	}

	/** Convenience for {@code componentWithId(OTHER_COMPONENT_ID)}. */
	public static BasicComponent otherComponent() {
		return componentWithId(OTHER_COMPONENT_ID);
	}

	/** Creates an OperationInterface with the given id and nothing else set. */
	public static OperationInterface interfaceWithId(String id) {
		OperationInterface opInterface = RepositoryFactory.eINSTANCE.createOperationInterface();
		opInterface.setId(id);
		return opInterface;
	}

	/** Creates a fresh, empty SourceCodeDecoratorRepository. */
	public static SourceCodeDecoratorRepository newSourceCodeDecoratorRepository() {
		return SourcecodedecoratorFactory.eINSTANCE.createSourceCodeDecoratorRepository();
	}
}
