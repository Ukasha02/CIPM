package tools.cipm.seff.extended;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.MembersFactory;
import org.emftext.language.java.members.Method;
import org.emftext.language.java.statements.Statement;
import org.emftext.language.java.statements.StatementsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.InternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingInternalBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.somox.gast2seff.visitors.IFunctionClassificationStrategy;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFindingFactory;
import org.somox.gast2seff.visitors.ResourceDemandingBehaviourForClassMethodFinding;
import org.somox.sourcecodedecorator.SeffElementSourceCodeLink;
import org.somox.sourcecodedecorator.SourceCodeDecoratorRepository;
import org.somox.sourcecodedecorator.SourcecodedecoratorFactory;

import de.uka.ipd.sdq.identifier.Identifier;
import tools.cipm.seff.BasicComponentFinding;
import tools.cipm.seff.CorrespondenceModelUtil;
import tools.cipm.seff.testutil.CorrespondenceModelViews;
import tools.cipm.seff.testutil.TestModelObjects;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests the extra step {@link ExtendedClassMethodBodyChangedTransformation} adds after the
 * base transformation: binding each SEFF action to the Java statements it was generated from
 * (step 5 of execute, implemented by the private bindAbstractActionsAndStatements).
 *
 * <p>Reached through execute() with the same two protected seams used for the fine-grained
 * tests: the real SoMoX call is skipped, and the "what SoMoX would have linked" repository is
 * supplied by the test. This only works because execute() reads the repository through
 * {@code this.getSourceCodeDecoratorRepository()}; it used to call {@code super.} and so
 * bypassed the seam (changed in a separate testability-only commit, no behaviour change).
 *
 * <p>Deliberately asserts only that a correspondence between an action and a statement
 * exists or does not exist. The method registers them under the correspondence tag "" while
 * the rest of the code uses the default tag; whether that is intended is still an open
 * question, so no assertion here depends on the tag.
 */
class ExtendedClassMethodBodyChangedTransformationTest {

	private static class SoMoXFreeTransformation extends ExtendedClassMethodBodyChangedTransformation {
		private final List<AbstractAction> actionsFromSoMoX;
		private final SourceCodeDecoratorRepository fixtureRepository;

		SoMoXFreeTransformation(Method newMethod, BasicComponentFinding basicComponentFinder,
				IFunctionClassificationStrategy classificationStrategy,
				InterfaceOfExternalCallFindingFactory interfaceOfExternalCallFindingFactory,
				ResourceDemandingBehaviourForClassMethodFinding resourceDemandingBehaviourForClassMethodFinding,
				List<AbstractAction> actionsFromSoMoX, SourceCodeDecoratorRepository fixtureRepository) {
			super(newMethod, basicComponentFinder, classificationStrategy, interfaceOfExternalCallFindingFactory,
					resourceDemandingBehaviourForClassMethodFinding);
			this.actionsFromSoMoX = actionsFromSoMoX;
			this.fixtureRepository = fixtureRepository;
		}

		@Override
		protected void executeSoMoXForMethod(BasicComponent basicComponent,
				ResourceDemandingBehaviour targetResourceDemandingBehaviour) {
			// Real SoMoX skipped - put in the SEFF what it would have produced.
			targetResourceDemandingBehaviour.getSteps_Behaviour().addAll(actionsFromSoMoX);
		}

		@Override
		protected SourceCodeDecoratorRepository getSourceCodeDecoratorRepository() {
			return fixtureRepository;
		}
	}

	private static SeffElementSourceCodeLink link(SourceCodeDecoratorRepository repository, Identifier seffElement,
			Statement... statements) {
		SeffElementSourceCodeLink link = SourcecodedecoratorFactory.eINSTANCE.createSeffElementSourceCodeLink();
		link.setSeffElement(seffElement);
		link.getStatement().addAll(List.of(statements));
		repository.getSeffElementsSourceCodeLinks().add(link);
		return link;
	}

	private static SourceCodeDecoratorRepository newRepository() {
		return TestModelObjects.newSourceCodeDecoratorRepository();
	}

	/** Registers a method -> empty SEFF correspondence so execute() treats the change as relevant. */
	private static ClassMethod methodWithSeff(EditableCorrespondenceModelView<Correspondence> view) {
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		ResourceDemandingSEFF seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		view.addCorrespondenceBetween(method, seff, null);
		return method;
	}

	private static List<Statement> statementsBoundTo(EditableCorrespondenceModelView<Correspondence> view,
			AbstractAction action) {
		return CorrespondenceModelUtil.getCorrespondingEObjects(view, action, Statement.class);
	}

