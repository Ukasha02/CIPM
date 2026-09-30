package tools.cipm.seff.finegrained;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.SeffFactory;

/**
 * Tests {@link AbstractActionMatching}: the constructor wires both actions through to their
 * getters, unchanged, for the immutable pair commit A1 established.
 */
class AbstractActionMatchingTest {

	@Test
	void constructorArguments_areReturnedUnchangedByTheGetters() {
		AbstractAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();

		AbstractActionMatching matching = new AbstractActionMatching(newAction, oldAction);

		assertSame(newAction, matching.getNewAbstractAction());
		assertSame(oldAction, matching.getOldAbstractAction());
	}
}
