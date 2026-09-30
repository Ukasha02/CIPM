package tools.cipm.seff;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EcoreFactory;
import org.emftext.language.java.commons.CommonsPackage;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.Field;
import org.emftext.language.java.members.MembersFactory;
import org.junit.jupiter.api.Test;

import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.change.atomic.feature.attribute.AttributeFactory;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * Tests {@link Java2PcmMethodBodyChangePreprocessor#doesHandleChange}, the predicate that
 * decides whether a change is even considered before any SEFF/SoMoX/correspondence-model
 * work happens. The method touches none of those - the correspondence model it's handed
 * is never read - so every case here is plain object construction, no fakes needed.
 */
class Java2PcmMethodBodyChangePreprocessorTest {

	/** The correspondenceModel parameter is never read by doesHandleChange; null stands in for "don't care". */
	private static final EditableCorrespondenceModelView<Correspondence> UNUSED_CORRESPONDENCE_MODEL = null;

	/** The method's name before the rename, used by the three rename-related test cases below. */
	private static final String OLD_NAME = "oldName";

	/** The method's new name in the one case where the rename is actually handled. */
	private static final String NEW_NAME = "newName";

	private final Java2PcmMethodBodyChangePreprocessor preprocessor = new Java2PcmMethodBodyChangePreprocessor(null);

	@Test
	void methodRenamedToNonEmptyName_isHandled() {
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		ReplaceSingleValuedEAttribute<ClassMethod, String> change = TypeInferringAtomicEChangeFactory.getInstance()
				.createReplaceSingleAttributeChange(method, CommonsPackage.Literals.NAMED_ELEMENT__NAME, OLD_NAME,
						NEW_NAME);

		assertTrue(preprocessor.doesHandleChange(change, UNUSED_CORRESPONDENCE_MODEL));
	}

	@Test
	void methodRenamedToEmptyName_isNotHandled() {
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		ReplaceSingleValuedEAttribute<ClassMethod, String> change = TypeInferringAtomicEChangeFactory.getInstance()
				.createReplaceSingleAttributeChange(method, CommonsPackage.Literals.NAMED_ELEMENT__NAME, OLD_NAME,
						"");

		assertFalse(preprocessor.doesHandleChange(change, UNUSED_CORRESPONDENCE_MODEL));
	}

	@Test
	void nonMethodElementRenamed_isNotHandled() {
		Field field = MembersFactory.eINSTANCE.createField();
		ReplaceSingleValuedEAttribute<Field, String> change = TypeInferringAtomicEChangeFactory.getInstance()
				.createReplaceSingleAttributeChange(field, CommonsPackage.Literals.NAMED_ELEMENT__NAME, OLD_NAME,
						NEW_NAME);

		assertFalse(preprocessor.doesHandleChange(change, UNUSED_CORRESPONDENCE_MODEL));
	}

	@Test
	void unrelatedAttributeOfMethodChanged_isNotHandled() {
		ClassMethod method = MembersFactory.eINSTANCE.createClassMethod();
		EAttribute someOtherAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		ReplaceSingleValuedEAttribute<ClassMethod, String> change = TypeInferringAtomicEChangeFactory.getInstance()
				.createReplaceSingleAttributeChange(method, someOtherAttribute, "oldValue", "newValue");

		assertFalse(preprocessor.doesHandleChange(change, UNUSED_CORRESPONDENCE_MODEL));
	}

	@Test
	void changeOfADifferentKind_isNotHandled() {
		EChange change = AttributeFactory.eINSTANCE.createInsertEAttributeValue();

		assertFalse(preprocessor.doesHandleChange(change, UNUSED_CORRESPONDENCE_MODEL));
	}
}
