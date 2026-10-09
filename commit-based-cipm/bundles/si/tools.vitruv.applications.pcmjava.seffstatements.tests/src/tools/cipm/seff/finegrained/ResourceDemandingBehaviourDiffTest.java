package tools.cipm.seff.finegrained;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.SeffFactory;

/**
 * Tests {@link ResourceDemandingBehaviourDiff}: the four action buckets it exposes (deleted,
 * added, modified, unmodified) and the shared findMatching lookup that hasOldAbstractActionMatching
 * / hasNewAbstractActionMatching / getNewAbstractActionMatching all delegate to.
 * No correspondence model or SoMoX involved - AbstractAction instances are compared by
 * identity throughout, so plain PCM objects are enough.
 */
class ResourceDemandingBehaviourDiffTest {

	private final ResourceDemandingBehaviourDiff diff = new ResourceDemandingBehaviourDiff();

	/** A plain old/new action pair, used by the matching-lookup tests below; unused by the bucket tests. */
	private AbstractAction oldAction;
	private AbstractAction newAction;

	@BeforeEach
	void setUp() {
		oldAction = SeffFactory.eINSTANCE.createInternalCallAction();
		newAction = SeffFactory.eINSTANCE.createInternalCallAction();
	}

	/** An action added as deleted shows up in the deleted bucket. */
	@Test
	void addDeletedAbstractAction_appearsInDeletedBucket() {
		AbstractAction deleted = SeffFactory.eINSTANCE.createInternalCallAction();

		diff.addDeletedAbstractAction(deleted);

		assertTrue(diff.getDeletedAbstractActions().contains(deleted));
	}

	/** An action added as added shows up in the added bucket. */
	@Test
	void addAddedAbstractAction_appearsInAddedBucket() {
		AbstractAction added = SeffFactory.eINSTANCE.createInternalCallAction();

		diff.addAddedAbstractAction(added);

		assertTrue(diff.getAddedAbstractActions().contains(added));
	}

	/** Adding several actions at once as added puts all of them in the added bucket. */
	@Test
	void addAddedAbstractActions_bulk_appearAllInAddedBucket() {
		AbstractAction first = SeffFactory.eINSTANCE.createInternalCallAction();
		AbstractAction second = SeffFactory.eINSTANCE.createInternalCallAction();

		diff.addAddedAbstractActions(List.of(first, second));

		assertTrue(diff.getAddedAbstractActions().contains(first));
		assertTrue(diff.getAddedAbstractActions().contains(second));
	}

	/**
	 * isModified is true only for the exact matching instance that was added, not for a
	 * different matching instance over the same pair of actions.
	 */
	@Test
	void isModified_trueOnlyForTheExactMatchingInstanceAddedAsModified() {
		AbstractActionMatching modifiedMatching = new AbstractActionMatching(newAction, oldAction);
		// A different matching instance over the very same pair of actions - deliberately
		// checking that isModified compares the matching object itself, not "any matching
		// covering these two actions".
		AbstractActionMatching lookalikeMatching = new AbstractActionMatching(newAction, oldAction);

		diff.addModifiedAbstractAction(modifiedMatching);

		assertTrue(diff.isModified(modifiedMatching));
		assertFalse(diff.isModified(lookalikeMatching));
	}

	/** True once a matching referencing that old action has been added, regardless of which bucket it was added to. */
	@Test
	void hasOldAbstractActionMatching_trueWhenAMatchingReferencesThatOldAction() {
		diff.addUnmodifiedAbstractAction(new AbstractActionMatching(newAction, oldAction));

		assertTrue(diff.hasOldAbstractActionMatching(oldAction));
	}

	/** False when no matching anywhere references that old action. */
	@Test
	void hasOldAbstractActionMatching_falseWhenNoMatchingReferencesIt() {
		assertFalse(diff.hasOldAbstractActionMatching(oldAction));
	}

	/** True once a matching referencing that new action has been added. */
	@Test
	void hasNewAbstractActionMatching_trueWhenAMatchingReferencesThatNewAction() {
		diff.addModifiedAbstractAction(new AbstractActionMatching(newAction, oldAction));

		assertTrue(diff.hasNewAbstractActionMatching(newAction));
	}

	/** False when no matching anywhere references that new action. */
	@Test
	void hasNewAbstractActionMatching_falseWhenNoMatchingReferencesIt() {
		assertFalse(diff.hasNewAbstractActionMatching(newAction));
	}

	/** Returns the exact matching instance that contains the given new action. */
	@Test
	void getNewAbstractActionMatching_returnsTheMatchingContainingThatNewAction() {
		AbstractActionMatching matching = new AbstractActionMatching(newAction, oldAction);
		diff.addUnmodifiedAbstractAction(matching);

		assertSame(matching, diff.getNewAbstractActionMatching(newAction));
	}

	/**
	 * Characterizes the shared findMatching helper's search order (this one method replaced
	 * three near-identical lookup loops): the modified bucket is searched before the
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
