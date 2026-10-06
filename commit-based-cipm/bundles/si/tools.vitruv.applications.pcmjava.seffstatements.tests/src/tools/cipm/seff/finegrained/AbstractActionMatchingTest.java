package tools.cipm.seff.finegrained;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.SeffFactory;

/**
 * Tests {@link AbstractActionMatching}: the constructor wires both actions through to their
 * getters, unchanged, for the immutable pair this class represents.
 */
class AbstractActionMatchingTest {

	/** The constructor's two arguments come back unchanged from their respective getters. */
	@Test
	void constructorArguments_areReturnedUnchangedByTheGetters() {
		AbstractAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();

		AbstractActionMatching matching = new AbstractActionMatching(newAction, oldAction);

		assertSame(newAction, matching.getNewAbstractAction());
		assertSame(oldAction, matching.getOldAbstractAction());
	}
}
