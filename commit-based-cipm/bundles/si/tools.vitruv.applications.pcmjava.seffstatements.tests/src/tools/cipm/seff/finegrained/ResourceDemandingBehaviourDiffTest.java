package tools.cipm.seff.finegrained;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.SeffFactory;

/**
 * Tests {@link ResourceDemandingBehaviourDiff}: the four action buckets it exposes (deleted,
 * added, modified, unmodified) and the shared findMatching lookup that hasOldAbstractActionMatching
 * / hasNewAbstractActionMatching / getNewAbstractActionMatching all delegate to (commit A4).
 * No correspondence model or SoMoX involved - AbstractAction instances are compared by
 * identity throughout, so plain PCM objects are enough.
 */
class ResourceDemandingBehaviourDiffTest {

	private final ResourceDemandingBehaviourDiff diff = new ResourceDemandingBehaviourDiff();

	@Test
	void addDeletedAbstractAction_appearsInDeletedBucket() {
		AbstractAction deleted = SeffFactory.eINSTANCE.createInternalCallAction();

		diff.addDeletedAbstractAction(deleted);

		assertTrue(diff.getDeletedAbstractActions().contains(deleted));
	}

	@Test
	void addAddedAbstractAction_appearsInAddedBucket() {
		AbstractAction added = SeffFactory.eINSTANCE.createInternalCallAction();

		diff.addAddedAbstractAction(added);

		assertTrue(diff.getAddedAbstractActions().contains(added));
	}

	@Test
	void addAddedAbstractActions_bulk_appearAllInAddedBucket() {
		AbstractAction first = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction second = SeffFactory.eINSTANCE.createInternalCallAction();

		diff.addAddedAbstractActions(List.of(first, second));

		assertTrue(diff.getAddedAbstractActions().contains(first));
		assertTrue(diff.getAddedAbstractActions().contains(second));
	}

	@Test
	void isModified_trueOnlyForTheExactMatchingInstanceAddedAsModified() {
		AbstractAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractActionMatching modifiedMatching = new AbstractActionMatching(newAction, oldAction);
		// A different matching instance over the very same pair of actions - deliberately
		// checking that isModified compares the matching object itself, not "any matching
		// covering these two actions".
		AbstractActionMatching lookalikeMatching = new AbstractActionMatching(newAction, oldAction);

		diff.addModifiedAbstractAction(modifiedMatching);

		assertTrue(diff.isModified(modifiedMatching));
		assertFalse(diff.isModified(lookalikeMatching));
	}

	@Test
	void hasOldAbstractActionMatching_trueWhenAMatchingReferencesThatOldAction() {
		AbstractAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		diff.addUnmodifiedAbstractAction(new AbstractActionMatching(newAction, oldAction));

		assertTrue(diff.hasOldAbstractActionMatching(oldAction));
	}

	@Test
	void hasOldAbstractActionMatching_falseWhenNoMatchingReferencesIt() {
		AbstractAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();

		assertFalse(diff.hasOldAbstractActionMatching(oldAction));
	}

	@Test
	void hasNewAbstractActionMatching_trueWhenAMatchingReferencesThatNewAction() {
		AbstractAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		diff.addModifiedAbstractAction(new AbstractActionMatching(newAction, oldAction));

		assertTrue(diff.hasNewAbstractActionMatching(newAction));
	}

	@Test
	void hasNewAbstractActionMatching_falseWhenNoMatchingReferencesIt() {
		AbstractAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();

		assertFalse(diff.hasNewAbstractActionMatching(newAction));
	}

	@Test
	void getNewAbstractActionMatching_returnsTheMatchingContainingThatNewAction() {
		AbstractAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractActionMatching matching = new AbstractActionMatching(newAction, oldAction);
		diff.addUnmodifiedAbstractAction(matching);

		assertSame(matching, diff.getNewAbstractActionMatching(newAction));
	}

	/**
	 * Characterizes the shared findMatching helper's search order (commit A4 collapsed three
	 * near-identical loops into this one method): the modified bucket is searched before the
	 * unmodified bucket, regardless of which matching was actually added first. If this ever
	 * flips, this test documents that it changed, on purpose or otherwise.
	 */
	@Test
	void getNewAbstractActionMatching_searchesModifiedBucketBeforeUnmodifiedBucket() {
		AbstractAction sharedNewAction = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction oldActionInModifiedMatching = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction oldActionInUnmodifiedMatching = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractActionMatching modifiedMatching = new AbstractActionMatching(sharedNewAction,
				oldActionInModifiedMatching);
		AbstractActionMatching unmodifiedMatching = new AbstractActionMatching(sharedNewAction,
				oldActionInUnmodifiedMatching);

		// Added in this order deliberately: if the lookup just returned "whichever was
		// inserted last", it would return the unmodified one here, which is not what
		// happens - modified is always checked first.
		diff.addModifiedAbstractAction(modifiedMatching);
		diff.addUnmodifiedAbstractAction(unmodifiedMatching);

		assertSame(modifiedMatching, diff.getNewAbstractActionMatching(sharedNewAction));
	}
}
