package tools.cipm.seff.finegrained;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.emftext.language.java.members.Method;
import org.emftext.language.java.statements.Statement;
import org.emftext.language.java.statements.StatementsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.InternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.somox.gast2seff.visitors.IFunctionClassificationStrategy;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFindingFactory;
import org.somox.gast2seff.visitors.ResourceDemandingBehaviourForClassMethodFinding;
import org.somox.sourcecodedecorator.SeffElementSourceCodeLink;
import org.somox.sourcecodedecorator.SourceCodeDecoratorRepository;
import org.somox.sourcecodedecorator.SourcecodedecoratorFactory;

import tools.cipm.seff.BasicComponentFinding;
import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.cipm.seff.testutil.TestModelObjects;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link FineGrainedClassMethodBodyChangedTransformation#execute}. Unlike the base
 * class, this one builds a brand-new SEFF from SoMoX and diffs it against the existing one
 * before merging - both protected seams (executeSoMoXForMethod, getSourceCodeDecoratorRepository)
 * are used together here.
 *
 * <p>This first test covers the "old SEFF has no real steps yet" fast path in
 * calculateResourceDemandingBehaviourDiff, which skips the statement-similarity comparison
 * entirely. The fake new SEFF deliberately has no Start/StopAction of its own - real SoMoX
 * output isn't guaranteed to include them either (that's exactly why ensureStartAndStopAction,
 * tested elsewhere, exists as a safety net); giving the fake one its own bookends would make
 * the merge logic insert a second, spurious pair into the existing SEFF.
 */
class FineGrainedClassMethodBodyChangedTransformationTest {

	/**
	 * Skips the real SoMoX call, populating the new SEFF with the given actions instead, and
	 * returns the given fixture repository in place of the one real SoMoX would have built.
	 */
	private static class SoMoXFreeTransformation extends FineGrainedClassMethodBodyChangedTransformation {
		private final List<AbstractAction> newActionsFromSoMoX;
		private final SourceCodeDecoratorRepository fixtureRepository;

		SoMoXFreeTransformation(Method newMethod, BasicComponentFinding basicComponentFinder,
				IFunctionClassificationStrategy classificationStrategy,
				InterfaceOfExternalCallFindingFactory interfaceOfExternalCallFindingFactory,
				ResourceDemandingBehaviourForClassMethodFinding resourceDemandingBehaviourForClassMethodFinding,
				List<AbstractAction> newActionsFromSoMoX, SourceCodeDecoratorRepository fixtureRepository) {
			super(newMethod, basicComponentFinder, classificationStrategy, interfaceOfExternalCallFindingFactory,
					resourceDemandingBehaviourForClassMethodFinding);
			this.newActionsFromSoMoX = newActionsFromSoMoX;
			this.fixtureRepository = fixtureRepository;
		}

		@Override
		protected void executeSoMoXForMethod(BasicComponent basicComponent,
				ResourceDemandingBehaviour targetResourceDemandingBehaviour) {
			// Real SoMoX skipped - directly populate the new SEFF with what it would have
			// produced. Deliberately no Start/StopAction here - see class Javadoc.
			targetResourceDemandingBehaviour.getSteps_Behaviour().addAll(newActionsFromSoMoX);
		}

		@Override
		protected SourceCodeDecoratorRepository getSourceCodeDecoratorRepository() {
			return fixtureRepository;
		}
	}

	/**
	 * The "old SEFF has no real steps yet" fast path: with nothing to compare against, every
	 * new action is simply added between the existing bookends.
	 */
	@Test
	void execute_oldSeffHasNoRelevantActions_allNewActionsAreAdded(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();

		ResourceDemandingSEFF oldSeff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		StartAction oldStart = SeffFactory.eINSTANCE.createStartAction();
		StopAction oldStop = SeffFactory.eINSTANCE.createStopAction();
		oldSeff.getSteps_Behaviour().add(oldStart);
		oldSeff.getSteps_Behaviour().add(oldStop);
		view.addCorrespondenceBetween(method, oldSeff, null);

		InternalCallAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		SourceCodeDecoratorRepository emptyRepository = TestModelObjects.newSourceCodeDecoratorRepository();

		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		SoMoXFreeTransformation transformation = new SoMoXFreeTransformation(method, finder, null, null, null,
				List.of(newAction), emptyRepository);

		transformation.execute(view, null);

		EList<AbstractAction> steps = oldSeff.getSteps_Behaviour();
		assertEquals(3, steps.size());
		assertSame(oldStart, steps.get(0));
		assertSame(newAction, steps.get(1));
		assertSame(oldStop, steps.get(2));
	}

