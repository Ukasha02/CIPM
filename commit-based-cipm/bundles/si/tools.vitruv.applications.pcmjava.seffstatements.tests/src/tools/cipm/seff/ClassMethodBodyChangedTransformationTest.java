package tools.cipm.seff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.eclipse.emf.common.util.EList;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.emftext.language.java.members.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.InternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.somox.gast2seff.visitors.IFunctionClassificationStrategy;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFindingFactory;
import org.somox.gast2seff.visitors.ResourceDemandingBehaviourForClassMethodFinding;

import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link ClassMethodBodyChangedTransformation}. The first part (unchanged) covers
 * {@link ClassMethodBodyChangedTransformation#ensureStartAndStopAction}, pure PCM/EMF list
 * manipulation with no SoMoX involved. The second part (Phase 3) covers {@code execute(...)}
 * itself, which does have a real SoMoX call buried inside it - reached via a test-only
 * subclass that overrides the two protected "seam" methods
 * (executeSoMoXForMethod/getSourceCodeDecoratorRepository) the real Extended/FineGrained
 * subclasses already use for their own purposes, so the real SoMoX call is skipped while
 * everything else in execute() runs unmodified.
 */
class ClassMethodBodyChangedTransformationTest {

	/** An empty behaviour gets both a StartAction and a StopAction added, since neither exists yet. */
	@Test
	void emptyBehaviour_getsBothStartAndStopActionAdded() {
		ResourceDemandingBehaviour behaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();

		ClassMethodBodyChangedTransformation.ensureStartAndStopAction(behaviour);

		EList<AbstractAction> steps = behaviour.getSteps_Behaviour();
		assertEquals(2, steps.size());
		assertTrue(steps.get(0) instanceof StartAction);
		assertTrue(steps.get(1) instanceof StopAction);
	}

	/** A behaviour that already has a StartAction only gets a StopAction appended; the existing StartAction is left in place. */
	@Test
	void behaviourWithOnlyAStartAction_getsAStopActionAppended() {
		ResourceDemandingBehaviour behaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();
		StartAction existingStart = SeffFactory.eINSTANCE.createStartAction();
		behaviour.getSteps_Behaviour().add(existingStart);

		ClassMethodBodyChangedTransformation.ensureStartAndStopAction(behaviour);

		EList<AbstractAction> steps = behaviour.getSteps_Behaviour();
		assertEquals(2, steps.size());
		assertSame(existingStart, steps.get(0));
		assertTrue(steps.get(1) instanceof StopAction);
	}

	/** A behaviour that already has a StopAction only gets a StartAction prepended; the existing StopAction is left in place. */
	@Test
	void behaviourWithOnlyAStopAction_getsAStartActionPrepended() {
		ResourceDemandingBehaviour behaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();
		StopAction existingStop = SeffFactory.eINSTANCE.createStopAction();
		behaviour.getSteps_Behaviour().add(existingStop);

		ClassMethodBodyChangedTransformation.ensureStartAndStopAction(behaviour);

		EList<AbstractAction> steps = behaviour.getSteps_Behaviour();
		assertEquals(2, steps.size());
		assertTrue(steps.get(0) instanceof StartAction);
		assertSame(existingStop, steps.get(1));
	}

	/** A behaviour that already has both bookends correctly positioned is left completely unchanged. */
	@Test
	void behaviourWithBothCorrectlyPositioned_isLeftUnchanged() {
		ResourceDemandingBehaviour behaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();
		StartAction existingStart = SeffFactory.eINSTANCE.createStartAction();
		StopAction existingStop = SeffFactory.eINSTANCE.createStopAction();
		behaviour.getSteps_Behaviour().add(existingStart);
		behaviour.getSteps_Behaviour().add(existingStop);

		ClassMethodBodyChangedTransformation.ensureStartAndStopAction(behaviour);

		EList<AbstractAction> steps = behaviour.getSteps_Behaviour();
		assertEquals(2, steps.size());
		assertSame(existingStart, steps.get(0));
		assertSame(existingStop, steps.get(1));
	}

	/**
	 * Characterizes current behaviour rather than asserting an ideal one: the method only
	 * checks index 0 for a StartAction. A StartAction present anywhere else in the list is
	 * not recognized, so a second StartAction is inserted at index 0 - the list ends up with
	 * two StartActions. Worth knowing before relying on "there is exactly one StartAction".
	 */
	@Test
	void startActionPresentButNotAtIndexZero_isNotRecognized_soASecondOneIsInserted() {
		ResourceDemandingBehaviour behaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();
		InternalCallAction leadingAction = SeffFactory.eINSTANCE.createInternalCallAction();
		StartAction misplacedStart = SeffFactory.eINSTANCE.createStartAction();
		behaviour.getSteps_Behaviour().add(leadingAction);
		behaviour.getSteps_Behaviour().add(misplacedStart);

		ClassMethodBodyChangedTransformation.ensureStartAndStopAction(behaviour);

		EList<AbstractAction> steps = behaviour.getSteps_Behaviour();
		assertEquals(4, steps.size());
		assertTrue(steps.get(0) instanceof StartAction);
		assertSame(leadingAction, steps.get(1));
		assertSame(misplacedStart, steps.get(2));
		assertTrue(steps.get(3) instanceof StopAction);
	}

	// ---- Phase 3: execute(...), with the real SoMoX call skipped via the protected seam ----

	/**
	 * Skips the real SoMoX call (executeSoMoXForMethod) entirely - simulates "SoMoX ran and
	 * added nothing new" by default - and records whether it was called at all. Everything else
	 * in execute() (the real, unmodified logic) still runs.
	 */
	private static class SoMoXFreeTransformation extends ClassMethodBodyChangedTransformation {
		private boolean soMoXCalled = false;

		SoMoXFreeTransformation(Method newMethod, BasicComponentFinding basicComponentFinder,
				IFunctionClassificationStrategy classificationStrategy,
				InterfaceOfExternalCallFindingFactory interfaceOfExternalCallFindingFactory,
				ResourceDemandingBehaviourForClassMethodFinding resourceDemandingBehaviourForClassMethodFinding) {
			super(newMethod, basicComponentFinder, classificationStrategy, interfaceOfExternalCallFindingFactory,
					resourceDemandingBehaviourForClassMethodFinding);
		}

		@Override
		protected void executeSoMoXForMethod(BasicComponent basicComponent,
				ResourceDemandingBehaviour targetResourceDemandingBehaviour) {
			soMoXCalled = true;
		}

		boolean wasSoMoXCalled() {
			return soMoXCalled;
		}
	}

	/**
	 * Additionally forces findRdBehaviorToInsertElements() to report "nothing found",
	 * independent of whatever is actually registered on the correspondence board. Needed
	 * because in real usage this and isArchitectureRelevantChange() query the same
	 * correspondence, so naturally provoking "relevant, but nothing to insert into" isn't
	 * practically possible - this tests that defensive null-guard in isolation instead.
	 */
	private static class ForcedNullRdBehaviour extends SoMoXFreeTransformation {
		ForcedNullRdBehaviour(Method newMethod, BasicComponentFinding basicComponentFinder,
				IFunctionClassificationStrategy classificationStrategy,
				InterfaceOfExternalCallFindingFactory interfaceOfExternalCallFindingFactory,
				ResourceDemandingBehaviourForClassMethodFinding resourceDemandingBehaviourForClassMethodFinding) {
			super(newMethod, basicComponentFinder, classificationStrategy, interfaceOfExternalCallFindingFactory,
					resourceDemandingBehaviourForClassMethodFinding);
		}

		@Override
		protected ResourceDemandingBehaviour findRdBehaviorToInsertElements(
				EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
			return null;
		}
	}

	/**
	 * A defensive null-guard: this branch never fires in the real TEAMMATES run -
	 * this is the first time it's ever been exercised by any test.
	 */
	@Test
	void execute_noResourceDemandingBehaviourToInsertInto_returnsCleanlyWithoutCallingSoMoX(
			@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		// Makes isArchitectureRelevantChange() true, so execute() gets past its first guard.
		ResourceDemandingBehaviour existingBehaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();
		view.addCorrespondenceBetween(method, existingBehaviour, null);

		// Never reached: findRdBehaviorToInsertElements() (overridden below) returns null
		// before basicComponentFinder is ever touched.
		BasicComponentFinding unusedFinder = (m, correspondenceModel) -> {
			throw new AssertionError("should not be called once findRdBehaviorToInsertElements returned null");
		};
		// iFunctionClassificationStrategy/interfaceOfExternalCallFindingFactory/
		// resourceDemandingBehaviourForClassMethodFinding are only ever read inside
		// executeSoMoXForMethod, which is overridden away entirely - safe to pass null.
		ForcedNullRdBehaviour transformation = new ForcedNullRdBehaviour(method, unusedFinder, null, null, null);

		transformation.execute(view, null);

		assertFalse(transformation.wasSoMoXCalled());
	}

	/**
	 * The other early-return guard, isArchitectureRelevantChange() - no override trick needed
	 * here, since simply not registering any correspondence at all naturally provokes it.
	 */
	@Test
	void execute_methodHasNoResourceDemandingBehaviourCorrespondence_returnsWithoutCallingSoMoX(
			@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		BasicComponentFinding unusedFinder = (m, correspondenceModel) -> {
			throw new AssertionError("should not be called - execute() must return at the very first guard");
		};
		SoMoXFreeTransformation transformation = new SoMoXFreeTransformation(method, unusedFinder, null, null, null);

		transformation.execute(view, null);

		assertFalse(transformation.wasSoMoXCalled());
	}

	/**
	 * emptyCorrespondingSeffs()/removeObjectWithChildren(): registers a SEFF that already has
	 * one action on it, with a correspondence on that action too, then confirms both the
	 * action and its correspondence are gone after execute() runs - not just that nothing
	 * crashed. The SEFF ends up with fresh Start/Stop bookends afterwards (ensureStartAndStop
	 * Action running on the now-emptied list further down in execute()), confirming the old
	 * action was genuinely removed rather than merely left untouched.
	 */
	@Test
	void execute_existingSeffAction_isRemovedAlongWithItsCorrespondence(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();

		ResourceDemandingBehaviour seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		InternalCallAction existingAction = SeffFactory.eINSTANCE.createInternalCallAction();
		seff.getSteps_Behaviour().add(existingAction);
		view.addCorrespondenceBetween(method, seff, null);
		// Something correspondence-tracked hanging off the old action, to confirm cleanup
		// reaches the action's own correspondences too, not just the action itself.
		ClassMethod unrelatedCorrespondenceTarget = MembersFactory.eINSTANCE.createClassMethod();
		view.addCorrespondenceBetween(existingAction, unrelatedCorrespondenceTarget, null);

		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		SoMoXFreeTransformation transformation = new SoMoXFreeTransformation(method, finder, null, null, null);

		transformation.execute(view, null);

		assertFalse(view.hasCorrespondences(existingAction));
		assertFalse(seff.getSteps_Behaviour().contains(existingAction));
		assertEquals(2, seff.getSteps_Behaviour().size());
		assertTrue(seff.getSteps_Behaviour().get(0) instanceof StartAction);
		assertTrue(seff.getSteps_Behaviour().get(1) instanceof StopAction);
	}
}
