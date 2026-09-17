package tools.cipm.seff;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;

import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

public final class CorrespondenceModelUtil {
	private CorrespondenceModelUtil() {}
	
	public static <TCorrespondingType> List<TCorrespondingType> getCorrespondingEObjects(EditableCorrespondenceModelView<Correspondence> correspondenceModel,
			EObject object, Class<TCorrespondingType> type) {
		return getCorrespondingEObjects(correspondenceModel, List.of(object), type);
	}

	public static <TCorrespondingType> List<TCorrespondingType> getCorrespondingEObjects(EditableCorrespondenceModelView<Correspondence> correspondenceModel,
			List<EObject> objects, Class<TCorrespondingType> type) {
		return correspondenceModel.getCorrespondingEObjects(objects).parallelStream().flatMap(list -> list.parallelStream())
			.filter(obj -> type.isInstance(obj)).map(obj -> type.cast(obj)).collect(Collectors.toList());
	}

	public static void removeCorrespondencesFor(EditableCorrespondenceModelView<Correspondence> correspondenceModel,
			EObject object) {
		removeCorrespondencesFor(correspondenceModel, object, null);
	}

	public static void removeCorrespondencesFor(EditableCorrespondenceModelView<Correspondence> correspondenceModel,
			EObject object, String tag) {
		removeCorrespondencesFor(correspondenceModel, List.of(object), tag);
	}

	public static void removeCorrespondencesFor(EditableCorrespondenceModelView<Correspondence> correspondenceModel,
			List<EObject> objects) {
		removeCorrespondencesFor(correspondenceModel, objects, null);
	}

	public static void removeCorrespondencesFor(EditableCorrespondenceModelView<Correspondence> correspondenceModel,
			List<EObject> objects, String tag) {
		var allCorrespondingElements = correspondenceModel.getCorrespondingEObjects(objects, tag);
		for (var corresponding : allCorrespondingElements) {
			correspondenceModel.removeCorrespondencesBetween(objects, corresponding, tag);
		}
	}
}