	private static void run(ClassMethod method, EditableCorrespondenceModelView<Correspondence> view,
			List<AbstractAction> actionsFromSoMoX, SourceCodeDecoratorRepository repository) {
		BasicComponentFinding finder = (m, correspondenceModel) -> null;
		new SoMoXFreeTransformation(method, finder, null, null, null, actionsFromSoMoX, repository).execute(view,
				null);
	}

	/**
	 * Purpose: the basic job of the step. An action in the main SEFF that SoMoX linked to one
	 * statement must end up registered together with that statement, so the later
	 * instrumentation step can find the code behind the action.
	 */
	@Test
	void execute_actionLinkedToOneStatement_actionAndStatementAreCorresponded(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodWithSeff(view);
		InternalCallAction action = SeffFactory.eINSTANCE.createInternalCallAction();
		Statement statement = StatementsFactory.eINSTANCE.createEmptyStatement();
		SourceCodeDecoratorRepository repository = newRepository();
		link(repository, action, statement);

		run(method, view, List.of(action), repository);

		assertEquals(List.of(statement), statementsBoundTo(view, action));
	}

	/**
	 * Purpose: one action can stand for several statements (e.g. a loop header plus its body);
	 * each linked statement must get its own correspondence, none dropped.
	 */
	@Test
	void execute_actionLinkedToTwoStatements_bothStatementsAreCorresponded(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodWithSeff(view);
		InternalCallAction action = SeffFactory.eINSTANCE.createInternalCallAction();
		Statement first = StatementsFactory.eINSTANCE.createEmptyStatement();
		Statement second = StatementsFactory.eINSTANCE.createEmptyStatement();
		SourceCodeDecoratorRepository repository = newRepository();
		link(repository, action, first, second);

		run(method, view, List.of(action), repository);

		List<Statement> bound = statementsBoundTo(view, action);
		assertEquals(2, bound.size());
		assertTrue(bound.contains(first));
		assertTrue(bound.contains(second));
	}

	/**
	 * Purpose: the documented skip rule. Actions inside a ResourceDemandingInternalBehaviour
	 * (a nested sub-recipe) are deliberately not bound. The skipped link comes first and a
	 * normal link second, proving the loop moves on to the next link (continue, not return).
	 */
	@Test
	void execute_actionInsideInternalBehaviour_isSkippedButLaterLinksAreStillBound(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodWithSeff(view);

		ResourceDemandingInternalBehaviour internalBehaviour = SeffFactory.eINSTANCE
				.createResourceDemandingInternalBehaviour();
		InternalCallAction nestedAction = SeffFactory.eINSTANCE.createInternalCallAction();
		internalBehaviour.getSteps_Behaviour().add(nestedAction);
		Statement nestedStatement = StatementsFactory.eINSTANCE.createEmptyStatement();

		InternalCallAction mainAction = SeffFactory.eINSTANCE.createInternalCallAction();
		Statement mainStatement = StatementsFactory.eINSTANCE.createEmptyStatement();

		SourceCodeDecoratorRepository repository = newRepository();
		link(repository, nestedAction, nestedStatement);
		link(repository, mainAction, mainStatement);

		run(method, view, List.of(mainAction), repository);

		assertTrue(statementsBoundTo(view, nestedAction).isEmpty());
		assertEquals(List.of(mainStatement), statementsBoundTo(view, mainAction));
	}

	/**
	 * Purpose: a link whose SEFF element is not an action at all (the repository can hold
	 * links for other SEFF elements) is ignored rather than crashing on a bad cast.
	 */
	@Test
	void execute_linkElementIsNotAnAction_isIgnoredWithoutError(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodWithSeff(view);
		BasicComponent notAnAction = RepositoryFactory.eINSTANCE.createBasicComponent();
		Statement statement = StatementsFactory.eINSTANCE.createEmptyStatement();
		SourceCodeDecoratorRepository repository = newRepository();
		link(repository, notAnAction, statement);

		assertDoesNotThrow(() -> run(method, view, List.of(), repository));

		assertTrue(CorrespondenceModelUtil.getCorrespondingEObjects(view, statement, AbstractAction.class).isEmpty());
	}

	/**
	 * Purpose: the null guard around the step. If no repository exists (nothing was ever
	 * extracted), execute() must finish cleanly and simply bind nothing.
	 */
	@Test
	void execute_noSourceCodeDecoratorRepository_bindsNothingAndDoesNotFail(@TempDir Path tempDir) {
		EditableCorrespondenceModelView<Correspondence> view = CorrespondenceModelViews.newEditableView(tempDir);
		ClassMethod method = methodWithSeff(view);
		InternalCallAction action = SeffFactory.eINSTANCE.createInternalCallAction();

		assertDoesNotThrow(() -> run(method, view, List.of(action), null));

		assertTrue(statementsBoundTo(view, action).isEmpty());
	}
}