	/**
	 * The "matched" comparison path: the real SimilarityChecker is used (not faked), but its
	 * own short-circuit rule - the exact same statement object is always similar to itself -
	 * makes this reliable to test without needing to understand its deeper type-specific
	 * comparison logic. Old and new actions correspond to the exact same statement, so they're
	 * classified as unmodified: the old action stays in the SEFF, the new one is discarded.
	 */
	@Test
	void execute_oldAndNewActionCorrespondToTheSameStatement_oldActionIsKeptAsUnmodified(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();

		ResourceDemandingSEFF oldSeff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		StartAction oldStart = SeffFactory.eINSTANCE.createStartAction();
		InternalCallAction oldAction = SeffFactory.eINSTANCE.createInternalCallAction();
		StopAction oldStop = SeffFactory.eINSTANCE.createStopAction();
		oldSeff.getSteps_Behaviour().add(oldStart);
		oldSeff.getSteps_Behaviour().add(oldAction);
		oldSeff.getSteps_Behaviour().add(oldStop);
		view.addCorrespondenceBetween(method, oldSeff, null);

		Statement sharedStatement = StatementsFactory.eINSTANCE.createEmptyStatement();
		view.addCorrespondenceBetween(oldAction, sharedStatement, null);

		InternalCallAction newAction = SeffFactory.eINSTANCE.createInternalCallAction();
		SourceCodeDecoratorRepository fixtureRepository = TestModelObjects.newSourceCodeDecoratorRepository();
		SeffElementSourceCodeLink link = SourcecodedecoratorFactory.eINSTANCE.createSeffElementSourceCodeLink();
		link.setSeffElement(newAction);
		link.getStatement().add(sharedStatement);
		fixtureRepository.getSeffElementsSourceCodeLinks().add(link);

		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		SoMoXFreeTransformation transformation = new SoMoXFreeTransformation(method, finder, null, null, null,
				List.of(newAction), fixtureRepository);

		transformation.execute(view, null);

		EList<AbstractAction> steps = oldSeff.getSteps_Behaviour();
		assertEquals(3, steps.size());
		assertSame(oldStart, steps.get(0));
		assertSame(oldAction, steps.get(1));
		assertSame(oldStop, steps.get(2));
	}

	/**
	 * The fine-grained "zero statements" open question. This is not just one test among
	 * several covering it - it is the only test that will ever exercise this scenario, since
	 * .finegrained is structurally unreachable through the TEAMMATES pipeline.
	 *
	 * <p>Found by tracing this by hand before writing it: when a new action has zero linked
	 * statements, "how many of its statements match this old action's statements" is trivially
	 * "0 out of 0", which the code accepts as a perfect match - pairing the zero-statement new
	 * action with whichever old action happens to be checked first, regardless of any actual
	 * relationship. Worse: because that old action gets "used up" by this arbitrary match, a
	 * completely unrelated, unchanged old action checked afterwards finds nothing left to
	 * correspond to it and gets classified as deleted - removed from the SEFF as a side effect,
	 * even though nothing about it changed. Recorded as current behaviour; whether this is
	 * correct is still an open design question, not yet resolved.
	 */
	@Test
	void execute_newActionHasZeroLinkedStatements_arbitraryMatchDeletesAnUnrelatedOldAction(
			@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();

		ResourceDemandingSEFF oldSeff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		StartAction oldStart = SeffFactory.eINSTANCE.createStartAction();
		InternalCallAction oldActionCheckedFirst = SeffFactory.eINSTANCE.createInternalCallAction();
		InternalCallAction oldActionCheckedSecond = SeffFactory.eINSTANCE.createInternalCallAction();
		StopAction oldStop = SeffFactory.eINSTANCE.createStopAction();
		oldSeff.getSteps_Behaviour().add(oldStart);
		oldSeff.getSteps_Behaviour().add(oldActionCheckedFirst);
		oldSeff.getSteps_Behaviour().add(oldActionCheckedSecond);
		oldSeff.getSteps_Behaviour().add(oldStop);
		view.addCorrespondenceBetween(method, oldSeff, null);

		// Deliberately unrelated to the new action below - proves the eventual "match" isn't
		// based on any real content overlap. oldActionCheckedSecond gets no correspondence at
		// all - it never changed.
		Statement unrelatedStatement = StatementsFactory.eINSTANCE.createEmptyStatement();
		view.addCorrespondenceBetween(oldActionCheckedFirst, unrelatedStatement, null);

		// The action under investigation: SoMoX produced it, but linked it to zero statements.
		InternalCallAction newActionWithNoStatements = SeffFactory.eINSTANCE.createInternalCallAction();
		SourceCodeDecoratorRepository fixtureRepository = TestModelObjects.newSourceCodeDecoratorRepository();
		SeffElementSourceCodeLink link = SourcecodedecoratorFactory.eINSTANCE.createSeffElementSourceCodeLink();
		link.setSeffElement(newActionWithNoStatements);
		// link.getStatement() deliberately left empty - this is the scenario under test.
		fixtureRepository.getSeffElementsSourceCodeLinks().add(link);

		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		SoMoXFreeTransformation transformation = new SoMoXFreeTransformation(method, finder, null, null, null,
				List.of(newActionWithNoStatements), fixtureRepository);

		transformation.execute(view, null);

		EList<AbstractAction> steps = oldSeff.getSteps_Behaviour();
		assertEquals(3, steps.size());
		assertSame(oldStart, steps.get(0));
		// The arbitrary "match": kept in place, unchanged, purely because it was checked first.
		assertSame(oldActionCheckedFirst, steps.get(1));
		assertSame(oldStop, steps.get(2));
		// The unrelated action that never actually changed - genuinely removed as a side effect.
		assertFalse(steps.contains(oldActionCheckedSecond));
	}
}
